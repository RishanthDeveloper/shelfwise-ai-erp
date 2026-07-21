package com.shelfwise.repository;

import com.shelfwise.model.B2BOrder;
import com.shelfwise.model.B2BOrder.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface B2BOrderRepository extends JpaRepository<B2BOrder, Long> {

    List<B2BOrder> findByStatus(OrderStatus status);

    List<B2BOrder> findByRetailerName(String retailerName);

    @Query("SELECT SUM(o.totalValue) FROM B2BOrder o WHERE o.status != 'CANCELLED'")
    Double getTotalB2BRevenue();

    @Query("SELECT o.retailerName, SUM(o.totalValue) FROM B2BOrder o GROUP BY o.retailerName ORDER BY SUM(o.totalValue) DESC")
    List<Object[]> getRevenueByRetailer();

    @Query("SELECT COUNT(o) FROM B2BOrder o WHERE o.status = 'PENDING'")
    Long countPendingOrders();
}
