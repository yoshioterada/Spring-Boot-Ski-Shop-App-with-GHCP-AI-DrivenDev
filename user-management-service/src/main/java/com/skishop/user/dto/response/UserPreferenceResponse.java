package com.skishop.user.dto.response;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * ユーザー設定レスポンスDTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferenceResponse {

    private String id;
    private String userId;
    private String key;
    private String value;
    private String category;
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;
}
