package com.shelfwise.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * Dashboard KPI summary returned by GET /api/dashboard/summary
 */
@Data
@Builder
public class DashboardSummaryDTO {
    private int totalProducts;
    private int expiringWithin7Days;
    private int lowStockItems;
    private int activeAlerts;
    private double totalInventoryValue;
    private double estimatedWasteValue;
    private double potentialSavings;
    private long fraudFlagged;
    private long pendingB2BOrders;
    private double b2bRevenue;
    private Map<String, Double> revenueByCategory;
    private List<String> topAlertMessages;
}
