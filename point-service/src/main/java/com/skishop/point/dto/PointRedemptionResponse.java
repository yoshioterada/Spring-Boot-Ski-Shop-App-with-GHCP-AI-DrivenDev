package com.skishop.point.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * ポイント使用レスポンス DTO
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PointRedemptionResponse {
    
    private UUID redemptionId;
    private Integer pointsUsed;
    private Integer valueRedeemed;
    private Integer balanceAfter;
    private String redemptionType;
    private LocalDateTime redeemedAt;
    private Object details;
    
    /**
     * 成功レスポンスを作成
     */
    public static PointRedemptionResponse success(UUID redemptionId, Integer pointsUsed, 
                                                  Integer valueRedeemed, Integer balanceAfter, 
                                                  String redemptionType, Object details) {
        return PointRedemptionResponse.builder()
                .redemptionId(redemptionId)
                .pointsUsed(pointsUsed)
                .valueRedeemed(valueRedeemed)
                .balanceAfter(balanceAfter)
                .redemptionType(redemptionType)
                .redeemedAt(LocalDateTime.now())
                .details(details)
                .build();
    }
}
