package com.shelfwise.controller;

import com.shelfwise.dto.FraudDetectRequestDTO;
import com.shelfwise.dto.FraudDetectResponseDTO;
import com.shelfwise.model.Transaction;
import com.shelfwise.service.FraudService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fraud")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Fraud Detection", description = "Isolation Forest + XGBoost fraud detection")
public class FraudController {

    private final FraudService fraudService;

    @PostMapping("/detect")
    @Operation(summary = "Run fraud detection on a transaction")
    public ResponseEntity<FraudDetectResponseDTO> detect(
            @Valid @RequestBody FraudDetectRequestDTO request) {
        return ResponseEntity.ok(fraudService.detectFraud(request));
    }

    @GetMapping("/flagged")
    @Operation(summary = "List all flagged (fraudulent) transactions")
    public ResponseEntity<List<Transaction>> getFlagged() {
        return ResponseEntity.ok(fraudService.getFlaggedTransactions());
    }
}
