package com.skishop.inventory.dto.request;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 商品作成リクエストDTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCreateRequest {

    @NotBlank(message = "SKUは必須です")
    @Size(max = 50, message = "SKUは50文字以内で入力してください")
    private String sku;

    @NotBlank(message = "商品名は必須です")
    @Size(max = 200, message = "商品名は200文字以内で入力してください")
    private String name;

    @Size(max = 1000, message = "商品説明は1000文字以内で入力してください")
    private String description;

    @Size(max = 100, message = "ブランド名は100文字以内で入力してください")
    private String brand;

    /**
     * 商品属性
     */
    private Map<String, Object> attributes;

    /**
     * 商品タグ
     */
    private List<String> tags;

    @NotBlank(message = "カテゴリIDは必須です")
    private String categoryId;

    /**
     * 価格情報
     */
    @NotNull(message = "価格情報は必須です")
    private PriceRequest price;

    /**
     * 在庫情報
     */
    @NotNull(message = "在庫情報は必須です")
    private InventoryRequest inventory;

    /**
     * 価格リクエスト
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PriceRequest {
        @NotNull(message = "通常価格は必須です")
        @DecimalMin(value = "0.0", inclusive = false, message = "通常価格は0より大きい値で入力してください")
        private BigDecimal regularPrice;

        @DecimalMin(value = "0.0", inclusive = false, message = "セール価格は0より大きい値で入力してください")
        private BigDecimal salePrice;

        private LocalDateTime saleStartDate;
        private LocalDateTime saleEndDate;

        @Size(max = 3, message = "通貨コードは3文字で入力してください")
        @Builder.Default
        private String currencyCode = "JPY";
    }

    /**
     * 在庫リクエスト
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventoryRequest {
        @NotNull(message = "在庫数量は必須です")
        @Min(value = 0, message = "在庫数量は0以上で入力してください")
        private Integer quantity;

        @NotBlank(message = "ロケーションコードは必須です")
        @Size(max = 20, message = "ロケーションコードは20文字以内で入力してください")
        private String locationCode;
    }
}
