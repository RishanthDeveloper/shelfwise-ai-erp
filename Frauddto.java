package com.shelfwise.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class FraudDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        @NotNull
        private Long customerId;
        @NotNull
        private Long productId;
        @Positive
        private int quantity;
        @Positive
        private double price;
        private String paymentMode = "cash";
        @Min(0) @Max(23)
        private int hour = 12;
        @Min(0)
        private int txnCount7d = 5;
        @Min(0)
        private int customerFreq = 10;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private boolean flagged;
        private double anomalyScore;
        private String riskLevel;
        private String reason;
    }
}
