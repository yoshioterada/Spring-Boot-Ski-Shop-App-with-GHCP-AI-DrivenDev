package com.skishop.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * システム統計レスポンス
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemStatsResponse {
    
    /**
     * 総ユーザー数
     */
    private Long totalUsers;
    
    /**
     * アクティブユーザー数
     */
    private Long activeUsers;
    
    /**
     * 保留中ユーザー数
     */
    private Long pendingUsers;
    
    /**
     * 非アクティブユーザー数
     */
    private Long inactiveUsers;
    
    /**
     * 総ロール数
     */
    private Long totalRoles;
    
    /**
     * 最近の登録数
     */
    private Long recentRegistrations;
}
