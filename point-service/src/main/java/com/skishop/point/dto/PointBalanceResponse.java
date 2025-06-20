package com.skishop.point.dto;

import java.util.UUID;

public record PointBalanceResponse(
    UUID userId,
    Integer totalEarned,
    Integer totalRedeemed,
    Integer currentBalance,
    Integer expiringPoints,
    String tierLevel,
    String tierName
) {
}
