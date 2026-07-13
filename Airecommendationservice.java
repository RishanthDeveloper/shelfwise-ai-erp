package com.shelfwise.service;

import com.shelfwise.dto.RecommendDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Orchestrates the three-model AI pipeline used to decide how much to discount
 * a batch of stock:
 *   1. Expiry-risk classifier  (probability this stock goes to waste)
 *   2. Demand forecaster       (expected units sold tomorrow)
 *   3. Discount-policy DQN     (learned optimal discount 0-50%)
 * <p>
 * The models themselves live in the Python ML microservice; all of the
 * business math (suggested price, expected revenue/profit, days of cover)
 * is computed here in Java.
 */
@Service
@RequiredArgsConstructor
public class AiRecommendationService {

    private final MlClientService mlClientService;

    public RecommendDto.Response recommend(RecommendDto.Request req) {
        int categoryId = ProductService.categoryIdOf(req.getCategory());

        // 1. Expiry risk
        Map<String, Object> riskResp = mlClientService.expiryRisk(
                req.getDaysLeft(), req.getStock(), req.getDemandRate(), req.getPrice(), req.getCost());
        double risk = ((Number) riskResp.getOrDefault("risk", 0.3)).doubleValue();
        String riskModel = String.valueOf(riskResp.getOrDefault("model", "XGBoost"));

        // 2. Demand forecast
        Map<String, Object> demandResp = mlClientService.demandForecast(
                req.getLag1(), req.getLag2(), req.getDaysLeft(), req.getPrice());
        int forecast = Math.max(1, ((Number) demandResp.getOrDefault("forecast", req.getDemandRate())).intValue());
        String demandModel = String.valueOf(demandResp.getOrDefault("model", "Ridge Regression"));

        // 3. Discount policy (DQN)
        Map<String, Object> discountResp = mlClientService.discount(
                req.getDaysLeft(), req.getStock(), forecast, req.getPriceChangeCount(), categoryId, req.getShelfLife());
        int discount = ((Number) discountResp.getOrDefault("discount", 0)).intValue();
        String discountModel = String.valueOf(discountResp.getOrDefault("model", "Deep Q-Network"));

        // Business math
        double suggestedPrice = round2(req.getPrice() * (1 - discount / 100.0));
        double daysCover = round1(req.getStock() / (double) Math.max(forecast, 1));
        int expectedUnits = Math.min(req.getStock(), (int) (forecast * (1 + discount / 30.0)));
        double expectedRevenue = round2(expectedUnits * suggestedPrice);
        double expectedProfit = round2(expectedRevenue - expectedUnits * req.getCost());

        return RecommendDto.Response.builder()
                .expiryRisk(round3(risk))
                .riskLabel(risk > 0.6 ? "high" : risk > 0.3 ? "medium" : "low")
                .demandForecast(forecast)
                .daysCover(daysCover)
                .recommendedDiscount(discount)
                .suggestedPrice(suggestedPrice)
                .expectedRevenue(expectedRevenue)
                .expectedProfit(expectedProfit)
                .modelsUsed(RecommendDto.ModelsUsed.builder()
                        .expiry(riskModel)
                        .demand(demandModel)
                        .discount(discountModel)
                        .build())
                .build();
    }

    private double round1(double v) { return Math.round(v * 10.0) / 10.0; }
    private double round2(double v) { return Math.round(v * 100.0) / 100.0; }
    private double round3(double v) { return Math.round(v * 1000.0) / 1000.0; }
}
