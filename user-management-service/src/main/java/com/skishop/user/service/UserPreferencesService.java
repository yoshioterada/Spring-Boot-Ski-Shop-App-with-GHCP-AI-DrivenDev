package com.skishop.user.service;

import com.skishop.user.dto.request.UserPreferenceUpdateRequest;
import com.skishop.user.dto.response.UserPreferenceResponse;
import com.skishop.user.dto.response.UserPreferencesListResponse;
import com.skishop.user.entity.User;
import com.skishop.user.entity.UserPreference;
import com.skishop.user.exception.UserNotFoundException;
import com.skishop.user.mapper.UserPreferenceMapper;
import com.skishop.user.repository.UserPreferenceRepository;
import com.skishop.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private final UserRepository userRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final UserPreferenceMapper userPreferenceMapper;

    /**
     * ユーザー設定一覧取得
     */
    @Transactional(readOnly = true)
    public UserPreferencesListResponse getUserPreferences(String userId, Pageable pageable) {
        log.info("Getting user preferences for user: {}", userId);
        
        UUID userUuid = UUID.fromString(userId);
        List<UserPreference> preferences = userPreferenceRepository.findByUser_Id(userUuid);
        
        List<UserPreferenceResponse> preferenceResponses = preferences.stream()
                .map(userPreferenceMapper::toResponse)
                .toList();
        
        // Pageableに対応した分割処理
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), preferenceResponses.size());
        List<UserPreferenceResponse> pagedResults = start >= preferenceResponses.size() ? 
            List.of() : preferenceResponses.subList(start, end);
        
        return UserPreferencesListResponse.builder()
                .preferences(pagedResults)
                .totalCount(preferences.size())
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .build();
    }

    /**
     * ユーザー設定取得
     */
    @Transactional(readOnly = true)
    public UserPreferenceResponse getUserPreference(String userId, String key) {
        log.info("Getting user preference: userId={}, key={}", userId, key);
        
        UUID userUuid = UUID.fromString(userId);
        UserPreference preference = userPreferenceRepository.findByUser_IdAndPrefKey(userUuid, key)
                .orElseThrow(() -> new IllegalArgumentException("User preference not found: userId=" + userId + ", key=" + key));
        
        return userPreferenceMapper.toResponse(preference);
    }

    /**
     * ユーザー設定更新
     */
    @Transactional
    public UserPreferenceResponse updateUserPreference(String userId, String key, UserPreferenceUpdateRequest request) {
        log.info("Updating user preference: userId={}, key={}", userId, key);
        
        UUID userUuid = UUID.fromString(userId);
        
        // ユーザーの存在確認
        User user = userRepository.findById(userUuid)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
        
        // 既存の設定を取得または新規作成
        UserPreference preference = userPreferenceRepository.findByUser_IdAndPrefKey(userUuid, key)
                .orElse(UserPreference.builder()
                        .user(user)
                        .prefKey(key)
                        .prefType(UserPreference.PreferenceType.STRING) // デフォルトタイプ
                        .build());
        
        userPreferenceMapper.updateEntity(preference, request);
        preference.setUpdatedAt(LocalDateTime.now());
        
        preference = userPreferenceRepository.save(preference);
        
        return userPreferenceMapper.toResponse(preference);
    }

    /**
     * ユーザー設定削除
     */
    @Transactional
    public void deleteUserPreference(UUID userId, String key) {
        log.info("Deleting user preference: userId={}, key={}", userId, key);
        
        // 設定の存在確認
        if (!userPreferenceRepository.existsByUser_IdAndPrefKey(userId, key)) {
            throw new IllegalArgumentException("User preference not found: userId=" + userId + ", key=" + key);
        }
        
        userPreferenceRepository.deleteByUser_IdAndPrefKey(userId, key);
        
        log.info("Successfully deleted user preference: userId={}, key={}", userId, key);
    }
}
