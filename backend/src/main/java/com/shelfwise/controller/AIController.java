package com.shelfwise.controller;

import com.shelfwise.dto.AIRecommendRequestDTO;
import com.shelfwise.dto.AIRecommendResponseDTO;
import com.shelfwise.service.AIService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "AI / ML", description = "AI-powered recommendation endpoints (DQN + XGBoost + Isolation Forest)")
public class AIController {

    private final AIService aiService;

    @PostMapping("/recommend")
    @Operation(summary = "Get pricing & discount recommendation from the DQN model")
    public ResponseEntity<AIRecommendResponseDTO> recommend(
            @Valid @RequestBody AIRecommendRequestDTO request) {
        return ResponseEntity.ok(aiService.recommend(request));
    }
}
