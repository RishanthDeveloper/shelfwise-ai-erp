package com.shelfwise.repository;

import com.shelfwise.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByCategoryIgnoreCase(String category);
    Optional<Product> findByBarcode(String barcode);
}
