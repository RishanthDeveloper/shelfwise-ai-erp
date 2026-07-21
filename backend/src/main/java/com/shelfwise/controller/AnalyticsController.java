package com.shelfwise.controller;

import com.shelfwise.dto.AnalyticsDTO;
import com.shelfwise.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Analytics", description = "Revenue, waste, and trend analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping
    @Operation(summary = "Get full analytics report")
    public ResponseEntity<AnalyticsDTO> getAnalytics() {
        return ResponseEntity.ok(analyticsService.getAnalytics());
    }
}
