package com.shelfwise.controller;

import com.shelfwise.dto.DashboardDto;
import com.shelfwise.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @GetMapping("/expiry")
    public List<DashboardDto.ExpiryAlert> expiry() {
        return alertService.expiryAlerts();
    }
}
