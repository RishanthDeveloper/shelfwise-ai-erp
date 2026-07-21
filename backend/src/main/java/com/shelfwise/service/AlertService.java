package com.shelfwise.service;

import com.shelfwise.model.Alert;
import com.shelfwise.model.Alert.AlertSeverity;
import com.shelfwise.model.Alert.AlertType;
import com.shelfwise.model.Product;
import com.shelfwise.repository.AlertRepository;
import com.shelfwise.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {

    private final ProductRepository productRepo;
    private final AlertRepository alertRepo;

    /** Run every hour to refresh alerts */
    @Scheduled(fixedRate = 3_600_000)
    @Transactional
    public void refreshAlerts() {
        log.info("Refreshing ERP alerts...");
        generateExpiryAlerts();
        generateStockAlerts();
        log.info("Alert refresh complete.");
    }

    @Transactional
    public List<Alert> generateExpiryAlerts() {
        LocalDate today = LocalDate.now();
        LocalDate cutoff = today.plusDays(7);
        List<Product> expiring = productRepo.findExpiringBefore(today, cutoff);
        List<Alert> created = new ArrayList<>();

        for (Product p : expiring) {
            long daysLeft = p.getDaysLeft();
            // Skip if already alerted for this product
            if (!alertRepo.findByProductIdAndIsResolvedFalse(p.getId()).isEmpty()) continue;

            AlertSeverity severity = daysLeft <= 1 ? AlertSeverity.CRITICAL
                    : daysLeft <= 3 ? AlertSeverity.HIGH
                    : AlertSeverity.MEDIUM;

            double wasteValue = p.getCost() * p.getStockQty();
            // Heuristic recommended discount based on urgency
            double discount = daysLeft <= 1 ? 0.50 : daysLeft <= 3 ? 0.30 : 0.15;

            Alert alert = Alert.builder()
                    .productId(p.getId())
                    .productName(p.getName())
                    .sku(p.getSku())
                    .type(AlertType.EXPIRY)
                    .severity(severity)
                    .message(String.format("%s expires in %d day(s) — apply %.0f%% discount to clear %d units",
                            p.getName(), daysLeft, discount * 100, p.getStockQty()))
                    .daysLeft(daysLeft)
                    .stockQty(p.getStockQty())
                    .recommendedDiscount(discount)
                    .wasteValue(wasteValue)
                    .isResolved(false)
                    .build();

            created.add(alertRepo.save(alert));
        }
        return created;
    }

    @Transactional
    public List<Alert> generateStockAlerts() {
        List<Product> lowStock = productRepo.findLowStockProducts();
        List<Alert> created = new ArrayList<>();

        for (Product p : lowStock) {
            if (!alertRepo.findByProductIdAndIsResolvedFalse(p.getId()).isEmpty()) continue;

            Alert alert = Alert.builder()
                    .productId(p.getId())
                    .productName(p.getName())
                    .sku(p.getSku())
                    .type(AlertType.LOW_STOCK)
                    .severity(AlertSeverity.HIGH)
                    .message(String.format("%s is low on stock (%d units, reorder point: %d)",
                            p.getName(), p.getStockQty(), p.getReorderPoint()))
                    .stockQty(p.getStockQty())
                    .isResolved(false)
                    .build();

            created.add(alertRepo.save(alert));
        }
        return created;
    }

    public List<Alert> getActiveAlerts() {
        return alertRepo.findByIsResolvedFalseOrderByCreatedAtDesc();
    }

    public List<Alert> getExpiryAlerts() {
        return alertRepo.findByTypeAndIsResolvedFalse(AlertType.EXPIRY);
    }

    @Transactional
    public Alert resolveAlert(Long id) {
        Alert alert = alertRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Alert not found: " + id));
        alert.setIsResolved(true);
        return alertRepo.save(alert);
    }
}
