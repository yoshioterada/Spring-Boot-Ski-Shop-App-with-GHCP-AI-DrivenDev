package com.skishop.frontend.service;

import com.skishop.frontend.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collections;

/**
 * 商品関連のAPIクライアントサービス
 */
@Service
public class ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);
    
    private final WebClient webClient;

    public ProductService(WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * 商品一覧取得
     */
    @Cacheable(value = "products", key = "#page + '-' + #size + '-' + #sortBy")
    public Mono<ProductSearchResponse> getProducts(int page, int size, String sortBy, String category, String brand) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/api/products")
                .queryParam("page", page)
                .queryParam("size", size)
                .queryParam("sort", sortBy)
                .queryParamIfPresent("category", category != null ? java.util.Optional.of(category) : java.util.Optional.empty())
                .queryParamIfPresent("brand", brand != null ? java.util.Optional.of(brand) : java.util.Optional.empty())
                .build())
            .retrieve()
            .bodyToMono(ProductSearchResponse.class)
            .timeout(Duration.ofSeconds(10))
            .onErrorResume(this::handleProductSearchError);
    }

    /**
     * 商品詳細取得
     */
    @Cacheable(value = "product", key = "#productId")
    public Mono<ProductDto> getProduct(String productId) {
        return webClient.get()
            .uri("/api/products/{id}", productId)
            .retrieve()
            .bodyToMono(ProductDto.class)
            .timeout(Duration.ofSeconds(10))
            .onErrorResume(ex -> {
                logger.error("Failed to get product {}: {}", productId, ex.getMessage());
                return Mono.empty();
            });
    }

    /**
     * 商品検索
     */
    public Mono<ProductSearchResponse> searchProducts(String query, int page, int size) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/api/products/search")
                .queryParam("q", query)
                .queryParam("page", page)
                .queryParam("size", size)
                .build())
            .retrieve()
            .bodyToMono(ProductSearchResponse.class)
            .timeout(Duration.ofSeconds(10))
            .onErrorResume(this::handleProductSearchError);
    }

    /**
     * カテゴリ一覧取得
     */
    @Cacheable(value = "categories")
    public Mono<java.util.List<CategoryDto>> getCategories() {
        return webClient.get()
            .uri("/api/categories")
            .retrieve()
            .bodyToFlux(CategoryDto.class)
            .collectList()
            .timeout(Duration.ofSeconds(10))
            .onErrorResume(ex -> {
                logger.error("Failed to get categories: {}", ex.getMessage());
                return Mono.just(Collections.emptyList());
            });
    }

    /**
     * 関連商品取得
     */
    public Mono<java.util.List<ProductDto>> getRelatedProducts(String productId) {
        return webClient.get()
            .uri("/api/v1/recommendations/similar/{productId}", productId)
            .retrieve()
            .bodyToFlux(ProductDto.class)
            .collectList()
            .timeout(Duration.ofSeconds(10))
            .onErrorResume(ex -> {
                logger.error("Failed to get related products for {}: {}", productId, ex.getMessage());
                return Mono.just(Collections.emptyList());
            });
    }

    private Mono<ProductSearchResponse> handleProductSearchError(Throwable ex) {
        logger.error("Product search failed: {}", ex.getMessage());
        
        if (ex instanceof WebClientResponseException webEx) {
            if (webEx.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Mono.just(new ProductSearchResponse(Collections.emptyList(), 0, 0, 20, "name", "asc"));
            }
        }
        
        // デフォルトレスポンスを返す
        return Mono.just(new ProductSearchResponse(Collections.emptyList(), 0, 0, 20, "name", "asc"));
    }
}
