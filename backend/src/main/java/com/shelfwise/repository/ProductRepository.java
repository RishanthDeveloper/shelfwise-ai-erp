package com.shelfwise.repository;

import com.shelfwise.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);

    List<Product> findByCategory(String category);

    /** Products expiring within the next N days */
    @Query("SELECT p FROM Product p WHERE p.expiryDate BETWEEN :today AND :cutoff ORDER BY p.expiryDate ASC")
    List<Product> findExpiringBefore(@Param("today") LocalDate today,
                                     @Param("cutoff") LocalDate cutoff);

    /** Products with stock below their reorder point */
    @Query("SELECT p FROM Product p WHERE p.stockQty < p.reorderPoint ORDER BY p.stockQty ASC")
    List<Product> findLowStockProducts();

    /** Products where stock is very high relative to demand */
    @Query("SELECT p FROM Product p WHERE p.stockQty > (p.demandRate * :days) ORDER BY p.stockQty DESC")
    List<Product> findOverstockProducts(@Param("days") int coverDays);

    /** Revenue by category */
    @Query("SELECT p.category, SUM(p.price * p.stockQty) FROM Product p GROUP BY p.category")
    List<Object[]> getRevenueByCategory();

    /** Total inventory value */
    @Query("SELECT SUM(p.cost * p.stockQty) FROM Product p")
    Double getTotalInventoryValue();

    /** Count products at risk of expiry (within 7 days) */
    @Query("SELECT COUNT(p) FROM Product p WHERE p.expiryDate BETWEEN :today AND :cutoff")
    Long countExpiringProducts(@Param("today") LocalDate today,
                               @Param("cutoff") LocalDate cutoff);

    List<Product> findBySupplierName(String supplierName);
}
