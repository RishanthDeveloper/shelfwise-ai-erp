package com.shelfwise.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", unique = true, nullable = false)
    private String transactionId;

    @Column(nullable = false)
    private Double amount;

    @Column(name = "avg_purchase_value")
    private Double avgPurchaseValue;

    @Column(name = "purchase_frequency")
    private Double purchaseFrequency;

    @Column(name = "hour_of_day")
    private Integer hourOfDay;

    @Column(name = "is_weekend")
    private Boolean isWeekend;

    @Column(name = "items_count")
    private Integer itemsCount;

    @Column(name = "customer_id")
    private String customerId;

    @Column(name = "is_fraud")
    private Boolean isFraud;

    @Column(name = "anomaly_score")
    private Double anomalyScore;

    @Column(name = "fraud_probability")
    private Double fraudProbability;

    @Column(name = "flagged_reason")
    private String flaggedReason;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
