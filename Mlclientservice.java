package com.shelfwise.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Thin client around the Python ML microservice (FastAPI + PyTorch/scikit-learn).
 * The Java side owns business logic and orchestration; this class only ever
 * ships raw numeric features out and gets model predictions back.
 * <p>
 * Every call degrades gracefully to a simple rule-based fallback if the ML
 * microservice is unreachable or slow, so the ERP core never goes down just
 * because the model server does.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MlClientService {

    private final WebClient mlServiceWebClient;

    public Map<String, Object> expiryRisk(int daysLeft, int stock, double demandRate, double price, double cost) {
        Map<String, Object> body = Map.of(
                "days_left", daysLeft, "stock", stock,
                "demand_rate", demandRate, "price", price, "cost", cost);
        try {
            return post("/predict/expiry-risk", body);
        } catch (Exception e) {
            log.warn("ML service unreachable (expiry-risk), falling back to rule-based: {}", e.getMessage());
            double score = Math.max(0.01, Math.min(0.99, (10.0 - daysLeft) / 10.0));
            if (stock > 80) score = Math.min(0.99, score + 0.15);
            return Map.of("risk", round3(score), "model", "rule-based");
        }
    }

    public Map<String, Object> demandForecast(double lag1, double lag2, int daysLeft, double price) {
        Map<String, Object> body = Map.of(
                "lag1", lag1, "lag2", lag2, "days_left", daysLeft, "price", price);
        try {
            return post("/predict/demand", body);
        } catch (Exception e) {
            log.warn("ML service unreachable (demand), falling back to rule-based: {}", e.getMessage());
            return Map.of("forecast", Math.max(1, (int) Math.round((lag1 + lag2) / 2.0)), "model", "rule-based");
        }
    }

    public Map<String, Object> discount(int daysLeft, int stock, double demandRate, int priceChangeCount,
                                         int categoryId, int shelfLife) {
        Map<String, Object> body = Map.of(
                "days_left", daysLeft, "stock", stock, "demand_rate", demandRate,
                "price_change_count", priceChangeCount, "category_id", categoryId, "shelf_life", shelfLife);
        try {
            return post("/predict/discount", body);
        } catch (Exception e) {
            log.warn("ML service unreachable (discount), falling back to rule-based: {}", e.getMessage());
            return Map.of("discount", ruleBasedDiscount(daysLeft), "model", "rule-based");
        }
    }

    public Map<String, Object> fraud(int quantity, double price, int hour, int txnCount7d,
                                      String paymentMode, int customerFreq) {
        Map<String, Object> body = Map.of(
                "quantity", quantity, "price", price, "hour", hour,
                "txn_count_7d", txnCount7d, "payment_mode", paymentMode, "customer_freq", customerFreq);
        try {
            return post("/predict/fraud", body);
        } catch (Exception e) {
            log.warn("ML service unreachable (fraud), falling back to rule-based: {}", e.getMessage());
            double anomaly = (hour < 5 && quantity > 300) ? 0.9 : 0.1;
            return Map.of("anomaly_score", anomaly, "model", "rule-based");
        }
    }

    @SuppressWarnings("unchecked")
    public List<Long> b2bRecommend(long customerId, int topN) {
        Map<String, Object> body = Map.of("customer_id", customerId, "top_n", topN);
        try {
            Map<String, Object> resp = post("/predict/b2b-recommend", body);
            List<?> raw = (List<?>) resp.get("recommended_ids");
            return raw.stream().map(o -> ((Number) o).longValue()).toList();
        } catch (Exception e) {
            log.warn("ML service unreachable (b2b-recommend), falling back to top popular ids: {}", e.getMessage());
            return List.of(1L, 2L, 3L, 4L, 5L).subList(0, Math.min(topN, 5));
        }
    }

    private int ruleBasedDiscount(int daysLeft) {
        if (daysLeft <= 1) return 50;
        if (daysLeft <= 2) return 40;
        if (daysLeft <= 4) return 30;
        if (daysLeft <= 7) return 15;
        if (daysLeft <= 10) return 5;
        return 0;
    }

    private double round3(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> post(String path, Map<String, Object> body) {
        return mlServiceWebClient.post()
                .uri(path)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }
}
