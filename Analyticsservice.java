package com.shelfwise.service;

import com.shelfwise.dto.AnalyticsDto;
import com.shelfwise.entity.Batch;
import com.shelfwise.repository.B2BTransactionRepository;
import com.shelfwise.repository.BatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final B2BTransactionRepository transactionRepository;
    private final BatchRepository batchRepository;

    public AnalyticsDto.ParetoResponse pareto() {
        List<B2BTransactionRepository.ProductSalesRow> rows = transactionRepository.aggregateSalesByProduct();
        long total = rows.stream().mapToLong(B2BTransactionRepository.ProductSalesRow::getTotalQty).sum();

        List<AnalyticsDto.ParetoRow> data = new ArrayList<>();
        long running = 0;
        int cutoff = 0;
        boolean cutoffFound = false;

        for (B2BTransactionRepository.ProductSalesRow row : rows) {
            running += row.getTotalQty();
            double cumulativePct = total == 0 ? 0 : Math.round(running * 1000.0 / total) / 10.0;
            data.add(AnalyticsDto.ParetoRow.builder()
                    .productId(row.getProductId())
                    .productName(row.getProductName())
                    .sold(row.getTotalQty())
                    .cumulativePct(cumulativePct)
                    .build());
            if (!cutoffFound) {
                cutoff++;
                if (cumulativePct >= 80.0) cutoffFound = true;
            }
        }

        return AnalyticsDto.ParetoResponse.builder()
                .data(data.size() > 20 ? data.subList(0, 20) : data)
                .cutoff(cutoff)
                .total(rows.size())
                .build();
    }

    public List<AnalyticsDto.WasteByCategory> wasteByCategory() {
        List<Batch> expired = batchRepository.findExpiredWithRemainingStock(LocalDate.now());

        Map<String, Long> byCategory = new LinkedHashMap<>();
        for (Batch b : expired) {
            byCategory.merge(b.getProduct().getCategory(), (long) b.getCurrentQty(), Long::sum);
        }

        return byCategory.entrySet().stream()
                .map(e -> AnalyticsDto.WasteByCategory.builder()
                        .category(e.getKey())
                        .wastedUnits(e.getValue())
                        .build())
                .toList();
    }
}
