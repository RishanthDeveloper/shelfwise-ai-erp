package com.shelfwise.service;

import com.shelfwise.dto.DashboardSummaryDTO;
import com.shelfwise.model.Alert;
import com.shelfwise.repository.AlertRepository;
import com.shelfwise.repository.B2BOrderRepository;
import com.shelfwise.repository.ProductRepository;
import com.shelfwise.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final ProductRepository productRepo;
    private final AlertRepository alertRepo;
    private final TransactionRepository transactionRepo;
    private final B2BOrderRepository b2bRepo;

    public DashboardSummaryDTO getSummary() {
        LocalDate today = LocalDate.now();
        LocalDate in7Days = today.plusDays(7);

        int totalProducts = (int) productRepo.count();
        int expiringWithin7Days = productRepo.findExpiringBefore(today, in7Days).size();
        int lowStockItems = productRepo.findLowStockProducts().size();
        int activeAlerts = alertRepo.findByIsResolvedFalseOrderByCreatedAtDesc().size();

        Double inventoryValue = productRepo.getTotalInventoryValue();
        double totalInventoryValue = inventoryValue != null ? inventoryValue : 0.0;

        // Estimate waste: expiring products * cost * stock
        double estimatedWaste = productRepo.findExpiringBefore(today, in7Days)
                .stream()
                .mapToDouble(p -> p.getCost() * p.getStockQty())
                .sum();

        // Potential savings from AI-recommended discounts (30% of waste recovered)
        double potentialSavings = estimatedWaste * 0.30;

        long fraudFlagged = transactionRepo.countFraudulentTransactions();
        long pendingB2B = b2bRepo.countPendingOrders();
        Double b2bRev = b2bRepo.getTotalB2BRevenue();

        // Revenue by category map
        Map<String, Double> revenueByCategory = new LinkedHashMap<>();
        productRepo.getRevenueByCategory().forEach(row ->
                revenueByCategory.put((String) row[0], (Double) row[1]));

        // Top 3 alert messages for quick view
        List<String> topAlerts = alertRepo.findByIsResolvedFalseOrderByCreatedAtDesc()
                .stream()
                .limit(3)
                .map(Alert::getMessage)
                .collect(Collectors.toList());

        return DashboardSummaryDTO.builder()
                .totalProducts(totalProducts)
                .expiringWithin7Days(expiringWithin7Days)
                .lowStockItems(lowStockItems)
                .activeAlerts(activeAlerts)
                .totalInventoryValue(totalInventoryValue)
                .estimatedWasteValue(estimatedWaste)
                .potentialSavings(potentialSavings)
                .fraudFlagged(fraudFlagged)
                .pendingB2BOrders(pendingB2B)
                .b2bRevenue(b2bRev != null ? b2bRev : 0.0)
                .revenueByCategory(revenueByCategory)
                .topAlertMessages(topAlerts)
                .build();
    }
}
