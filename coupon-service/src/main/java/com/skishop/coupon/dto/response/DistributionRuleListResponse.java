package com.skishop.coupon.dto.response;

import java.util.List;

/**
 * 配布ルール一覧応答
 */
public record DistributionRuleListResponse(
    String campaignId,
    String campaignName,
    List<DistributionRuleResponse> rules,
    Integer totalCount
) {}
