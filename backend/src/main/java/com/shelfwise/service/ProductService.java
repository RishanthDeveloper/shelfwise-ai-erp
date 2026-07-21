package com.shelfwise.service;

import com.shelfwise.model.Product;
import com.shelfwise.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepo;

    public List<Product> getAllProducts() {
        return productRepo.findAll();
    }

    public Product getProductById(Long id) {
        return productRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));
    }

    public Product getProductBySku(String sku) {
        return productRepo.findBySku(sku)
                .orElseThrow(() -> new RuntimeException("Product not found with SKU: " + sku));
    }

    @Transactional
    public Product createProduct(Product product) {
        if (productRepo.findBySku(product.getSku()).isPresent()) {
            throw new IllegalArgumentException("SKU already exists: " + product.getSku());
        }
        return productRepo.save(product);
    }

    @Transactional
    public Product updateProduct(Long id, Product updated) {
        Product existing = getProductById(id);
        existing.setName(updated.getName());
        existing.setCategory(updated.getCategory());
        existing.setPrice(updated.getPrice());
        existing.setCost(updated.getCost());
        existing.setStockQty(updated.getStockQty());
        existing.setReorderPoint(updated.getReorderPoint());
        existing.setExpiryDate(updated.getExpiryDate());
        existing.setDemandRate(updated.getDemandRate());
        existing.setSupplierName(updated.getSupplierName());
        return productRepo.save(existing);
    }

    @Transactional
    public void deleteProduct(Long id) {
        productRepo.deleteById(id);
    }

    public List<Product> getExpiringProducts(int days) {
        return productRepo.findExpiringBefore(LocalDate.now(), LocalDate.now().plusDays(days));
    }

    public List<Product> getLowStockProducts() {
        return productRepo.findLowStockProducts();
    }
}
