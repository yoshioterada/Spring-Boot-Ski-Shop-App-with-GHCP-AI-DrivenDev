package com.skishop.ai.entity;

import java.util.List;

/**
 * 商品推奨タイプ - Java 21のsealed interfaceを使用
 */
public sealed interface RecommendationType
    permits RecommendationType.Collaborative, RecommendationType.ContentBased, RecommendationType.Hybrid {
    
    record Collaborative(String algorithm) implements RecommendationType {}
    record ContentBased(List<String> features) implements RecommendationType {}
    record Hybrid(String primaryAlgorithm, List<String> fallbackAlgorithms) implements RecommendationType {}
    
    /**
     * 推奨タイプから文字列表現への変換
     */
    default String toStringValue() {
        return switch (this) {
            case Collaborative(var algorithm) -> "COLLABORATIVE_" + algorithm.toUpperCase();
            case ContentBased(var features) -> "CONTENT_BASED_" + String.join("_", features);
            case Hybrid(var primary, var fallback) -> "HYBRID_" + primary + "_WITH_" + String.join("_", fallback);
        };
    }
}
