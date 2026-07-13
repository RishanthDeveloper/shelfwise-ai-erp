package com.shelfwise.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class RecommendDto {

    /** Mirrors the original Python ProductInput schema so existing clients need no changes. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        @Min(0) @Max(365)
        private int daysLeft;
        @Min(0)
        private int stock;
        @Min(0)
        private double demandRate;
        @Positive
        private double price;
        @Positive
        private double cost;
        @Min(0) @Max(20)
        private int priceChangeCount = 0;
        @Min(0)
        private double lag1 = 10;
        @Min(0)
        private double lag2 = 10;
        private String category = "Dairy";
        @Min(1)
        private int shelfLife = 14;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private double expiryRisk;
        private String riskLabel;
        private int demandForecast;
        private double daysCover;
        private int recommendedDiscount;
        private double suggestedPrice;
        private double expectedRevenue;
        private double expectedProfit;
        private ModelsUsed modelsUsed;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModelsUsed {
        private String expiry;
        private String demand;
        private String discount;
    }
}
