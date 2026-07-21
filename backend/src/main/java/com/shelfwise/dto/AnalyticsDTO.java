package com.shelfwise.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * Analytics payload returned by GET /api/analytics
 */
@Data
@Builder
public class AnalyticsDTO {
    private List<String> months;
    private List<Double> revenueTrend;
    private List<Double> wasteReductionTrend;
    private Map<String, Integer> categorySalesDistribution;
    private double totalRevenue;
    private double wasteReduction;
    private double aiSavings;
    private double avgDiscount;
}
