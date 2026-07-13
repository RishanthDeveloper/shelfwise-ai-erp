package com.shelfwise.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class AnalyticsDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParetoRow {
        private Long productId;
        private String productName;
        private long sold;
        private double cumulativePct;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParetoResponse {
        private List<ParetoRow> data;
        private int cutoff;
        private int total;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WasteByCategory {
        private String category;
        private long wastedUnits;
    }
}
