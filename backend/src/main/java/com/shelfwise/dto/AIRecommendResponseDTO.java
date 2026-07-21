package com.shelfwise.dto;

import lombok.Data;
import java.util.List;

/**
 * Response from POST /api/ai/recommend (proxied from Python ML microservice)
 */
@Data
public class AIRecommendResponseDTO {
    private double discountFactor;
    private String action;
    private double predictedDemand;
    private double anomalyScore;
    private boolean isAnomaly;
    private List<Double> qValues;
    private String rationale;
    private String modelSource;   // "live" | "fallback"
}
