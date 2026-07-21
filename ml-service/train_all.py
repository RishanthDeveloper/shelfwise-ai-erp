"""
ShelfWise ML Training Pipeline
Generates synthetic training data and trains:
  - DQN Agent (discount pricing)
  - XGBoost (demand forecasting)
  - Isolation Forest (anomaly/fraud detection)
  - NMF (B2B collaborative filtering — saves embeddings)

Outputs saved to ./models/ directory.
Run: python train_all.py
"""

import os, logging, warnings
from pathlib import Path
import numpy as np
import joblib

warnings.filterwarnings("ignore")
logging.basicConfig(level=logging.INFO, format="%(asctime)s  %(levelname)s  %(message)s")
log = logging.getLogger("shelfwise-train")

MODEL_DIR = Path("./models")
DATA_DIR = Path("./data")
MODEL_DIR.mkdir(exist_ok=True)
DATA_DIR.mkdir(exist_ok=True)

RNG = np.random.default_rng(42)
N_PRODUCTS = 1000
N_TRANSACTIONS = 5000

# ── 1. Generate Synthetic Data ─────────────────────────────────────────────────
log.info("Generating synthetic training data...")

CATEGORIES = ["Dairy", "Bakery", "Produce", "Protein", "Beverages", "Snacks"]

shelf_lives = RNG.integers(5, 365, N_PRODUCTS)
days_left   = RNG.integers(0, shelf_lives)
stocks      = RNG.integers(5, 500, N_PRODUCTS)
demand      = RNG.uniform(1, 20, N_PRODUCTS)
prices      = RNG.uniform(1.0, 15.0, N_PRODUCTS)
costs       = prices * RNG.uniform(0.4, 0.8, N_PRODUCTS)
lag1        = demand * RNG.uniform(0.8, 1.2, N_PRODUCTS)
lag2        = demand * RNG.uniform(0.8, 1.2, N_PRODUCTS)
pcc         = RNG.integers(0, 10, N_PRODUCTS)
urgency     = days_left / shelf_lives
stock_ratio = stocks / np.maximum(demand * np.maximum(days_left, 1), 1)

import numpy as np
features = np.column_stack([
    days_left / 30, stocks / 500, demand / 20, prices / 20, costs / 10,
    pcc / 10, lag1 / 20, lag2 / 20, urgency, stock_ratio
]).astype(np.float32)

# Target demand (slight uplift from discount)
discounts = RNG.uniform(0, 0.40, N_PRODUCTS)
target_demand = demand * (1 + discounts * 1.5) + RNG.normal(0, 0.5, N_PRODUCTS)
target_demand = np.clip(target_demand, 0, None)

np.save(DATA_DIR / "features.npy", features)
np.save(DATA_DIR / "target_demand.npy", target_demand)
log.info(f"Saved {N_PRODUCTS} product feature vectors.")

# Fraud / anomaly data
tx_amounts   = RNG.exponential(50, N_TRANSACTIONS)
tx_avg       = RNG.uniform(20, 100, N_TRANSACTIONS)
tx_freq      = RNG.uniform(0, 5, N_TRANSACTIONS)
# Inject 5% anomalous transactions
fraud_mask   = RNG.random(N_TRANSACTIONS) < 0.05
tx_amounts[fraud_mask] *= RNG.uniform(5, 15, fraud_mask.sum())

tx_features  = np.column_stack([tx_amounts, tx_avg, tx_freq]).astype(np.float32)
np.save(DATA_DIR / "tx_features.npy", tx_features)
log.info(f"Saved {N_TRANSACTIONS} transaction feature vectors ({fraud_mask.sum()} fraudulent).")

# ── 2. Train XGBoost Demand Forecaster ────────────────────────────────────────
log.info("Training XGBoost demand forecaster...")
try:
    import xgboost as xgb
    model_xgb = xgb.XGBRegressor(
        n_estimators=200, max_depth=6, learning_rate=0.05,
        subsample=0.8, colsample_bytree=0.8,
        objective="reg:squarederror", random_state=42, n_jobs=-1,
    )
    model_xgb.fit(features, target_demand)
    joblib.dump(model_xgb, MODEL_DIR / "xgb_demand.pkl")
    log.info("XGBoost saved → models/xgb_demand.pkl")
except ImportError:
    log.warning("xgboost not installed — skipping XGBoost training.")

# ── 3. Train Isolation Forest (Anomaly / Fraud) ───────────────────────────────
log.info("Training Isolation Forest...")
from sklearn.ensemble import IsolationForest
iso = IsolationForest(n_estimators=200, contamination=0.05, random_state=42, n_jobs=-1)
iso.fit(tx_features)
joblib.dump(iso, MODEL_DIR / "iso_forest.pkl")
log.info("Isolation Forest saved → models/iso_forest.pkl")

# ── 4. Train DQN Agent ────────────────────────────────────────────────────────
log.info("Training DQN agent...")
try:
    import torch
    from shelfwise_dqn_model import DQNAgent

    agent = DQNAgent(state_size=10, action_size=4, epsilon=1.0)
    EPISODES = 300
    BATCH_SIZE = 64
    TARGET_UPDATE = 20

    for ep in range(EPISODES):
        idx = RNG.integers(0, N_PRODUCTS)
        state = features[idx]
        total_reward = 0.0

        for _ in range(20):  # steps per episode
            action = agent.act(state)
            reward = DQNAgent.compute_reward(
                action, float(days_left[idx]), int(stocks[idx]),
                float(demand[idx]), float(prices[idx]), float(costs[idx])
            )
            next_idx = RNG.integers(0, N_PRODUCTS)
            next_state = features[next_idx]
            done = days_left[idx] <= 0
            agent.remember(state, action, reward, next_state, done)
            agent.replay(BATCH_SIZE)
            state = next_state
            idx = next_idx
            total_reward += reward

        if (ep + 1) % TARGET_UPDATE == 0:
            agent.update_target()

        if (ep + 1) % 50 == 0:
            log.info(f"  DQN Episode {ep+1}/{EPISODES} | ε={agent.epsilon:.3f} | reward={total_reward:.1f}")

    torch.save(agent.q_network.state_dict(), MODEL_DIR / "dqn_model.pth")
    log.info("DQN saved → models/dqn_model.pth")
except ImportError:
    log.warning("PyTorch not installed — skipping DQN training.")

# ── 5. NMF B2B Collaborative Filtering ───────────────────────────────────────
log.info("Training NMF for B2B recommendations...")
from sklearn.decomposition import NMF

N_RETAILERS = 50
interaction_matrix = RNG.integers(0, 100, (N_RETAILERS, N_PRODUCTS)).astype(float)
# Sparsify (80% missing)
mask = RNG.random((N_RETAILERS, N_PRODUCTS)) < 0.8
interaction_matrix[mask] = 0.0

nmf = NMF(n_components=20, random_state=42, max_iter=500)
W = nmf.fit_transform(interaction_matrix)  # retailer embeddings
H = nmf.components_                         # product embeddings
joblib.dump({"W": W, "H": H, "nmf": nmf}, MODEL_DIR / "nmf_b2b.pkl")
log.info("NMF saved → models/nmf_b2b.pkl")

log.info("=" * 60)
log.info("All models trained and saved to ./models/")
log.info("=" * 60)
