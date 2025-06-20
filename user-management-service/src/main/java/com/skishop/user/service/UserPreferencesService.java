package com.skishop.user.service;

import com.skishop.user.dto.request.UserPreferenceUpdateRequest;
import com.skishop.user.dto.response.UserPreferenceResponse;
import com.skishop.user.dto.response.UserPreferencesListResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * ユーザー設定管理サービス
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserPreferencesService {

    /**
     * ユーザー設定一覧取得
     */
    public UserPreferencesListResponse getUserPreferences(String userId, Pageable pageable) {
        log.info("Getting user preferences for user: {}", userId);
        
        // モック実装
        UserPreferenceResponse mockPreference = UserPreferenceResponse.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .key("theme")
                .value("dark")
                .category("ui")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
                
        return UserPreferencesListResponse.builder()
                .preferences(List.of(mockPreference))
                .totalCount(1)
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .build();
    }

    /**
     * ユーザー設定取得
     */
    public UserPreferenceResponse getUserPreference(String userId, String key) {
        log.info("Getting user preference: userId={}, key={}", userId, key);
        
        // モック実装
        return UserPreferenceResponse.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .key(key)
                .value("default_value")
                .category("general")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * ユーザー設定更新
     */
    public UserPreferenceResponse updateUserPreference(String userId, String key, UserPreferenceUpdateRequest request) {
        log.info("Updating user preference: userId={}, key={}", userId, key);
        
        // モック実装
        return UserPreferenceResponse.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .key(key)
                .value(request.value())
                .category("general")
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * ユーザー設定削除
     */
    public void deleteUserPreference(UUID userId, String key) {
        log.info("Deleting user preference: userId={}, key={}", userId, key);
        // モック実装 - 実際の削除処理をここに実装
    }
}
