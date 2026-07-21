package com.shelfwise.service;

import com.shelfwise.dto.AnalyticsDTO;
import com.shelfwise.repository.ProductRepository;
import com.shelfwise.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Month;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final ProductRepository productRepo;
    private final TransactionRepository transactionRepo;

    public AnalyticsDTO getAnalytics() {
        // Generate simulated monthly trend data (last 6 months)
        List<String> months = new ArrayList<>();
        List<Double> revenue = new ArrayList<>();
        List<Double> wasteReduction = new ArrayList<>();

        Random rng = new Random(42);
        java.time.LocalDate today = java.time.LocalDate.now();
        for (int i = 5; i >= 0; i--) {
            java.time.LocalDate m = today.minusMonths(i);
            months.add(m.getMonth().getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH)
                    + " " + m.getYear());
            revenue.add(80000 + rng.nextDouble() * 40000);
            wasteReduction.add(15 + rng.nextDouble() * 20);
        }

        // Category distribution from real inventory
        Map<String, Integer> catDist = new LinkedHashMap<>();
        productRepo.getRevenueByCategory().forEach(row ->
                catDist.put((String) row[0], (int) Math.round((Double) row[1])));

        Double inventoryValue = productRepo.getTotalInventoryValue();
        double totalRev = inventoryValue != null ? inventoryValue * 1.35 : 0.0;

        return AnalyticsDTO.builder()
                .months(months)
                .revenueTrend(revenue)
                .wasteReductionTrend(wasteReduction)
                .categorySalesDistribution(catDist)
                .totalRevenue(totalRev)
                .wasteReduction(32.5)
                .aiSavings(totalRev * 0.08)
                .avgDiscount(18.7)
                .build();
    }
}
