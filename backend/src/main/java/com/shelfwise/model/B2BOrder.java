package com.shelfwise.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "b2b_orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class B2BOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_reference", unique = true, nullable = false)
    private String orderReference;

    @Column(name = "retailer_name", nullable = false)
    private String retailerName;

    @Column(name = "retailer_size")
    @Enumerated(EnumType.STRING)
    private RetailerSize retailerSize;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "quantity_ordered", nullable = false)
    private Integer quantityOrdered;

    @Column(name = "unit_price", nullable = false)
    private Double unitPrice;

    @Column(name = "discount_applied")
    private Double discountApplied;

    @Column(name = "total_value", nullable = false)
    private Double totalValue;

    @Column(name = "payment_terms")
    private String paymentTerms;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum RetailerSize { SMALL, MEDIUM, LARGE, ENTERPRISE }
    public enum OrderStatus { PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED }
}
