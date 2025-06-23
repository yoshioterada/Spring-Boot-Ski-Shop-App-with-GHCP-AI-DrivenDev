package com.skishop.frontend.dto;

import java.util.List;

/**
 * 商品検索レスポンスDTO
 */
public record ProductSearchResponse(
    List<ProductDto> products,
    int totalCount,
    int page,
    int size,
    String sortBy,
    String sortDirection
) {}
