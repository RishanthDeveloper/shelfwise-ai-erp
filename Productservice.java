package com.shelfwise.service;

import com.shelfwise.dto.ProductDto;
import com.shelfwise.entity.Product;
import com.shelfwise.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    /** Category ordering must match the order the Python models were trained with. */
    public static final List<String> CATEGORIES = List.of(
            "Dairy", "Bakery", "Fresh Produce", "Beverages", "Canned Goods", "Snacks", "Frozen");

    private final ProductRepository productRepository;

    public List<Product> findAll() {
        return productRepository.findAll();
    }

    public Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product " + id + " not found"));
    }

    @Transactional
    public Product create(ProductDto.Request req) {
        Product product = Product.builder()
                .name(req.getName())
                .category(req.getCategory())
                .basePrice(req.getBasePrice())
                .cost(req.getCost())
                .shelfLifeDays(req.getShelfLifeDays())
                .uom(req.getUom())
                .barcode(req.getBarcode())
                .build();
        return productRepository.save(product);
    }

    @Transactional
    public Product update(Long id, ProductDto.Request req) {
        Product product = findById(id);
        product.setName(req.getName());
        product.setCategory(req.getCategory());
        product.setBasePrice(req.getBasePrice());
        product.setCost(req.getCost());
        product.setShelfLifeDays(req.getShelfLifeDays());
        product.setUom(req.getUom());
        product.setBarcode(req.getBarcode());
        return productRepository.save(product);
    }

    @Transactional
    public void delete(Long id) {
        productRepository.deleteById(id);
    }

    public static int categoryIdOf(String category) {
        int idx = CATEGORIES.indexOf(category);
        return idx < 0 ? 0 : idx;
    }
}
