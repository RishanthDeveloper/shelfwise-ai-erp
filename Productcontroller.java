package com.shelfwise.controller;

import com.shelfwise.dto.ProductDto;
import com.shelfwise.entity.Product;
import com.shelfwise.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public List<ProductDto.Response> all() {
        return productService.findAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public ProductDto.Response one(@PathVariable Long id) {
        return toResponse(productService.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDto.Response create(@Valid @RequestBody ProductDto.Request req) {
        return toResponse(productService.create(req));
    }

    @PutMapping("/{id}")
    public ProductDto.Response update(@PathVariable Long id, @Valid @RequestBody ProductDto.Request req) {
        return toResponse(productService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private ProductDto.Response toResponse(Product p) {
        return ProductDto.Response.builder()
                .id(p.getId())
                .name(p.getName())
                .category(p.getCategory())
                .basePrice(p.getBasePrice())
                .cost(p.getCost())
                .shelfLifeDays(p.getShelfLifeDays())
                .uom(p.getUom())
                .barcode(p.getBarcode())
                .build();
    }
}
