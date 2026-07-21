package com.shelfwise.service;

import com.shelfwise.model.B2BOrder;
import com.shelfwise.model.B2BOrder.OrderStatus;
import com.shelfwise.repository.B2BOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class B2BService {

    private final B2BOrderRepository b2bOrderRepository;

    public List<B2BOrder> getAllOrders() {
        return b2bOrderRepository.findAll();
    }

    public List<B2BOrder> getOrdersByStatus(OrderStatus status) {
        return b2bOrderRepository.findByStatus(status);
    }

    public List<B2BOrder> getOrdersByRetailer(String retailerName) {
        return b2bOrderRepository.findByRetailerName(retailerName);
    }

    public B2BOrder getOrderById(Long id) {
        return b2bOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("B2B Order not found with id: " + id));
    }

    @Transactional
    public B2BOrder createOrder(B2BOrder order) {
        if (order.getTotalValue() == null && order.getUnitPrice() != null && order.getQuantityOrdered() != null) {
            double discount = order.getDiscountApplied() != null ? order.getDiscountApplied() : 0.0;
            order.setTotalValue(order.getUnitPrice() * order.getQuantityOrdered() * (1 - discount));
        }
        return b2bOrderRepository.save(order);
    }

    @Transactional
    public B2BOrder updateOrderStatus(Long id, OrderStatus status) {
        B2BOrder order = getOrderById(id);
        order.setStatus(status);
        return b2bOrderRepository.save(order);
    }
}
