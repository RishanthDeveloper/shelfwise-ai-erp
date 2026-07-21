"""
ShelfWise DQN Agent — Deep Q-Network for dynamic discount pricing
State: [daysLeft, stock, demandRate, price, cost, priceChanges, lag1, lag2, urgency, stockRatio]
Actions: 0=HOLD_PRICE, 1=MILD_DISCOUNT(10%), 2=DISCOUNT(25%), 3=DEEP_DISCOUNT(40%)
"""

import numpy as np
import random
from collections import deque

try:
    import torch
    import torch.nn as nn
    import torch.optim as optim
    TORCH_AVAILABLE = True
except ImportError:
    TORCH_AVAILABLE = False
    print("WARNING: PyTorch not available — DQN will be stub only.")


# ── Neural Network ────────────────────────────────────────────────────────────
if TORCH_AVAILABLE:
    class QNetwork(nn.Module):
        def __init__(self, state_size: int, action_size: int):
            super().__init__()
            self.net = nn.Sequential(
                nn.Linear(state_size, 128),
                nn.ReLU(),
                nn.Dropout(0.1),
                nn.Linear(128, 128),
                nn.ReLU(),
                nn.Dropout(0.1),
                nn.Linear(128, 64),
                nn.ReLU(),
                nn.Linear(64, action_size),
            )

        def forward(self, x):
            return self.net(x)


# ── Replay Buffer ─────────────────────────────────────────────────────────────
class ReplayBuffer:
    def __init__(self, capacity: int = 10_000):
        self.buffer = deque(maxlen=capacity)

    def push(self, state, action, reward, next_state, done):
        self.buffer.append((state, action, reward, next_state, done))

    def sample(self, batch_size: int):
        return random.sample(self.buffer, batch_size)

    def __len__(self):
        return len(self.buffer)


# ── DQN Agent ─────────────────────────────────────────────────────────────────
class DQNAgent:
    ACTIONS = ["HOLD_PRICE", "MILD_DISCOUNT", "DISCOUNT", "DEEP_DISCOUNT"]
    DISCOUNT_MAP = {0: 0.0, 1: 0.10, 2: 0.25, 3: 0.40}

    def __init__(self, state_size: int = 10, action_size: int = 4,
                 lr: float = 1e-3, gamma: float = 0.95,
                 epsilon: float = 1.0, epsilon_min: float = 0.01,
                 epsilon_decay: float = 0.995):
        self.state_size = state_size
        self.action_size = action_size
        self.gamma = gamma
        self.epsilon = epsilon
        self.epsilon_min = epsilon_min
        self.epsilon_decay = epsilon_decay

        self.memory = ReplayBuffer(10_000)

        if TORCH_AVAILABLE:
            self.q_network = QNetwork(state_size, action_size)
            self.target_network = QNetwork(state_size, action_size)
            self.target_network.load_state_dict(self.q_network.state_dict())
            self.optimizer = optim.Adam(self.q_network.parameters(), lr=lr)
            self.criterion = nn.MSELoss()
        else:
            self.q_network = None

    def act(self, state: np.ndarray) -> int:
        if not TORCH_AVAILABLE or random.random() < self.epsilon:
            return random.randint(0, self.action_size - 1)
        import torch
        with torch.no_grad():
            q = self.q_network(torch.FloatTensor(state).unsqueeze(0))
            return int(q.argmax().item())

    def remember(self, state, action, reward, next_state, done):
        self.memory.push(state, action, reward, next_state, done)

    def replay(self, batch_size: int = 64):
        if not TORCH_AVAILABLE or len(self.memory) < batch_size:
            return
        import torch
        batch = self.memory.sample(batch_size)
        states = torch.FloatTensor(np.array([b[0] for b in batch]))
        actions = torch.LongTensor([b[1] for b in batch])
        rewards = torch.FloatTensor([b[2] for b in batch])
        next_states = torch.FloatTensor(np.array([b[3] for b in batch]))
        dones = torch.FloatTensor([b[4] for b in batch])

        current_q = self.q_network(states).gather(1, actions.unsqueeze(1)).squeeze(1)
        with torch.no_grad():
            next_q = self.target_network(next_states).max(1)[0]
        target_q = rewards + (1 - dones) * self.gamma * next_q

        loss = self.criterion(current_q, target_q)
        self.optimizer.zero_grad()
        loss.backward()
        self.optimizer.step()

        if self.epsilon > self.epsilon_min:
            self.epsilon *= self.epsilon_decay

    def update_target(self):
        if TORCH_AVAILABLE:
            self.target_network.load_state_dict(self.q_network.state_dict())

    @staticmethod
    def compute_reward(action_idx: int, days_left: float, stock: int,
                       demand_rate: float, price: float, cost: float) -> float:
        """
        Reward shaping:
        - Selling expiring goods earns higher reward
        - Discounting below cost is penalized
        - Holding excess stock near expiry is penalized
        """
        discount = DQNAgent.DISCOUNT_MAP[action_idx]
        sell_price = price * (1 - discount)
        margin = sell_price - cost
        units_sold = demand_rate * (1 + discount * 2)
        revenue = margin * min(units_sold, stock)

        # Urgency penalty for holding stock near expiry
        urgency_penalty = 0.0
        if days_left <= 3 and action_idx == 0:
            urgency_penalty = -cost * stock * 0.5

        # Margin guard
        margin_penalty = -50.0 if sell_price < cost else 0.0

        return revenue + urgency_penalty + margin_penalty
