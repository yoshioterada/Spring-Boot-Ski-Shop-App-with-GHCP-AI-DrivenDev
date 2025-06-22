package com.skishop.user.dto.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.skishop.user.enums.ProcessingStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * ユーザー管理ステータスフィードバックペイロード
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserManagementStatusPayload {
    
    @JsonProperty("userId")
    private String userId;
    
    @JsonProperty("originalEventId")
    private String originalEventId;
    
    @JsonProperty("status")
    private ProcessingStatus status; // SUCCESS, FAILED
    
    @JsonProperty("reason")
    private String reason;
    
    @JsonProperty("processingTime")
    private Long processingTime; // ミリ秒
}
