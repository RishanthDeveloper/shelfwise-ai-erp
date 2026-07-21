package com.shelfwise.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Payload for POST /api/fraud/detect
 */
@Data
public class FraudDetectRequestDTO {

    private String customerId;

    @NotNull
    @DecimalMin("0.01")
    private Double amount;

    @NotNull
    @DecimalMin("0.01")
    private Double avgPurchaseValue;

    @NotNull
    @DecimalMin("0.0")
    private Double purchaseFrequency;

    @NotNull
    @Min(0) @Max(23)
    private Integer hourOfDay;

    @NotNull
    private Boolean isWeekend;

    @NotNull
    @Min(1)
    private Integer itemsCount;
}
