package com.skishop.inventory.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 商品画像DTO（Java 21のrecord使用）
 */
public record ProductImageDTO(
    UUID id,
    
    @NotBlank(message = "商品IDは必須です")
    String productId,
    
    @NotBlank(message = "画像URLは必須です")
    @Size(max = 500, message = "画像URLは500文字以内で入力してください")
    String url,
    
    @Size(max = 500, message = "サムネイル画像URLは500文字以内で入力してください")
    String thumbnailUrl,
    
    @NotNull(message = "画像タイプは必須です")
    String type,
    
    @NotNull(message = "表示順序は必須です")
    @Min(value = 0, message = "表示順序は0以上で入力してください")
    Integer sortOrder,
    
    @Size(max = 200, message = "代替テキストは200文字以内で入力してください")
    String altText,
    
    /**
     * 作成日時
     */
    LocalDateTime createdAt,
    
    /**
     * 更新日時
     */
    LocalDateTime updatedAt
) {
    /**
     * メイン画像かどうかの判定
     */
    public boolean isMainImage() {
        return "MAIN".equalsIgnoreCase(type);
    }
    
    /**
     * サムネイル画像が利用可能かどうかの判定
     */
    public boolean hasThumbnail() {
        return thumbnailUrl != null && !thumbnailUrl.isBlank();
    }
}
