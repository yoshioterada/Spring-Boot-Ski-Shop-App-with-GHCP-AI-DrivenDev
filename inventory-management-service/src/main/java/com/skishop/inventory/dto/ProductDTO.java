package com.skishop.inventory.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 商品レスポンスDTO
 * 
 * <p>Java 21のrecord機能を使用した不変データクラス</p>
 * 
 * @param id 商品ID
 * @param sku SKU
 * @param name 商品名
 * @param description 商品説明
 * @param brand ブランド名
 * @param attributes 商品属性
 * @param tags 商品タグ
 * @param category カテゴリ情報
 * @param price 価格情報
 * @param inventory 在庫情報
 * @param images 商品画像
 * @param imageUrl メイン画像URL
 * @param active アクティブ状態
 * @param createdAt 作成日時
 * @param updatedAt 更新日時
 * 
 * @since 1.0.0
 */
public record ProductDTO(
    String id,
    
    @NotBlank(message = "SKUは必須です")
    @Size(max = 50, message = "SKUは50文字以内で入力してください")
    String sku,
    
    @NotBlank(message = "商品名は必須です")
    @Size(max = 200, message = "商品名は200文字以内で入力してください")
    String name,
    
    @Size(max = 1000, message = "商品説明は1000文字以内で入力してください")
    String description,
    
    @Size(max = 100, message = "ブランド名は100文字以内で入力してください")
    String brand,
    
    Map<String, Object> attributes,
    List<String> tags,
    CategoryDTO category,
    PriceInfoDTO price,
    InventoryInfoDTO inventory,
    List<ProductImageDTO> images,
    String imageUrl,
    Boolean active,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    
    /**
     * アクティブな商品のみをフィルタリング
     * 
     * @return アクティブな場合true
     */
    public boolean isActive() {
        return active != null && active;
    }
    
    /**
     * セール中かどうかを判定
     * 
     * @return セール中の場合true
     */
    public boolean isOnSale() {
        return price != null && price.onSale() != null && price.onSale();
    }
    
    /**
     * 在庫があるかどうかを判定
     * 
     * @return 在庫がある場合true
     */
    public boolean hasStock() {
        return inventory != null && 
               inventory.availableQuantity() != null && 
               inventory.availableQuantity() > 0;
    }
    
    /**
     * 価格情報DTO
     * 
     * @param regularPrice 通常価格
     * @param salePrice セール価格
     * @param currentPrice 現在価格
     * @param currencyCode 通貨コード
     * @param onSale セール中フラグ
     * @param saleStartDate セール開始日
     * @param saleEndDate セール終了日
     */
    public record PriceInfoDTO(
        BigDecimal regularPrice,
        BigDecimal salePrice,
        BigDecimal currentPrice,
        String currencyCode,
        Boolean onSale,
        LocalDateTime saleStartDate,
        LocalDateTime saleEndDate
    ) {
        
        /**
         * 有効なセール期間中かどうかを判定
         * 
         * @return セール期間中の場合true
         */
        public boolean isValidSalePeriod() {
            if (!Boolean.TRUE.equals(onSale)) {
                return false;
            }
            
            var now = LocalDateTime.now();
            var afterStart = saleStartDate == null || !now.isBefore(saleStartDate);
            var beforeEnd = saleEndDate == null || !now.isAfter(saleEndDate);
            
            return afterStart && beforeEnd;
        }
    }
    
    /**
     * 在庫情報DTO
     * 
     * @param status ステータス
     * @param quantity 総在庫数
     * @param availableQuantity 利用可能在庫数
     * @param locationCode 場所コード
     */
    public record InventoryInfoDTO(
        String status,
        Integer quantity,
        Integer availableQuantity,
        String locationCode
    ) {
        
        /**
         * 在庫ステータスを判定
         * 
         * @return 在庫ステータス文字列
         */
        public String getStockStatus() {
            var qty = availableQuantity == null ? 0 : availableQuantity;
            return switch (qty) {
                case 0 -> "在庫切れ";
                case 1, 2, 3, 4, 5 -> "残りわずか";
                default -> qty > 10 ? "在庫あり" : "在庫少";
            };
        }
    }
}
