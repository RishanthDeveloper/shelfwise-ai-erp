package com.shelfwise.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String sku;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private Double price;

    @Column(nullable = false)
    private Double cost;

    @Column(name = "stock_qty", nullable = false)
    private Integer stockQty;

    @Column(name = "reorder_point", nullable = false)
    private Integer reorderPoint;

    @Column(name = "shelf_life_days", nullable = false)
    private Integer shelfLifeDays;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Column(name = "demand_rate", nullable = false)
    private Double demandRate;

    @Column(name = "price_change_count")
    @Builder.Default
    private Integer priceChangeCount = 0;

    @Column(name = "lag1_sales")
    private Double lag1Sales;

    @Column(name = "lag2_sales")
    private Double lag2Sales;

    @Column(name = "supplier_id")
    private Long supplierId;

    @Column(name = "supplier_name")
    private String supplierName;

    @Column(name = "lead_time_days")
    @Builder.Default
    private Integer leadTimeDays = 3;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /** Computed: days until expiry from today */
    @Transient
    public long getDaysLeft() {
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), expiryDate);
    }
}
