package com.skishop.sales.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 配送作成リクエストDTO
 */
@Data
public class ShipmentCreateRequest {

    @NotNull(message = "注文IDは必須です")
    private String orderId;

    @NotBlank(message = "配送業者は必須です")
    private String carrier;

    private String trackingNumber;

    @Valid
    @NotNull(message = "配送先住所は必須です")
    private ShippingAddressRequest shippingAddress;

    private LocalDateTime estimatedDeliveryAt;

    private String notes;

    /**
     * 配送先住所リクエスト
     */
    @Data
    public static class ShippingAddressRequest {

        @NotBlank(message = "郵便番号は必須です")
        @Pattern(regexp = "\\d{3}-\\d{4}", message = "郵便番号は000-0000の形式である必要があります")
        private String postalCode;

        @NotBlank(message = "都道府県は必須です")
        private String prefecture;

        @NotBlank(message = "市区町村は必須です")
        private String city;

        @NotBlank(message = "住所1は必須です")
        private String addressLine1;

        private String addressLine2;

        @NotBlank(message = "受取人名は必須です")
        private String recipientName;

        @Pattern(regexp = "\\d{2,4}-\\d{2,4}-\\d{4}", message = "電話番号の形式が正しくありません")
        private String phoneNumber;
    }
}


