package com.shelfwise.controller;

import com.shelfwise.model.Alert;
import com.shelfwise.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Alerts", description = "ERP alert management")
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    @Operation(summary = "Get all unresolved alerts")
    public ResponseEntity<List<Alert>> getActiveAlerts() {
        return ResponseEntity.ok(alertService.getActiveAlerts());
    }

    @GetMapping("/expiry")
    @Operation(summary = "Get expiry-specific alerts")
    public ResponseEntity<List<Alert>> getExpiryAlerts() {
        return ResponseEntity.ok(alertService.getExpiryAlerts());
    }

    @PostMapping("/refresh")
    @Operation(summary = "Force-refresh alert generation from current inventory")
    public ResponseEntity<List<Alert>> refreshAlerts() {
        alertService.generateExpiryAlerts();
        alertService.generateStockAlerts();
        return ResponseEntity.ok(alertService.getActiveAlerts());
    }

    @PatchMapping("/{id}/resolve")
    @Operation(summary = "Mark an alert as resolved")
    public ResponseEntity<Alert> resolveAlert(@PathVariable Long id) {
        return ResponseEntity.ok(alertService.resolveAlert(id));
    }
}
