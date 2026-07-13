package com.shelfwise.service;

import com.shelfwise.dto.DashboardDto;
import com.shelfwise.repository.BatchRepository;
import com.shelfwise.repository.ProductRepository;
import com.shelfwise.repository.B2BTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ProductRepository productRepository;
    private final BatchRepository batchRepository;
    private final B2BTransactionRepository transactionRepository;
    private final AlertService alertService;

    public DashboardDto.Kpis kpis() {
        long totalSold = transactionRepository.sumAllQuantity();
        long initialQty = batchRepository.sumInitialQty();
        long currentQty = batchRepository.sumCurrentQty();

        double wastePct = initialQty == 0 ? 0
                : round1(batchRepository.findExpiredWithRemainingStock(LocalDate.now())
                        .stream().mapToLong(b -> b.getCurrentQty()).sum() * 100.0 / initialQty);

        double avgPrice = productRepository.findAll().stream()
                .mapToDouble(p -> p.getBasePrice()).average().orElse(0);

        List<DashboardDto.ExpiryAlert> alerts = alertService.expiryAlerts();
        double avgDiscount = alerts.isEmpty() ? 0
                : round1(alerts.stream().mapToInt(DashboardDto.ExpiryAlert::getRecommendedDiscount).average().orElse(0));

        double turnover = currentQty == 0 ? 0 : round1(totalSold / (double) Math.max(1, currentQty));

        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        double revenueMtd = round2(transactionRepository.sumRevenueSince(monthStart));

        double avgCost = productRepository.findAll().stream().mapToDouble(p -> p.getCost()).average().orElse(0);
        double profitMargin = avgPrice == 0 ? 0 : round1((avgPrice - avgCost) / avgPrice * 100);

        return DashboardDto.Kpis.builder()
                .totalSold(totalSold)
                .wastePct(wastePct)
                .avgPrice(round2(avgPrice))
                .avgDiscount(avgDiscount)
                .turnover(turnover)
                .revenueMtd(revenueMtd)
                .profitMargin(profitMargin)
                .build();
    }

    private double round1(double v) { return Math.round(v * 10.0) / 10.0; }
    private double round2(double v) { return Math.round(v * 100.0) / 100.0; }
}
