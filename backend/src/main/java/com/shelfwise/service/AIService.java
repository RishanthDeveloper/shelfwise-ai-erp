package com.shelfwise.service;

import com.shelfwise.dto.AIRecommendRequestDTO;
import com.shelfwise.dto.AIRecommendResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIService {

    private final WebClient.Builder webClientBuilder;

    @Value("${shelfwise.ml-service.url:http://localhost:8000}")
    private String mlServiceUrl;

    /**
     * Calls Python ML microservice for pricing/discount recommendation.
     * Falls back to a rule-based heuristic if the ML service is unavailable.
     */
    public AIRecommendResponseDTO recommend(AIRecommendRequestDTO request) {
        try {
            return webClientBuilder.build()
                    .post()
                    .uri(mlServiceUrl + "/recommend")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(AIRecommendResponseDTO.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
        } catch (Exception ex) {
            log.warn("ML service unavailable ({}), using fallback heuristic.", ex.getMessage());
            return fallbackRecommend(request);
        }
    }

    /**
     * Rule-based fallback — no external dependency required.
     * Mirrors the JS simulation in the original frontend.
     */
    private AIRecommendResponseDTO fallbackRecommend(AIRecommendRequestDTO req) {
        double urgency = req.getDaysLeft() / (double) req.getShelfLife();
        double stockRatio = req.getStock() / Math.max(req.getDemandRate() * req.getDaysLeft(), 1.0);

        double discount;
        String action;
        if (urgency < 0.2 || stockRatio > 2.5) {
            discount = 0.40;
            action = "DEEP_DISCOUNT";
        } else if (urgency < 0.4) {
            discount = 0.25;
            action = "DISCOUNT";
        } else if (urgency > 0.7 && stockRatio < 0.5) {
            discount = 0.0;
            action = "HOLD_PRICE";
        } else {
            discount = 0.10;
            action = "MILD_DISCOUNT";
        }

        double predictedDemand = req.getDemandRate() * (1 + discount);
        double anomalyScore = Math.abs(req.getPrice() - req.getCost()) / Math.max(req.getCost(), 1.0);
        boolean isAnomaly = anomalyScore > 2.0;

        // Simulated Q-values for the 4 discount actions
        List<Double> qValues = Arrays.asList(
                round(0.3 + Math.random() * 0.2),
                round(0.5 + Math.random() * 0.3),
                round(0.4 + Math.random() * 0.2),
                round(0.2 + Math.random() * 0.1)
        );

        String rationale = String.format(
                "Urgency: %.0f%% | Stock coverage: %.1f days | DQN action: %s",
                (1 - urgency) * 100, req.getStock() / Math.max(req.getDemandRate(), 0.01), action);

        AIRecommendResponseDTO response = new AIRecommendResponseDTO();
        response.setDiscountFactor(discount);
        response.setAction(action);
        response.setPredictedDemand(predictedDemand);
        response.setAnomalyScore(anomalyScore);
        response.setAnomaly(isAnomaly);
        response.setQValues(qValues);
        response.setRationale(rationale);
        response.setModelSource("fallback");
        return response;
    }

    private double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
