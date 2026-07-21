"""
ShelfWise ML Microservice — FastAPI inference server
Serves: DQN pricing, XGBoost demand forecast, Isolation Forest anomaly/fraud detection
Called by the Java Spring Boot backend via REST.
"""

from __future__ import annotations
import os, logging
from pathlib import Path
from typing import Optional

import numpy as np
import joblib
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field

# ── Logging ──────────────────────────────────────────────────────────────────
logging.basicConfig(level=logging.INFO, format="%(asctime)s  %(levelname)s  %(message)s")
log = logging.getLogger("shelfwise-ml")

# ── App ───────────────────────────────────────────────────────────────────────
app = FastAPI(
    title="ShelfWise ML Microservice",
    description="DQN + XGBoost + Isolation Forest inference endpoints",
    version="1.0.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

# ── Model paths ───────────────────────────────────────────────────────────────
MODEL_DIR = Path(os.getenv("MODEL_DIR", "./models"))

# Lazy-loaded model singletons
_dqn = None
_xgb = None
_iso = None


def load_models():
    global _dqn, _xgb, _iso
    try:
        from shelfwise_dqn_model import DQNAgent
        dqn_path = MODEL_DIR / "dqn_model.pth"
        if dqn_path.exists():
            import torch
            agent = DQNAgent(state_size=10, action_size=4)
            agent.q_network.load_state_dict(torch.load(str(dqn_path), map_location="cpu"))
            agent.q_network.eval()
            _dqn = agent
            log.info("DQN model loaded.")
        else:
            log.warning("DQN model file not found — will use heuristic fallback.")
    except Exception as e:
        log.warning(f"DQN load failed: {e}")

    try:
        xgb_path = MODEL_DIR / "xgb_demand.pkl"
        if xgb_path.exists():
            _xgb = joblib.load(xgb_path)
            log.info("XGBoost model loaded.")
    except Exception as e:
        log.warning(f"XGBoost load failed: {e}")

    try:
        iso_path = MODEL_DIR / "iso_forest.pkl"
        if iso_path.exists():
            _iso = joblib.load(iso_path)
            log.info("Isolation Forest model loaded.")
    except Exception as e:
        log.warning(f"Isolation Forest load failed: {e}")


@app.on_event("startup")
def startup():
    load_models()


# ── Request / Response schemas ────────────────────────────────────────────────
class RecommendRequest(BaseModel):
    daysLeft: float = Field(..., ge=0, description="Days until expiry")
    stock: int = Field(..., ge=0)
    demandRate: float = Field(..., ge=0)
    price: float = Field(..., gt=0)
    cost: float = Field(..., gt=0)
    priceChangeCount: int = Field(0, ge=0)
    lag1: Optional[float] = 0.0
    lag2: Optional[float] = 0.0
    category: Optional[str] = "General"
    shelfLife: int = Field(..., ge=1)


class RecommendResponse(BaseModel):
    discountFactor: float
    action: str
    predictedDemand: float
    anomalyScore: float
    isAnomaly: bool
    qValues: list[float]
    rationale: str
    modelSource: str


class FraudRequest(BaseModel):
    customerId: Optional[str] = None
    amount: float = Field(..., gt=0)
    avgPurchaseValue: float = Field(..., gt=0)
    purchaseFrequency: float = Field(..., ge=0)
    hourOfDay: int = Field(..., ge=0, le=23)
    isWeekend: bool
    itemsCount: int = Field(..., ge=1)


class FraudResponse(BaseModel):
    isFraud: bool
    fraudProbability: float
    anomalyScore: float
    flaggedReason: str
    riskLevel: str
    modelSource: str


# ── Helpers ───────────────────────────────────────────────────────────────────
ACTIONS = ["HOLD_PRICE", "MILD_DISCOUNT", "DISCOUNT", "DEEP_DISCOUNT"]
DISCOUNT_MAP = {"HOLD_PRICE": 0.0, "MILD_DISCOUNT": 0.10, "DISCOUNT": 0.25, "DEEP_DISCOUNT": 0.40}


def _build_state(req: RecommendRequest) -> np.ndarray:
    urgency = req.daysLeft / max(req.shelfLife, 1)
    stock_ratio = req.stock / max(req.demandRate * req.daysLeft, 1)
    margin = (req.price - req.cost) / max(req.cost, 1)
    return np.array([
        req.daysLeft / 30,
        req.stock / 500,
        req.demandRate / 20,
        req.price / 20,
        req.cost / 10,
        req.priceChangeCount / 10,
        (req.lag1 or 0) / 20,
        (req.lag2 or 0) / 20,
        urgency,
        stock_ratio,
    ], dtype=np.float32)


def _heuristic_recommend(req: RecommendRequest) -> RecommendResponse:
    urgency = req.daysLeft / max(req.shelfLife, 1)
    stock_ratio = req.stock / max(req.demandRate * req.daysLeft, 1)

    if urgency < 0.15 or stock_ratio > 2.5:
        action = "DEEP_DISCOUNT"
    elif urgency < 0.35:
        action = "DISCOUNT"
    elif urgency > 0.7 and stock_ratio < 0.5:
        action = "HOLD_PRICE"
    else:
        action = "MILD_DISCOUNT"

    discount = DISCOUNT_MAP[action]
    rng = np.random.default_rng(int(req.daysLeft * 17))
    q_vals = [round(float(rng.uniform(0.2, 0.9)), 3) for _ in range(4)]
    q_vals[ACTIONS.index(action)] = max(q_vals) + 0.1

    return RecommendResponse(
        discountFactor=discount,
        action=action,
        predictedDemand=round(req.demandRate * (1 + discount), 2),
        anomalyScore=round(abs(req.price - req.cost) / max(req.cost, 1), 3),
        isAnomaly=False,
        qValues=[round(v, 3) for v in q_vals],
        rationale=f"Urgency={urgency:.2f}, StockRatio={stock_ratio:.2f} → {action}",
        modelSource="fallback",
    )


# ── Endpoints ─────────────────────────────────────────────────────────────────
@app.get("/health")
def health():
    return {
        "status": "ok",
        "models": {
            "dqn": _dqn is not None,
            "xgboost": _xgb is not None,
            "isolation_forest": _iso is not None,
        }
    }


@app.post("/recommend", response_model=RecommendResponse)
def recommend(req: RecommendRequest):
    try:
        state = _build_state(req)

        # --- DQN inference ---
        if _dqn is not None:
            import torch
            with torch.no_grad():
                q_tensor = _dqn.q_network(torch.FloatTensor(state).unsqueeze(0))
                q_vals = q_tensor.squeeze().tolist()
            action_idx = int(np.argmax(q_vals))
            action = ACTIONS[action_idx]
            discount = DISCOUNT_MAP[action]
            model_src = "dqn"
        else:
            return _heuristic_recommend(req)

        # --- XGBoost demand forecast ---
        if _xgb is not None:
            pred_demand = float(_xgb.predict(state.reshape(1, -1))[0])
        else:
            pred_demand = round(req.demandRate * (1 + discount), 2)

        # --- Isolation Forest anomaly ---
        price_feat = np.array([[req.price, req.cost, req.priceChangeCount]], dtype=np.float32)
        if _iso is not None:
            score = float(-_iso.score_samples(price_feat)[0])
            is_anom = bool(_iso.predict(price_feat)[0] == -1)
        else:
            score = round(abs(req.price - req.cost) / max(req.cost, 1), 3)
            is_anom = score > 2.0

        urgency = req.daysLeft / max(req.shelfLife, 1)
        stock_ratio = req.stock / max(req.demandRate * req.daysLeft, 1)
        rationale = (
            f"DQN Q-max={max(q_vals):.3f} → {action} | "
            f"XGB demand={pred_demand:.1f} | "
            f"Urgency={urgency:.2f}, StockRatio={stock_ratio:.2f}"
        )

        return RecommendResponse(
            discountFactor=discount,
            action=action,
            predictedDemand=round(pred_demand, 2),
            anomalyScore=round(score, 3),
            isAnomaly=is_anom,
            qValues=[round(v, 3) for v in q_vals],
            rationale=rationale,
            modelSource=model_src,
        )
    except Exception as e:
        log.error(f"Recommend error: {e}", exc_info=True)
        return _heuristic_recommend(req)


@app.post("/fraud/detect", response_model=FraudResponse)
def detect_fraud(req: FraudRequest):
    try:
        ratio = req.amount / max(req.avgPurchaseValue, 1.0)
        odd_hour = req.hourOfDay < 6 or req.hourOfDay > 22
        high_amount = ratio > 3.0

        feat = np.array([[req.amount, req.avgPurchaseValue, req.purchaseFrequency,
                          req.hourOfDay, int(req.isWeekend), req.itemsCount]], dtype=np.float32)

        if _iso is not None:
            score = float(-_iso.score_samples(feat[:, :3])[0])
            is_fraud = bool(_iso.predict(feat[:, :3])[0] == -1) or (high_amount and odd_hour)
        else:
            score = min(ratio / 3.0, 1.0)
            is_fraud = (high_amount and odd_hour) or score > 0.75

        fraud_prob = min(score * 0.8 + (0.2 if odd_hour else 0.0), 1.0)
        risk = ("CRITICAL" if fraud_prob > 0.8 else
                "HIGH" if fraud_prob > 0.6 else
                "MEDIUM" if fraud_prob > 0.35 else "LOW")
        reason = (("Amount 3x above average" if high_amount else "") +
                  (" + Off-hours" if odd_hour else "") or "Within normal parameters")

        return FraudResponse(
            isFraud=is_fraud,
            fraudProbability=round(fraud_prob, 3),
            anomalyScore=round(score, 3),
            flaggedReason=reason,
            riskLevel=risk,
            modelSource="isolation_forest" if _iso else "fallback",
        )
    except Exception as e:
        log.error(f"Fraud detect error: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=False)
