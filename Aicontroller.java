package com.shelfwise.controller;

import com.shelfwise.dto.RecommendDto;
import com.shelfwise.service.AiRecommendationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiRecommendationService aiRecommendationService;

    /**
     * Full AI pipeline: XGBoost expiry-risk -> Ridge demand forecast -> DQN discount policy.
     * Model inference happens in the Python ML microservice; this endpoint owns the
     * business math and always returns a usable recommendation (falls back to
     * rule-based logic per model if the ML service is unreachable).
     */
    @PostMapping("/recommend")
    public RecommendDto.Response recommend(@Valid @RequestBody RecommendDto.Request request) {
        return aiRecommendationService.recommend(request);
    }
}
