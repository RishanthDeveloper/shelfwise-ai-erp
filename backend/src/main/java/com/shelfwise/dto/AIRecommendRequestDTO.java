package com.shelfwise.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Incoming payload for POST /api/ai/recommend
 * Maps to the Python ML microservice RecommendRequest schema.
 */
@Data
public class AIRecommendRequestDTO {

    @NotNull
    @DecimalMin("0.0")
    private Double daysLeft;

    @NotNull
    @Min(0)
    private Integer stock;

    @NotNull
    @DecimalMin("0.0")
    private Double demandRate;

    @NotNull
    @DecimalMin("0.0")
    private Double price;

    @NotNull
    @DecimalMin("0.0")
    private Double cost;

    @NotNull
    @Min(0)
    private Integer priceChangeCount;

    @DecimalMin("0.0")
    private Double lag1;

    @DecimalMin("0.0")
    private Double lag2;

    private String category;

    @NotNull
    @Min(1)
    private Integer shelfLife;
}
