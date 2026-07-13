package com.shelfwise.service;

import com.shelfwise.dto.FraudDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class FraudService {

    private final MlClientService mlClientService;

    public FraudDto.Response detect(FraudDto.Request req) {
        Map<String, Object> resp = mlClientService.fraud(
                req.getQuantity(), req.getPrice(), req.getHour(),
                req.getTxnCount7d(), req.getPaymentMode(), req.getCustomerFreq());

        double anomalyScore = ((Number) resp.getOrDefault("anomaly_score", 0.1)).doubleValue();
        boolean flagged = anomalyScore > 0.65;

        String reason;
        if (req.getHour() < 6 && req.getQuantity() > 200) {
            reason = "High volume at odd hour";
        } else if (req.getQuantity() > 500) {
            reason = "Unusually large quantity";
        } else if (req.getTxnCount7d() < 2) {
            reason = "New customer pattern";
        } else {
            reason = "";
        }

        return FraudDto.Response.builder()
                .flagged(flagged)
                .anomalyScore(Math.round(anomalyScore * 1000.0) / 1000.0)
                .riskLevel(anomalyScore > 0.8 ? "high" : anomalyScore > 0.5 ? "medium" : "low")
                .reason(reason)
                .build();
    }
}
