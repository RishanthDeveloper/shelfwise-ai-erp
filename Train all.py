"""
ShelfWise ERP — Complete Model Training Pipeline
=================================================
Run:  python train_all.py
This script generates a synthetic retail dataset and trains all 5 AI models,
saving the artifacts (*.pkl, dqn_model.pth) into this folder so main.py can
load them on startup. This dataset is independent of the Java backend's own
demo Postgres data — it exists purely to train the models offline.
"""

import os, sys, warnings
warnings.filterwarnings("ignore")
os.makedirs("data", exist_ok=True)
os.makedirs("models", exist_ok=True)

import numpy as np
import pandas as pd
from datetime import datetime, timedelta


# ══════════════════════════════════════════════════════════════
#  STEP 1 — Generate Synthetic Retail Data
# ══════════════════════════════════════════════════════════════
def generate_data():
    print("\n[1/6] Generating synthetic retail data…")
    np.random.seed(42)

    CATEGORIES = {
        "Dairy":       (7,   14),
        "Bakery":      (3,    7),
        "Fresh Produce": (5, 10),
        "Beverages":   (180, 365),
        "Canned Goods": (365, 730),
        "Snacks":      (90,  180),
        "Frozen":      (90,  365),
    }

    # Products
    rows = []
    for pid in range(1, 201):
        cat  = np.random.choice(list(CATEGORIES.keys()))
        lo, hi = CATEGORIES[cat]
        shelf = np.random.randint(lo, hi + 1)
        price = round(np.random.uniform(20, 500), 2)
        cost  = round(price * np.random.uniform(0.4, 0.7), 2)
        uom   = np.random.choice(["pc", "kg", "ltr", "pack"])
        rows.append([pid, f"Product_{pid}", cat, price, cost, shelf, uom, f"890{pid:012d}"])
    products_df = pd.DataFrame(rows, columns=["id","name","category","base_price","cost","shelf_life_days","uom","barcode"])
    products_df.to_csv("data/products.csv", index=False)

    # Batches
    batches, bid = [], 1
    for _, p in products_df.iterrows():
        for _ in range(np.random.randint(1, 5)):
            entry  = datetime(2025, 1, 1) + timedelta(days=np.random.randint(0, 60))
            expiry = entry + timedelta(days=int(p["shelf_life_days"]))
            qty    = np.random.randint(20, 200)
            batches.append([bid, p["id"], f"BATCH_{bid}", entry.date(), expiry.date(),
                            qty, qty, round(p["cost"] * np.random.uniform(0.9, 1.1), 2)])
            bid += 1
    batches_df = pd.DataFrame(batches, columns=["id","product_id","batch_code","entry_date","expiry_date","initial_qty","current_qty","cost_price"])
    batches_df.to_csv("data/batches.csv", index=False)

    # Daily sales
    records = []
    for _, batch in batches_df.iterrows():
        entry  = pd.to_datetime(batch["entry_date"])
        expiry = pd.to_datetime(batch["expiry_date"])
        stock  = int(batch["initial_qty"])
        price  = float(products_df.loc[products_df["id"] == batch["product_id"], "base_price"].values[0])
        cost   = float(batch["cost_price"])
        shelf  = (expiry - entry).days

        current = entry
        while current <= expiry and stock > 0:
            days_left   = (expiry - current).days
            demand_rate = max(1, int(10 * (days_left / max(shelf, 1))))
            demand      = min(stock, np.random.poisson(lam=demand_rate))
            stock      -= demand
            records.append({"batch_id": batch["id"], "product_id": batch["product_id"],
                            "date": current.strftime("%Y-%m-%d"), "days_left": days_left,
                            "stock_before": stock + demand, "sold": demand, "stock_after": stock,
                            "price": price, "cost": cost, "shelf_life": shelf})
            current += timedelta(days=1)
        if stock > 0:
            records.append({"batch_id": batch["id"], "product_id": batch["product_id"],
                            "date": expiry.strftime("%Y-%m-%d"), "days_left": 0,
                            "stock_before": stock, "sold": 0, "stock_after": 0,
                            "price": price, "cost": cost, "shelf_life": shelf, "wasted": stock})

    pd.DataFrame(records).to_csv("data/daily_sales.csv", index=False)

    # B2B customers & transactions
    b2b = [[i, f"B2B_{i}", np.random.choice(["gold","silver","bronze"], p=[0.2,0.3,0.5])]
           for i in range(1, 51)]
    pd.DataFrame(b2b, columns=["id","name","tier"]).to_csv("data/b2b_customers.csv", index=False)

    txns = []
    for _ in range(2000):
        cid  = np.random.randint(1, 51)
        pid  = np.random.randint(1, 201)
        qty  = np.random.randint(1, 100)
        px   = float(products_df.loc[products_df["id"] == pid, "base_price"].values[0])
        mode = np.random.choice(["cash","upi","credit","cheque"], p=[0.4,0.35,0.15,0.1])
        date = datetime(2025, 1, 1) + timedelta(days=np.random.randint(0, 90))
        txns.append([cid, pid, qty, round(px*qty, 2), mode, date.strftime("%Y-%m-%d")])
    pd.DataFrame(txns, columns=["customer_id","product_id","quantity","price","payment_mode","date"]).to_csv("data/b2b_transactions.csv", index=False)

    # Sync anomaly logs
    logs = []
    for _ in range(1000):
        ss = np.random.randint(0, 200)
        ps = ss + np.random.choice([-20,-10,0,10,20], p=[0.05,0.1,0.7,0.1,0.05])
        logs.append([ss, ps, np.random.uniform(1,50), np.random.randint(1,72)])
    pd.DataFrame(logs, columns=["sys_stock","phys_stock","sales_rate","hours_since"]).to_csv("data/sync_logs.csv", index=False)

    print("   ✓ Products, batches, sales, B2B, sync logs saved to data/")


# ══════════════════════════════════════════════════════════════
#  STEP 2 — Train Expiry Risk (XGBoost)
# ══════════════════════════════════════════════════════════════
def train_expiry_risk():
    print("\n[2/6] Training expiry risk model (XGBoost)…")
    import xgboost as xgb
    import joblib
    from sklearn.model_selection import train_test_split
    from sklearn.metrics import accuracy_score, roc_auc_score

    df = pd.read_csv("data/daily_sales.csv")
    if "wasted" in df.columns:
        wb = df.groupby("batch_id")["wasted"].sum().reset_index().rename(columns={"wasted":"wasted_total"})
        df = df.merge(wb, on="batch_id")
        df["risky"] = (df["wasted_total"] > 0).astype(int)
    else:
        df["risky"] = ((df["days_left"] < 3) & (df["stock_before"] > 10)).astype(int)

    feats = ["days_left","stock_before","sold","price","cost"]
    df = df.dropna(subset=feats + ["risky"])
    X, y = df[feats], df["risky"]
    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

    model = xgb.XGBClassifier(n_estimators=100, max_depth=4, learning_rate=0.05,
                               eval_metric="logloss", random_state=42, use_label_encoder=False)
    model.fit(X_train, y_train, eval_set=[(X_test, y_test)], verbose=False)

    y_pred  = model.predict(X_test)
    y_proba = model.predict_proba(X_test)[:,1]
    print(f"   Accuracy: {accuracy_score(y_test, y_pred):.3f} | AUC: {roc_auc_score(y_test, y_proba):.3f}")
    joblib.dump(model, "expiry_risk_model.pkl")
    print("   ✓ expiry_risk_model.pkl saved")


# ══════════════════════════════════════════════════════════════
#  STEP 3 — Train Demand Forecast (Linear Regression)
# ══════════════════════════════════════════════════════════════
def train_demand_model():
    print("\n[3/6] Training demand forecast model (Linear Regression)…")
    import joblib
    from sklearn.linear_model import Ridge
    from sklearn.metrics import mean_absolute_error

    df = pd.read_csv("data/daily_sales.csv").sort_values(["batch_id","date"])
    df["lag1"] = df.groupby("batch_id")["sold"].shift(1)
    df["lag2"] = df.groupby("batch_id")["sold"].shift(2)
    df = df.dropna()

    X = df[["lag1","lag2","days_left","price"]]
    y = df["sold"]
    model = Ridge(alpha=1.0)
    model.fit(X, y)
    mae = mean_absolute_error(y, model.predict(X))
    print(f"   MAE: {mae:.2f} units/day")
    joblib.dump(model, "demand_model.pkl")
    print("   ✓ demand_model.pkl saved")


# ══════════════════════════════════════════════════════════════
#  STEP 4 — Train Discount Policy (DQN)
# ══════════════════════════════════════════════════════════════
def train_dqn():
    print("\n[4/6] Training discount policy (Deep Q-Network)…")
    import torch
    from dqn_model import DQNAgent, RetailEnv

    sales_df    = pd.read_csv("data/daily_sales.csv")
    products_df = pd.read_csv("data/products.csv")
    merged      = sales_df.merge(products_df[["id","category"]], left_on="product_id", right_on="id")
    CATS        = ["Dairy","Bakery","Fresh Produce","Beverages","Canned Goods","Snacks","Frozen"]

    agent   = DQNAgent(state_size=5, action_size=6)
    episodes = 200

    for ep in range(episodes):
        sample  = merged.sample(1).iloc[0]
        cat_idx = CATS.index(sample["category"]) if sample["category"] in CATS else 0
        batch   = {
            "days_left": max(1, int(sample["days_left"])),
            "stock":     max(1, int(sample["stock_before"])),
            "demand_rate": max(1, float(sample["sold"])),
            "price_change_count": 0,
            "category_id": cat_idx,
            "price": float(sample["price"]),
            "cost":  float(sample["cost"]),
            "shelf_life": float(sample["shelf_life"]),
        }
        env          = RetailEnv(batch)
        state        = env.state
        total_reward = 0.0
        done, steps  = False, 0

        while not done and steps < 20:
            action             = agent.act(state)
            next_state, reward, done = env.step(action)
            agent.remember(state, action, reward, next_state, done)
            agent.replay()
            state        = next_state
            total_reward += reward
            steps        += 1

        agent.update_target()
        if ep % 50 == 0:
            print(f"   Ep {ep:>3}  reward={total_reward:>8.1f}  ε={agent.epsilon:.3f}")

    torch.save(agent.policy_net.state_dict(), "dqn_model.pth")
    print("   ✓ dqn_model.pth saved")


# ══════════════════════════════════════════════════════════════
#  STEP 5 — Train B2B Recommender (NMF)
# ══════════════════════════════════════════════════════════════
def train_b2b():
    print("\n[5/6] Training B2B recommender (NMF)…")
    import joblib
    from sklearn.decomposition import NMF

    df     = pd.read_csv("data/b2b_transactions.csv")
    matrix = df.pivot_table(index="customer_id", columns="product_id",
                            values="quantity", aggfunc="sum", fill_value=0)
    n_comp = min(20, matrix.shape[1] - 1, matrix.shape[0] - 1)
    model  = NMF(n_components=n_comp, init="random", random_state=42, max_iter=300)

    user_factors = model.fit_transform(matrix.values)
    item_factors = model.components_.T

    joblib.dump({
        "model":        model,
        "user_factors": user_factors,
        "item_factors": item_factors,
        "product_ids":  matrix.columns.tolist(),
        "customer_ids": matrix.index.tolist(),
    }, "models/b2b_recommender.pkl")
    print(f"   Components: {n_comp}  ✓ models/b2b_recommender.pkl saved")


# ══════════════════════════════════════════════════════════════
#  STEP 6 — Train Fraud Detector (Isolation Forest)
# ══════════════════════════════════════════════════════════════
def train_fraud():
    print("\n[6/6] Training fraud detector (Isolation Forest)…")
    import joblib
    from sklearn.ensemble import IsolationForest

    df = pd.read_csv("data/b2b_transactions.csv")
    df["hour"]          = np.random.randint(0, 24, len(df))
    df["txn_count_7d"]  = np.random.poisson(5, len(df))
    df["payment_enc"]   = df["payment_mode"].astype("category").cat.codes
    df["customer_freq"] = df["customer_id"].map(df["customer_id"].value_counts())

    X     = df[["quantity","price","hour","txn_count_7d","payment_enc","customer_freq"]]
    model = IsolationForest(contamination=0.05, n_estimators=100, random_state=42)
    model.fit(X)
    joblib.dump(model, "models/fraud_detector.pkl")
    print("   ✓ models/fraud_detector.pkl saved")


# ══════════════════════════════════════════════════════════════
#  MAIN
# ══════════════════════════════════════════════════════════════
if __name__ == "__main__":
    generate_data()
    train_expiry_risk()
    train_demand_model()
    train_dqn()
    train_b2b()
    train_fraud()
    print("\n✅  All models trained and saved. Run:  uvicorn main:app --reload")
