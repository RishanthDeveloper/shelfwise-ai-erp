# ShelfWise: AI-Based Retail ERP

An intelligent retail Enterprise Resource Planning (ERP) system focused on AI-driven expiry management and inventory optimization for physical retailers.

## 📌 Overview

ShelfWise leverages advanced machine learning algorithms to revolutionize how physical retail stores manage their on-shelf inventory. By accurately predicting product expiry risks and automating stock management decisions, the system minimizes waste, reduces financial losses, and maximizes operational efficiency.

## 🧠 Core AI Architecture

ShelfWise utilizes a dual-model artificial intelligence approach to handle inventory lifecycle management:

* **Risk Classification (XGBoost):** Analyzes historical sales data, seasonal trends, and batch information to classify the expiration risk of current inventory items. 
* **Dynamic Decision Making (Deep Q-Networks):** Implements a reinforcement learning environment (DQN) that continuously learns the optimal actions (e.g., dynamic discounting, automated restocking, or promotional bundling) to clear high-risk inventory before expiration.

## 🚀 Features

* **Real-Time Expiry Tracking:** Dashboard for monitoring the health and shelf-life of all active stock.
* **Automated Markdown Strategies:** AI-suggested pricing adjustments based on real-time risk scores.
* **ERP Integration:** Seamlessly connects with existing point-of-sale (POS) and supply chain databases.

## 🛠️ Installation & Setup

1. **Clone the repository:**
   ```bash
   git clone [https://github.com/your-username/ShelfWise.git](https://github.com/your-username/ShelfWise.git)
   cd ShelfWise
