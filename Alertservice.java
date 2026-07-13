package com.shelfwise.service;

import com.shelfwise.dto.DashboardDto;
import com.shelfwise.entity.Batch;
import com.shelfwise.repository.BatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AlertService {

    private static final int LOOKAHEAD_DAYS = 7;

    private final BatchRepository batchRepository;
    private final MlClientService mlClientService;

    public List<DashboardDto.ExpiryAlert> expiryAlerts() {
        LocalDate cutoff = LocalDate.now().plusDays(LOOKAHEAD_DAYS);
        List<Batch> batches = batchRepository.findNearExpiry(cutoff);

        return batches.stream().map(batch -> {
            long daysLeft = Math.max(0, batch.daysLeft());
            int categoryId = ProductService.categoryIdOf(batch.getProduct().getCategory());

            Map<String, Object> discountResp = mlClientService.discount(
                    (int) daysLeft,
                    batch.getCurrentQty(),
                    batch.getCurrentQty() / (double) Math.max(1, batch.getProduct().getShelfLifeDays()),
                    batch.getPriceChangeCount(),
                    categoryId,
                    batch.getProduct().getShelfLifeDays());
            int discount = ((Number) discountResp.getOrDefault("discount", 0)).intValue();

            double wasteValue = batch.getCurrentQty() * batch.getProduct().getBasePrice();
            double discountedPrice = batch.getProduct().getBasePrice() * (1 - discount / 100.0);
            double estimatedRecoverable = batch.getCurrentQty()
                    * (discountedPrice - batch.getProduct().getCost());

            return DashboardDto.ExpiryAlert.builder()
                    .batchId(batch.getId())
                    .productId(batch.getProduct().getId())
                    .product(batch.getProduct().getName())
                    .category(batch.getProduct().getCategory())
                    .daysLeft(daysLeft)
                    .stock(batch.getCurrentQty())
                    .unitPrice(batch.getProduct().getBasePrice())
                    .unitCost(batch.getProduct().getCost())
                    .recommendedDiscount(discount)
                    .wasteValue(Math.round(wasteValue * 100.0) / 100.0)
                    .estimatedRecoverable(Math.round(Math.max(0, estimatedRecoverable) * 100.0) / 100.0)
                    .build();
        }).sorted((a, b) -> Long.compare(a.getDaysLeft(), b.getDaysLeft())).toList();
    }
}
