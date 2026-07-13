package com.shelfwise.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class ProductDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        @NotBlank
        private String name;
        @NotBlank
        private String category;
        @Positive
        private Double basePrice;
        @Positive
        private Double cost;
        @Positive
        private Integer shelfLifeDays;
        private String uom;
        private String barcode;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String name;
        private String category;
        private Double basePrice;
        private Double cost;
        private Integer shelfLifeDays;
        private String uom;
        private String barcode;
    }
}
