package com.shelfwise.controller;

import com.shelfwise.dto.AnalyticsDto;
import com.shelfwise.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/pareto")
    public AnalyticsDto.ParetoResponse pareto() {
        return analyticsService.pareto();
    }

    @GetMapping("/waste-by-category")
    public List<AnalyticsDto.WasteByCategory> wasteByCategory() {
        return analyticsService.wasteByCategory();
    }
}
