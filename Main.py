"""
ShelfWise ERP — ML Microservice
================================
Pure model-inference microservice. Owns NO business data — the Java backend
(shelfwise-erp-backend) owns products, batches, customers, transactions, and
all business math. This service only ever receives feature vectors and
returns model predictions.

Run:  uvicorn main:app --reload --port 8000
Docs: http://localhost:8000/docs
"""

from fastapi import FastAPI
from pydantic import BaseModel, Field
import numpy as np
import joblib
import torch
import os

from dqn_model import DQNAgent

app = FastAPI(
    title="ShelfWise ML Service",
    description="Model inference for expiry-risk, demand forecasting, discount policy (DQN), fraud detection, and B2B recommendations.",
    version="1.0.0",
)

MODELS: dict = {}
CATEGORIES = ["Dairy", "Bakery", "Fresh Produce", "Beverages",
              "Canned Goods", "Snacks", "Frozen"]


def _load(path: str):
    if os.path.exists(path):
        try:
            return joblib.load(path)
        except Exception as e:
            print(f"[WARN] Could not load {path}: {e}")
    return None


@app.on_event("startup")
async def load_models():
    MODELS["expiry_risk"] = _load("expiry_risk_model.pkl")
    MODELS["demand"] = _load("demand_model.pkl")
    MODELS["b2b"] = _load("models/b2b_recommender.pkl")
    MODELS["fraud"] = _load("models/fraud_detector.pkl")

    if os.path.exists("dqn_model.pth"):
        agent = DQNAgent(state_size=5, action_size=6)
        agent.policy_net.load_state_dict(torch.load("dqn_model.pth", map_location="cpu"))
        agent.epsilon = 0.0
        MODELS["dqn"] = agent

    print("[INFO] Models loaded:", [k for k, v in MODELS.items() if v is not None])


# ──────────────────────────────────────────────
#  Schemas (feature vectors only — no business fields)
# ──────────────────────────────────────────────
class ExpiryRiskRequest(BaseModel):
    days_left: int
    stock: int
    demand_rate: float
    price: float
    cost: float


class DemandRequest(BaseModel):
    lag1: float
    lag2: float
    days_left: int
    price: float


class DiscountRequest(BaseModel):
    days_left: int
    stock: int
    demand_rate: float
    price_change_count: int = Field(0, ge=0, le=20)
    category_id: int = Field(0, ge=0, le=6)
    shelf_life: int = Field(14, ge=1)


class FraudRequest(BaseModel):
    quantity: int
    price: float
    hour: int = Field(12, ge=0, le=23)
    txn_count_7d: int = 5
    payment_mode: str = "cash"
    customer_freq: int = 10


class B2BRecommendRequest(BaseModel):
    customer_id: int
    top_n: int = Field(5, ge=1, le=20)


# ──────────────────────────────────────────────
#  Rule-based fallbacks (used only if a model failed to load / wasn't trained yet)
# ──────────────────────────────────────────────
def _rule_based_risk(days_left: int, stock: int) -> float:
    score = max(0.01, min(0.99, (10 - days_left) / 10))
    if stock > 80:
        score = min(0.99, score + 0.15)
    return round(score, 3)


def _rule_based_discount(days_left: int) -> int:
    if days_left <= 1: return 50
    if days_left <= 2: return 40
    if days_left <= 4: return 30
    if days_left <= 7: return 15
    if days_left <= 10: return 5
    return 0


# ──────────────────────────────────────────────
#  Endpoints
# ──────────────────────────────────────────────
@app.get("/health")
async def health():
    return {"status": "ok", "models_loaded": [k for k, v in MODELS.items() if v is not None]}


@app.post("/predict/expiry-risk")
async def predict_expiry_risk(req: ExpiryRiskRequest):
    features = np.array([[req.days_left, req.stock, req.demand_rate, req.price, req.cost]])
    if MODELS.get("expiry_risk"):
        risk = float(MODELS["expiry_risk"].predict_proba(features)[0][1])
        model = "XGBoost"
    else:
        risk = _rule_based_risk(req.days_left, req.stock)
        model = "rule-based"
    return {"risk": round(risk, 3), "model": model}


@app.post("/predict/demand")
async def predict_demand(req: DemandRequest):
    features = np.array([[req.lag1, req.lag2, req.days_left, req.price]])
    if MODELS.get("demand"):
        forecast = max(0, int(round(float(MODELS["demand"].predict(features)[0]))))
        model = "Ridge Regression"
    else:
        forecast = max(1, int(round((req.lag1 + req.lag2) / 2)))
        model = "rule-based"
    return {"forecast": forecast, "model": model}


@app.post("/predict/discount")
async def predict_discount(req: DiscountRequest):
    if MODELS.get("dqn"):
        state = [
            req.days_left / max(req.shelf_life, 1),
            req.stock / 200.0,
            req.demand_rate / 50.0,
            min(10, req.price_change_count) / 10.0,
            req.category_id / 7.0,
        ]
        action = MODELS["dqn"].act(state, eval_mode=True)
        discount = action * 10
        model = "Deep Q-Network"
    else:
        discount = _rule_based_discount(req.days_left)
        model = "rule-based"
    return {"discount": discount, "model": model}


@app.post("/predict/fraud")
async def predict_fraud(req: FraudRequest):
    payment_map = {"cash": 0, "credit": 1, "upi": 2, "cheque": 3}
    payment_code = payment_map.get(req.payment_mode.lower(), 0)
    features = np.array([[req.quantity, req.price, req.hour, req.txn_count_7d, payment_code, req.customer_freq]])

    if MODELS.get("fraud"):
        score = float(MODELS["fraud"].decision_function(features)[0])
        anomaly_prob = round(1 / (1 + np.exp(score * 3)), 3)
        model = "Isolation Forest"
    else:
        anomaly_prob = 0.9 if (req.hour < 5 and req.quantity > 300) else 0.1
        model = "rule-based"
    return {"anomaly_score": anomaly_prob, "model": model}


@app.post("/predict/b2b-recommend")
async def predict_b2b_recommend(req: B2BRecommendRequest):
    b2b_data = MODELS.get("b2b")
    if b2b_data:
        customer_ids = b2b_data["customer_ids"]
        product_ids = b2b_data["product_ids"]
        user_factors = b2b_data["user_factors"]
        item_factors = b2b_data["item_factors"]

        if req.customer_id in customer_ids:
            idx = customer_ids.index(req.customer_id)
            scores = user_factors[idx] @ item_factors.T
            top_indices = np.argsort(scores)[::-1][:req.top_n]
            recommended = [int(product_ids[i]) for i in top_indices]
            model = "NMF"
        else:
            recommended = list(range(1, req.top_n + 1))
            model = "fallback"
    else:
        recommended = list(np.random.choice(range(1, 36), req.top_n, replace=False).tolist())
        model = "fallback"

    return {"customer_id": req.customer_id, "recommended_ids": recommended, "model": model}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
