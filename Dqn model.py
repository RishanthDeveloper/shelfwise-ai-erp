"""
ShelfWise ERP — DQN Model
=========================
Deep Q-Network for dynamic discount optimisation.
Three components:
  DQN          — neural network (state → Q-values for each discount action)
  ReplayBuffer — experience replay memory
  DQNAgent     — epsilon-greedy agent with target network
  RetailEnv    — single-step retail environment
"""

import torch
import torch.nn as nn
import torch.optim as optim
import numpy as np
from collections import deque
import random


class DQN(nn.Module):
    def __init__(self, state_size: int, action_size: int, hidden_size: int = 128):
        super().__init__()
        self.network = nn.Sequential(
            nn.Linear(state_size, hidden_size),
            nn.ReLU(),
            nn.Linear(hidden_size, hidden_size),
            nn.ReLU(),
            nn.Linear(hidden_size, action_size),
        )

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        return self.network(x)


class ReplayBuffer:
    def __init__(self, capacity: int = 10_000):
        self.buffer: deque = deque(maxlen=capacity)

    def push(self, state, action, reward, next_state, done):
        self.buffer.append((state, action, reward, next_state, done))

    def sample(self, batch_size: int):
        batch = random.sample(self.buffer, batch_size)
        states, actions, rewards, next_states, dones = zip(*batch)
        return (
            np.array(states),
            np.array(actions),
            np.array(rewards),
            np.array(next_states),
            np.array(dones),
        )

    def __len__(self) -> int:
        return len(self.buffer)


class DQNAgent:
    def __init__(
        self,
        state_size: int,
        action_size: int,
        lr: float = 0.001,
        gamma: float = 0.99,
        epsilon: float = 1.0,
        epsilon_min: float = 0.01,
        epsilon_decay: float = 0.995,
    ):
        self.state_size    = state_size
        self.action_size   = action_size
        self.gamma         = gamma
        self.epsilon       = epsilon
        self.epsilon_min   = epsilon_min
        self.epsilon_decay = epsilon_decay
        self.device        = torch.device("cuda" if torch.cuda.is_available() else "cpu")

        self.policy_net = DQN(state_size, action_size).to(self.device)
        self.target_net = DQN(state_size, action_size).to(self.device)
        self.target_net.load_state_dict(self.policy_net.state_dict())
        self.target_net.eval()

        self.optimizer = optim.Adam(self.policy_net.parameters(), lr=lr)
        self.memory    = ReplayBuffer()
        self.loss_fn   = nn.MSELoss()

    def act(self, state, eval_mode: bool = False) -> int:
        if not eval_mode and np.random.rand() < self.epsilon:
            return np.random.randint(self.action_size)
        state_tensor = torch.FloatTensor(state).unsqueeze(0).to(self.device)
        with torch.no_grad():
            q_values = self.policy_net(state_tensor)
        return int(torch.argmax(q_values).item())

    def remember(self, state, action, reward, next_state, done):
        self.memory.push(state, action, reward, next_state, done)

    def replay(self, batch_size: int = 64):
        if len(self.memory) < batch_size:
            return
        states, actions, rewards, next_states, dones = self.memory.sample(batch_size)

        states      = torch.FloatTensor(states).to(self.device)
        actions     = torch.LongTensor(actions).unsqueeze(1).to(self.device)
        rewards     = torch.FloatTensor(rewards).unsqueeze(1).to(self.device)
        next_states = torch.FloatTensor(next_states).to(self.device)
        dones       = torch.FloatTensor(dones).unsqueeze(1).to(self.device)

        current_q = self.policy_net(states).gather(1, actions)
        next_q    = self.target_net(next_states).max(1)[0].unsqueeze(1)
        target_q  = rewards + (1 - dones) * self.gamma * next_q

        loss = self.loss_fn(current_q, target_q)
        self.optimizer.zero_grad()
        loss.backward()
        self.optimizer.step()

        if self.epsilon > self.epsilon_min:
            self.epsilon *= self.epsilon_decay

    def update_target(self):
        self.target_net.load_state_dict(self.policy_net.state_dict())


class RetailEnv:
    """
    Single-step retail environment for one product batch.

    State vector (5-dim, all normalised to [0,1]):
        [days_left/shelf_life, stock/200, demand_rate/50,
         price_change_count/10, category_id/7]

    Actions: 0–5  →  discount = action × 10%  (0%, 10%, …, 50%)

    Reward = profit
           − trust_penalty  (if too many price changes + high discount)
           − waste_penalty   (if near expiry + low discount)
    """

    def __init__(self, batch_dict: dict):
        d = batch_dict
        self.price              = float(d["price"])
        self.cost               = float(d["cost"])
        self.days_left          = int(d["days_left"])
        self.stock              = int(d["stock"])
        self.demand_rate        = float(d["demand_rate"])
        self.price_change_count = int(d["price_change_count"])
        self.shelf_life         = max(1, int(d["shelf_life"]))
        self.category_id        = int(d["category_id"])

        self.state = np.array([
            self.days_left / self.shelf_life,
            self.stock / 200.0,
            self.demand_rate / 50.0,
            min(1.0, self.price_change_count / 10.0),
            self.category_id / 7.0,
        ], dtype=np.float32)

    def step(self, action: int):
        discount         = action * 0.1
        discounted_price = self.price * (1 - discount)
        demand_boost     = 1 + discount
        expected_sales   = min(self.stock, int(self.demand_rate * demand_boost))

        revenue    = expected_sales * discounted_price
        cost_goods = expected_sales * self.cost
        profit     = revenue - cost_goods

        trust_penalty = 0.0
        if self.price_change_count > 3 and discount > 0.2:
            trust_penalty = -5 * (discount * 100)

        waste_penalty = 0.0
        if self.days_left < 2 and discount < 0.3:
            waste_penalty = -10 * (self.stock / 10.0)

        reward = profit + trust_penalty + waste_penalty

        self.days_left          = max(0, self.days_left - 1)
        self.stock              = max(0, self.stock - expected_sales)
        self.price_change_count += 1 if discount > 0 else 0

        next_state = np.array([
            self.days_left / self.shelf_life,
            self.stock / 200.0,
            self.demand_rate / 50.0,
            min(1.0, self.price_change_count / 10.0),
            self.category_id / 7.0,
        ], dtype=np.float32)

        done = (self.stock <= 0) or (self.days_left <= 0)
        return next_state, reward, done
