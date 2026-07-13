package com.shelfwise.repository;

import com.shelfwise.entity.B2BTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface B2BTransactionRepository extends JpaRepository<B2BTransaction, Long> {

    List<B2BTransaction> findTop50ByOrderByTxnDateDesc();

    @Query("select coalesce(sum(t.quantity), 0) from B2BTransaction t")
    long sumAllQuantity();

    @Query("select coalesce(sum(t.price), 0) from B2BTransaction t where t.txnDate >= :from")
    double sumRevenueSince(@Param("from") LocalDate from);

    @Query("""
           select t.product.id as productId, t.product.name as productName,
                  sum(t.quantity) as totalQty
           from B2BTransaction t
           group by t.product.id, t.product.name
           order by totalQty desc
           """)
    List<ProductSalesRow> aggregateSalesByProduct();

    @Query("""
           select t.product.category as category, sum(t.quantity) as totalQty
           from B2BTransaction t
           group by t.product.category
           order by totalQty desc
           """)
    List<CategorySalesRow> aggregateSalesByCategory();

    interface ProductSalesRow {
        Long getProductId();
        String getProductName();
        Long getTotalQty();
    }

    interface CategorySalesRow {
        String getCategory();
        Long getTotalQty();
    }
}
