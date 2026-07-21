package com.shelfwise.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "alerts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(nullable = false)
    private String sku;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AlertType type;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AlertSeverity severity;

    @Column(nullable = false)
    private String message;

    @Column(name = "days_left")
    private Long daysLeft;

    @Column(name = "stock_qty")
    private Integer stockQty;

    @Column(name = "recommended_discount")
    private Double recommendedDiscount;

    @Column(name = "waste_value")
    private Double wasteValue;

    @Column(name = "is_resolved")
    @Builder.Default
    private Boolean isResolved = false;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum AlertType {
        EXPIRY, LOW_STOCK, OVERSTOCK, PRICE_ANOMALY, DEMAND_SPIKE
    }

    public enum AlertSeverity {
        CRITICAL, HIGH, MEDIUM, LOW
    }
}
