# ShelfWise AI Retail ERP

**AI-powered retail ERP for expiry management, demand forecasting, dynamic discounting, fraud detection, and B2B recommendations.**

A polyglot, microservice-style system: a **Java Spring Boot** backend owns all business logic and data, and delegates model inference to a **Python (FastAPI + PyTorch/scikit-learn)** ML microservice. A single-page dashboard visualizes everything live.

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-brightgreen)
![Python](https://img.shields.io/badge/Python-3.11-blue)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED)
![License](https://img.shields.io/badge/license-MIT-lightgrey)

**🔗 Live demo (frontend only):** [rishanthdeveloper.github.io/shelfwise-ai-retail-erp](https://rishanthdeveloper.github.io/shelfwise-ai-retail-erp/)

> GitHub Pages only serves static files, so it can't run the Java backend or Python ML service — the hosted demo above shows the full UI/UX with demo data (or its own in-browser fallback simulation of the models), not a live database or real model inference. Run `docker compose up --build` locally (see [Quick start](#quick-start)) to get the real Spring Boot API, Postgres data, and actual DQN/XGBoost/Ridge/Isolation-Forest/NMF predictions behind every screen.

---

## Why this architecture

Retail AI models (a discount-policy DQN, an XGBoost expiry-risk classifier, an Isolation Forest fraud detector, an NMF collaborative-filter recommender) are all built and iterated on fastest in Python's ML ecosystem. Core ERP concerns — transactional data, business rules, validation, orchestration, uptime — are a better fit for a statically-typed, JVM-based service.

So the system is split by responsibility, not by convenience:

- **`backend-java/`** — the system of record. Owns Postgres, all CRUD, KPI/analytics computation, business math (suggested price, expected revenue/profit), and resilience (falls back to rule-based logic if the ML service is down).
- **`ml-service/`** — a stateless model-serving layer. Receives feature vectors, returns predictions. No business data lives here.
- **`frontend/`** — a single-file dashboard that talks to the Java API over REST, with graceful fallback to demo data if the backend isn't running.

This mirrors how AI features get shipped inside real engineering orgs: a thin, swappable model-serving layer behind a stable business API.

```
┌────────────────────┐        REST/JSON        ┌──────────────────────┐        REST/JSON        ┌───────────────────────┐
│   frontend/         │  ───────────────────▶   │  backend-java/        │  ───────────────────▶   │   ml-service/          │
│   index.html        │  ◀───────────────────   │  Spring Boot API      │  ◀───────────────────   │   FastAPI + PyTorch    │
│   (dashboard UI)     │                         │  Postgres (JPA)       │                          │   scikit-learn, XGBoost│
└────────────────────┘                          └──────────┬────────────┘                          └───────────────────────┘
                                                             │
                                                        PostgreSQL
                                                     (products, batches,
                                                    b2b customers/txns)
```

---

## Features

| Feature | Model | Owner |
|---|---|---|
| Expiry-risk scoring | XGBoost classifier | `ml-service`, orchestrated by `backend-java` |
| Next-day demand forecast | Ridge regression | `ml-service`, orchestrated by `backend-java` |
| Dynamic discount policy | Deep Q-Network (custom retail RL environment) | `ml-service`, orchestrated by `backend-java` |
| B2B product recommendations | NMF collaborative filtering | `ml-service`, enriched with product data by `backend-java` |
| Transaction fraud detection | Isolation Forest | `ml-service`, business rules layered in `backend-java` |
| Dashboard KPIs, expiry alerts, 80/20 pareto analysis, waste-by-category | — | Computed live in `backend-java` from Postgres |

Every ML-backed endpoint in `backend-java` degrades to a simple rule-based fallback if the ML microservice is unreachable — the ERP never goes down just because the model server does.

---

## Project structure

```
shelfwise-erp/
├── backend-java/                  # Spring Boot — core business API
│   ├── src/main/java/com/shelfwise/
│   │   ├── ShelfwiseApplication.java
│   │   ├── config/                # CORS, WebClient, ML service properties
│   │   ├── entity/                # Product, Batch, B2BCustomer, B2BTransaction
│   │   ├── repository/            # Spring Data JPA repositories
│   │   ├── dto/                   # Request/response DTOs per feature
│   │   ├── service/                # Business logic + ML orchestration
│   │   ├── controller/            # REST endpoints
│   │   └── exception/             # Centralized error handling
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── data.sql               # Seed data (35 products, batches, B2B customers/txns)
│   ├── pom.xml
│   └── Dockerfile
│
├── ml-service/                    # Python — pure model inference microservice
│   ├── main.py                    # FastAPI app: /predict/* endpoints
│   ├── dqn_model.py                # DQN network, replay buffer, agent, RetailEnv
│   ├── train_all.py               # Generates synthetic data + trains all 5 models
│   ├── requirements.txt
│   └── Dockerfile
│
├── frontend/
│   └── index.html                 # Dashboard UI (vanilla JS + Chart.js), calls backend-java
│
├── docker-compose.yml             # Postgres + backend-java + ml-service
├── .gitignore
└── README.md
```

---

## Quick start

### Option A — Docker Compose (recommended)

```bash
git clone <your-repo-url> shelfwise-erp
cd shelfwise-erp
docker compose up --build
```

This starts:
- **Postgres** on `localhost:5432` (seeded automatically on first boot)
- **ml-service** (FastAPI) on `localhost:8000` — models are trained at image build time
- **backend-java** (Spring Boot) on `localhost:8080`

Then open `frontend/index.html` directly in a browser (or serve it: `cd frontend && npx serve .`).

### Option B — Run each service manually

**1. Python ML microservice**
```bash
cd ml-service
pip install -r requirements.txt
python train_all.py              # trains all 5 models (~2-3 min)
uvicorn main:app --reload --port 8000
```

**2. Postgres**
```bash
docker run -d --name shelfwise-postgres \
  -e POSTGRES_DB=shelfwise -e POSTGRES_USER=shelfwise -e POSTGRES_PASSWORD=shelfwise \
  -p 5432:5432 postgres:16-alpine
```

**3. Java backend**
```bash
cd backend-java
mvn spring-boot:run
```
API docs (via Actuator health): `http://localhost:8080/api/health`

**4. Frontend**
Just open `frontend/index.html` in a browser. It talks to `http://localhost:8080` by default — override with:
```html
<script>window.SHELFWISE_API_BASE = 'http://localhost:8080';</script>
```

---

## API reference (`backend-java`)

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/health` | Backend + ML service reachability |
| GET | `/api/products` | List products |
| POST | `/api/products` | Create product |
| PUT | `/api/products/{id}` | Update product |
| DELETE | `/api/products/{id}` | Delete product |
| GET | `/api/dashboard/kpis` | Store-level KPIs (computed live from Postgres) |
| GET | `/api/alerts/expiry` | Near-expiry batches with AI-recommended discounts |
| POST | `/api/ai/recommend` | Full pipeline: expiry-risk → demand forecast → DQN discount |
| POST | `/api/fraud/detect` | Isolation-Forest anomaly check on a transaction |
| POST | `/api/b2b/recommend` | NMF-based product recommendations, enriched with product data |
| GET | `/api/analytics/pareto` | 80/20 sales concentration analysis |
| GET | `/api/analytics/waste-by-category` | Wasted units grouped by category |

### `ml-service` internal API (called by `backend-java`, not the frontend)

| Method | Endpoint | Description |
|---|---|---|
| GET | `/health` | Model load status |
| POST | `/predict/expiry-risk` | XGBoost waste-risk probability |
| POST | `/predict/demand` | Ridge regression next-day demand |
| POST | `/predict/discount` | DQN discount action (0–50%, steps of 10%) |
| POST | `/predict/fraud` | Isolation Forest anomaly score |
| POST | `/predict/b2b-recommend` | NMF top-N product ids for a customer |

---

## Configuration

`backend-java` reads these via environment variables (see `application.yml`):

| Variable | Default | Purpose |
|---|---|---|
| `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USER` / `DB_PASSWORD` | `localhost` / `5432` / `shelfwise` / `shelfwise` / `shelfwise` | Postgres connection |
| `ML_SERVICE_URL` | `http://localhost:8000` | Base URL of the Python ML microservice |

---

## Deploying the backend + ML service (to make the live demo show real data)

The GitHub Pages link only hosts `frontend/index.html`. To make that page pull real data, you need `backend-java` and `ml-service` running somewhere reachable over HTTPS, then point the frontend at it.

### 1. Deploy `ml-service` (Python/FastAPI)

Any container host works (Render, Railway, Fly.io). Using **Render** as an example:

1. Push this repo to GitHub (if not already).
2. Render dashboard → **New → Web Service** → connect the repo.
3. Root directory: `ml-service`
4. Render auto-detects the `Dockerfile` — or set manually:
   - Build command: `pip install -r requirements.txt && python train_all.py`
   - Start command: `uvicorn main:app --host 0.0.0.0 --port $PORT`
5. Deploy. Note the resulting URL, e.g. `https://shelfwise-ml.onrender.com`.

### 2. Deploy `backend-java` (Spring Boot)

1. Render dashboard → **New → Web Service** → same repo, root directory `backend-java` (Dockerfile auto-detected).
2. **New → PostgreSQL** → create a free Postgres instance; copy its connection details.
3. Set environment variables on the `backend-java` service:

   | Variable | Value |
   |---|---|
   | `DB_HOST` | from the Render Postgres instance |
   | `DB_PORT` | `5432` |
   | `DB_NAME` | your Postgres database name |
   | `DB_USER` | your Postgres user |
   | `DB_PASSWORD` | your Postgres password |
   | `ML_SERVICE_URL` | the `ml-service` URL from step 1, e.g. `https://shelfwise-ml.onrender.com` |

4. Deploy. Note the resulting URL, e.g. `https://shelfwise-backend.onrender.com`.
5. Sanity check: `curl https://shelfwise-backend.onrender.com/api/health` should return `{"status":"ok","mlService":"up"}`.

> **CORS is already open** in `WebConfig.java` (`allowedOriginPatterns("*")` on `/api/**`), so no extra config is needed for the GitHub Pages origin to call it.

### 3. Point the GitHub Pages frontend at the live backend

Add one line before the closing `</body>` (or right after `<script>` opens) in `frontend/index.html`, then commit and push — GitHub Pages redeploys automatically:

```html
<script>window.SHELFWISE_API_BASE = 'https://shelfwise-backend.onrender.com';</script>
```

Once deployed, reload the [live demo](https://rishanthdeveloper.github.io/shelfwise-ai-retail-erp/) — the Dashboard, Expiry Alerts, and AI Advisor will now show real Postgres data and real model predictions instead of the offline fallback.

> **Free-tier note:** Render's free web services spin down after inactivity and take ~30–60s to wake up on the first request — the frontend's fallback logic means visitors just see demo data briefly instead of an error while it wakes up.

---

## What's live vs. illustrative in the frontend

To keep the scope honest for a portfolio project:

- **Live-wired to the real backend** (with automatic fallback to demo data if the API isn't running): Dashboard KPIs, Expiry Alerts, and the AI Advisor (`/api/ai/recommend`).
- **Illustrative demo data**: B2B hub, Fraud Watch list, Analytics charts, Suppliers, and Settings pages use static example data — the corresponding real endpoints (`/api/b2b/recommend`, `/api/fraud/detect`, `/api/analytics/*`) are fully implemented and testable directly (e.g. via `curl` or the OpenAPI docs), they're just not yet wired into every corner of the UI.

This is a natural next contribution if you extend the project.

---

## Tech stack

- **Backend:** Java 17, Spring Boot 3.3 (Web, Data JPA, WebFlux/WebClient, Validation, Actuator), Lombok, PostgreSQL, Maven
- **ML service:** Python 3.11, FastAPI, PyTorch (DQN), XGBoost, scikit-learn (Ridge, Isolation Forest, NMF), pandas/numpy
- **Frontend:** Vanilla JS, Chart.js
- **Infra:** Docker, Docker Compose

---

## Highlights for a resume

- Designed and built a **polyglot microservice architecture** separating a Java/Spring Boot business API from a Python ML inference service, communicating over REST with resilient rule-based fallbacks.
- Implemented a **Deep Q-Network** in PyTorch to learn a dynamic discount policy for perishable inventory, trained in a custom Gym-style retail environment.
- Modeled a relational schema (products, batches, B2B customers/transactions) in **JPA/Hibernate** with computed analytics (80/20 Pareto analysis, waste-by-category, live KPIs) instead of static reports.
- Containerized the full stack with **Docker Compose** for one-command local orchestration.

---

## License

MIT — use freely for learning, portfolio, or as a starting point for your own retail ERP.
