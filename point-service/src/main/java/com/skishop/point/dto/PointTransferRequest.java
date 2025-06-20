package com.skishop.point.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.UUID;

/**
 * ポイント譲渡リクエスト DTO
 */
@Data
public class PointTransferRequest {
    
    @NotNull(message = "Transfer recipient user ID is required")
    private UUID toUserId;
    
    @NotNull(message = "Transfer amount is required")
    @Positive(message = "Transfer amount must be positive")
    private Integer amount;
    
    private String reason;
}
