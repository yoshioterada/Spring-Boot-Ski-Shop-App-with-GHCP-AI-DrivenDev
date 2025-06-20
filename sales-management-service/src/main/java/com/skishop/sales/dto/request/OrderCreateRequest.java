package com.skishop.sales.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 注文作成リクエストDTO
 * Java 21のRecordを使用してイミュータブルなデータ構造を定義
 */
public record OrderCreateRequest(
    @NotBlank(message = "顧客IDは必須です")
    String customerId,

    @Valid
    @NotEmpty(message = "注文明細は1つ以上必要です")
    List<OrderItemRequest> items,

    @Valid
    @NotNull(message = "配送先住所は必須です")
    ShippingAddressRequest shippingAddress,

    @NotBlank(message = "支払い方法は必須です")
    String paymentMethod,

    String couponCode,

    @Min(value = 0, message = "使用ポイントは0以上である必要があります")
    Integer usedPoints,

    String notes) {

    /**
     * 注文明細リクエスト
     * Java 21のRecordを使用
     */
    public record OrderItemRequest(
        @NotBlank(message = "商品IDは必須です")
        String productId,

        @NotBlank(message = "商品名は必須です")
        String productName,

        @NotBlank(message = "SKUは必須です")
        String sku,

        @NotNull(message = "単価は必須です")
        @DecimalMin(value = "0.00", inclusive = false, message = "単価は0より大きい値である必要があります")
        BigDecimal unitPrice,

        @NotNull(message = "数量は必須です")
        @Min(value = 1, message = "数量は1以上である必要があります")
        Integer quantity) {
    }

    /**
     * 配送先住所リクエスト
     * Java 21のRecordを使用
     */
    public record ShippingAddressRequest(
        @NotBlank(message = "郵便番号は必須です")
        @Pattern(regexp = "\\d{3}-\\d{4}", message = "郵便番号は000-0000の形式である必要があります")
        String postalCode,

        @NotBlank(message = "都道府県は必須です")
        String prefecture,

        @NotBlank(message = "市区町村は必須です")
        String city,

        @NotBlank(message = "住所1は必須です")
        String addressLine1,

        String addressLine2,

        @NotBlank(message = "受取人名は必須です")
        String recipientName,

        @Pattern(regexp = "\\d{2,4}-\\d{2,4}-\\d{4}", message = "電話番号の形式が正しくありません")
        String phoneNumber) {
    }
}
