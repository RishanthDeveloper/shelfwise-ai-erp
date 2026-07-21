package com.shelfwise.service;

import com.shelfwise.dto.FraudDetectRequestDTO;
import com.shelfwise.dto.FraudDetectResponseDTO;
import com.shelfwise.model.Transaction;
import com.shelfwise.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudService {

    private final TransactionRepository transactionRepo;
    private final WebClient.Builder webClientBuilder;

    @Value("${shelfwise.ml-service.url:http://localhost:8000}")
    private String mlServiceUrl;

    public FraudDetectResponseDTO detectFraud(FraudDetectRequestDTO request) {
        FraudDetectResponseDTO response;
        try {
            response = webClientBuilder.build()
                    .post()
                    .uri(mlServiceUrl + "/fraud/detect")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(FraudDetectResponseDTO.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
        } catch (Exception ex) {
            log.warn("ML service unavailable for fraud detection, using heuristic fallback.");
            response = heuristicFraudDetect(request);
        }

        // Persist transaction record
        assert response != null;
        Transaction tx = Transaction.builder()
                .transactionId(UUID.randomUUID().toString())
                .customerId(request.getCustomerId())
                .amount(request.getAmount())
                .avgPurchaseValue(request.getAvgPurchaseValue())
                .purchaseFrequency(request.getPurchaseFrequency())
                .hourOfDay(request.getHourOfDay())
                .isWeekend(request.getIsWeekend())
                .itemsCount(request.getItemsCount())
                .isFraud(response.isFraud())
                .anomalyScore(response.getAnomalyScore())
                .fraudProbability(response.getFraudProbability())
                .flaggedReason(response.getFlaggedReason())
                .build();
        transactionRepo.save(tx);
        return response;
    }

    public List<Transaction> getFlaggedTransactions() {
        return transactionRepo.findByIsFraudTrue();
    }

    /** Heuristic fallback when Python ML service is offline */
    private FraudDetectResponseDTO heuristicFraudDetect(FraudDetectRequestDTO req) {
        double ratio = req.getAmount() / Math.max(req.getAvgPurchaseValue(), 1.0);
        double anomalyScore = Math.min(ratio / 3.0, 1.0);
        boolean isOddHour = req.getHourOfDay() < 6 || req.getHourOfDay() > 22;
        boolean highAmount = ratio > 3.0;
        boolean unusualItems = req.getItemsCount() > 20;

        boolean isFraud = (highAmount && isOddHour) || anomalyScore > 0.75;
        double fraudProb = Math.min(anomalyScore * 0.9 + (isOddHour ? 0.1 : 0.0), 1.0);

        String reason = isFraud
                ? (highAmount ? "Amount 3x above average" : "") + (isOddHour ? " + Off-hours transaction" : "")
                : "Within normal parameters";

        String riskLevel = fraudProb > 0.8 ? "CRITICAL"
                : fraudProb > 0.6 ? "HIGH"
                : fraudProb > 0.35 ? "MEDIUM" : "LOW";

        FraudDetectResponseDTO resp = new FraudDetectResponseDTO();
        resp.setFraud(isFraud);
        resp.setFraudProbability(Math.round(fraudProb * 100.0) / 100.0);
        resp.setAnomalyScore(Math.round(anomalyScore * 100.0) / 100.0);
        resp.setFlaggedReason(reason);
        resp.setRiskLevel(riskLevel);
        resp.setModelSource("fallback");
        return resp;
    }
}
