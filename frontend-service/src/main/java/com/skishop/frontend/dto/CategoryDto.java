package com.skishop.frontend.dto;

import java.util.List;

/**
 * カテゴリDTO
 */
public record CategoryDto(
    String id,
    String name,
    String description,
    String imageUrl,
    List<CategoryDto> subcategories
) {}
