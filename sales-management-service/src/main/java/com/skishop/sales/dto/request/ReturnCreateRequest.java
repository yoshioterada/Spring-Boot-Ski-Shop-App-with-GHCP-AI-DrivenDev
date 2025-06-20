package com.skishop.sales.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 返品申請リクエストDTO
 */
@Data
public class ReturnCreateRequest {

    @NotNull(message = "注文IDは必須です")
    private String orderId;

    @NotNull(message = "注文明細IDは必須です")
    private String orderItemId;

    @NotBlank(message = "返品理由は必須です")
    @Pattern(regexp = "DEFECTIVE|WRONG_ITEM|SIZE_ISSUE|NOT_AS_DESCRIBED|DAMAGED_SHIPPING|CUSTOMER_CHANGED_MIND|OTHER", 
             message = "有効な返品理由を指定してください")
    private String reason;

    @Size(max = 1000, message = "返品理由の詳細は1000文字以内で入力してください")
    private String reasonDetail;

    @NotNull(message = "返品数量は必須です")
    @Min(value = 1, message = "返品数量は1以上である必要があります")
    private Integer quantity;

    @NotNull(message = "返金額は必須です")
    @DecimalMin(value = "0.00", message = "返金額は0以上である必要があります")
    private BigDecimal refundAmount;
}
