package com.shelfwise.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String category;

    @Column(name = "base_price", nullable = false)
    private Double basePrice;

    @Column(nullable = false)
    private Double cost;

    @Column(name = "shelf_life_days", nullable = false)
    private Integer shelfLifeDays;

    private String uom;

    private String barcode;
}
