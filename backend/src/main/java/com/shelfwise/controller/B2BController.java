package com.shelfwise.controller;

import com.shelfwise.model.B2BOrder;
import com.shelfwise.model.B2BOrder.OrderStatus;
import com.shelfwise.service.B2BService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/b2b")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "B2B Orders", description = "Wholesale B2B order management")
public class B2BController {

    private final B2BService b2bService;

    @GetMapping
    @Operation(summary = "List all wholesale B2B orders")
    public ResponseEntity<List<B2BOrder>> getAllOrders() {
        return ResponseEntity.ok(b2bService.getAllOrders());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a B2B order by ID")
    public ResponseEntity<B2BOrder> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(b2bService.getOrderById(id));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Filter B2B orders by status")
    public ResponseEntity<List<B2BOrder>> getOrdersByStatus(@PathVariable OrderStatus status) {
        return ResponseEntity.ok(b2bService.getOrdersByStatus(status));
    }

    @PostMapping
    @Operation(summary = "Create a new B2B order")
    public ResponseEntity<B2BOrder> createOrder(@Valid @RequestBody B2BOrder order) {
        return ResponseEntity.status(HttpStatus.CREATED).body(b2bService.createOrder(order));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update status of a B2B order")
    public ResponseEntity<B2BOrder> updateStatus(
            @PathVariable Long id,
            @RequestParam OrderStatus status) {
        return ResponseEntity.ok(b2bService.updateOrderStatus(id, status));
    }
}
