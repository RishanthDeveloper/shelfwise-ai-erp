package com.shelfwise.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private final WebClient mlServiceWebClient;

    @GetMapping
    public Map<String, Object> health() {
        String mlStatus;
        try {
            mlServiceWebClient.get().uri("/health").retrieve().toBodilessEntity().block();
            mlStatus = "up";
        } catch (Exception e) {
            mlStatus = "unreachable";
        }
        return Map.of(
                "status", "ok",
                "service", "shelfwise-erp-backend",
                "mlService", mlStatus
        );
    }
}
