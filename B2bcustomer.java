package com.shelfwise.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "b2b_customers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class B2BCustomer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    /** gold | silver | bronze */
    @Column(nullable = false)
    private String tier;
}
