package com.shelfwise.controller;

import com.shelfwise.dto.DashboardDto;
import com.shelfwise.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/kpis")
    public DashboardDto.Kpis kpis() {
        return dashboardService.kpis();
    }
}
