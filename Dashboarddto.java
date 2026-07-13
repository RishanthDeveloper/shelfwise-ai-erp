package com.shelfwise.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class DashboardDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Kpis {
        private long totalSold;
        private double wastePct;
        private double avgPrice;
        private double avgDiscount;
        private double turnover;
        private double revenueMtd;
        private double profitMargin;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExpiryAlert {
        private Long batchId;
        private Long productId;
        private String product;
        private String category;
        private long daysLeft;
        private int stock;
        private double unitPrice;
        private double unitCost;
        private int recommendedDiscount;
        private double wasteValue;
        private double estimatedRecoverable;
    }
}
