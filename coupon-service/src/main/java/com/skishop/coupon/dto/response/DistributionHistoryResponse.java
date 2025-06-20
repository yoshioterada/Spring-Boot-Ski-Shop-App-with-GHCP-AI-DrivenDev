package com.skishop.coupon.dto.response;

import org.springframework.data.domain.Page;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 配布履歴応答
 */
public record DistributionHistoryResponse(
    String campaignId,
    String campaignName,
    List<DistributionRecord> distributionHistory,
    PageInfo pageInfo
) {
    
    /**
     * 配布履歴記録
     */
    public record DistributionRecord(
        String distributionId,
        String ruleId,
        String ruleName,
        String userId,
        String couponCode,
        String distributionType,
        String status,
        LocalDateTime distributedAt,
        LocalDateTime usedAt,
        String errorMessage
    ) {}
    
    /**
     * ページ情報
     */
    public record PageInfo(
        Integer page,
        Integer size,
        Integer totalPages,
        Long totalElements,
        Boolean hasNext,
        Boolean hasPrevious
    ) {
        public static PageInfo from(Page<?> page) {
            return new PageInfo(
                page.getNumber(),
                page.getSize(),
                page.getTotalPages(),
                page.getTotalElements(),
                page.hasNext(),
                page.hasPrevious()
            );
        }
    }
}
