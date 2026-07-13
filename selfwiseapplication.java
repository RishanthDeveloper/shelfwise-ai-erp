package com.shelfwise;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ShelfWise ERP — Core Business API.
 *
 * Owns all business data (products, batches, B2B customers/transactions) and
 * orchestrates calls to the companion Python ML microservice for model
 * inference (expiry-risk classifier, demand forecaster, discount DQN,
 * fraud detector, B2B recommender).
 */
@SpringBootApplication
public class ShelfwiseApplication {
    public static void main(String[] args) {
        SpringApplication.run(ShelfwiseApplication.class, args);
    }
}
