package com.skishop.frontend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品情報DTO
 */
public record ProductDto(
    String id,
    String sku,
    String name,
    String description,
    BigDecimal price,
    String brand,
    String category,
    List<String> imageUrls,
    boolean inStock,
    int stockQuantity,
    double rating,
    int reviewCount,
    List<SpecificationDto> specifications,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    
    public record SpecificationDto(
        String name,
        String value
    ) {}
}


