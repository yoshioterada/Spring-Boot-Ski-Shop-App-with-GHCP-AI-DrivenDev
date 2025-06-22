package com.skishop.user.dto.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.Instant;

/**
 * ユーザー削除イベントペイロード
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDeletionPayload {
    
    @JsonProperty("userId")
    private String userId;
    
    @JsonProperty("reason")
    private String reason;
    
    @JsonProperty("deletedAt")
    private Instant deletedAt;
}
