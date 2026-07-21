package com.shelfwise.dto;

import lombok.Data;

@Data
public class FraudDetectResponseDTO {
    private boolean isFraud;
    private double fraudProbability;
    private double anomalyScore;
    private String flaggedReason;
    private String riskLevel;   // LOW | MEDIUM | HIGH | CRITICAL
    private String modelSource;
}
