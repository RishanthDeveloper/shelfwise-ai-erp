package com.shelfwise.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "batches")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Batch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "batch_code", nullable = false, unique = true)
    private String batchCode;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Column(name = "initial_qty", nullable = false)
    private Integer initialQty;

    @Column(name = "current_qty", nullable = false)
    private Integer currentQty;

    @Column(name = "cost_price", nullable = false)
    private Double costPrice;

    @Column(name = "price_change_count", nullable = false)
    @Builder.Default
    private Integer priceChangeCount = 0;

    @Transient
    public long daysLeft() {
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), expiryDate);
    }
}
