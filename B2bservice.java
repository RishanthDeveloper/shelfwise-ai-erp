package com.shelfwise.service;

import com.shelfwise.dto.B2BDto;
import com.shelfwise.entity.Product;
import com.shelfwise.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class B2BService {

    private final MlClientService mlClientService;
    private final ProductRepository productRepository;

    public B2BDto.Response recommend(B2BDto.Request req) {
        List<Long> ids = mlClientService.b2bRecommend(req.getCustomerId(), req.getTopN());

        List<B2BDto.RecommendedProduct> recommendations = ids.stream()
                .map(id -> productRepository.findById(id).orElse(null))
                .filter(java.util.Objects::nonNull)
                .map(this::toRecommendedProduct)
                .toList();

        return B2BDto.Response.builder()
                .customerId(req.getCustomerId())
                .recommendations(recommendations)
                .model(recommendations.isEmpty() ? "fallback" : "NMF")
                .build();
    }

    private B2BDto.RecommendedProduct toRecommendedProduct(Product p) {
        return B2BDto.RecommendedProduct.builder()
                .productId(p.getId())
                .productName(p.getName())
                .category(p.getCategory())
                .basePrice(p.getBasePrice())
                .build();
    }
}
