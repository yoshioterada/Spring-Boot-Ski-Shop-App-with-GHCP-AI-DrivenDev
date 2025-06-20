package com.skishop.point.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record PointTransactionDto(
    UUID id,
    UUID userId,
    String transactionType,
    Integer amount,
    Integer balanceAfter,
    String reason,
    String referenceId,
    LocalDateTime expiresAt,
    Boolean isExpired,
    LocalDateTime createdAt
) {
}
