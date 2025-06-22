package com.skishop.user.service;

import com.skishop.user.dto.event.UserRegistrationPayload;
import com.skishop.user.dto.event.UserDeletionPayload;
import com.skishop.user.entity.User;
import com.skishop.user.entity.UserActivity;
import com.skishop.user.repository.UserRepository;
import com.skishop.user.repository.UserActivityRepository;
import com.skishop.user.repository.UserPreferenceRepository;
import com.skishop.user.service.metrics.MetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * ユーザーイベント駆動処理専用サービス
 * 認証サービスからのイベントに基づくユーザープロファイル管理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserEventService {

    private final UserRepository userRepository;
    private final UserActivityRepository userActivityRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final MetricsService metricsService;

    /**
     * ユーザープロファイル作成（イベント駆動）
     * 認証サービスからのユーザー登録イベントに基づいてプロファイルを作成
     */
    @Transactional
    public User createUserProfile(UserRegistrationPayload payload) {
        long startTime = System.currentTimeMillis();
        
        try {
            log.info("ユーザープロファイル作成開始: id={}, email={}", payload.getUserId(), payload.getEmail());

            // 重複チェック
            if (userRepository.existsById(UUID.fromString(payload.getUserId()))) {
                log.warn("ユーザーが既に存在します: id={}", payload.getUserId());
                throw new IllegalArgumentException("User already exists: " + payload.getUserId());
            }

            // ユーザーエンティティ作成
            User user = User.builder()
                    .id(UUID.fromString(payload.getUserId()))
                    .email(payload.getEmail())
                    .firstName(payload.getFirstName())
                    .lastName(payload.getLastName())
                    .phoneNumber(payload.getPhoneNumber())
                    .status(User.UserStatus.valueOf(payload.getStatus()))
                    .emailVerified(false)
                    .phoneVerified(false)
                    .createdAt(payload.getCreatedAt() != null ? 
                        LocalDateTime.ofInstant(payload.getCreatedAt(), java.time.ZoneId.systemDefault()) 
                        : LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            user = userRepository.save(user);

            // アクティビティ記録
            recordUserActivity(user.getId(), "PROFILE_CREATED", "User profile created from registration event");

            // メトリクス記録
            long processingTime = System.currentTimeMillis() - startTime;
            metricsService.recordUserRegistration(true, processingTime);
            
            log.info("ユーザープロファイル作成完了: id={}, processingTime={}ms", user.getId(), processingTime);

            return user;

        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            log.error("ユーザープロファイル作成失敗: id={}, processingTime={}ms, error={}", 
                    payload.getUserId(), processingTime, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * ユーザープロファイル削除（イベント駆動）
     * 認証サービスからのユーザー削除イベントに基づいてプロファイルを削除
     */
    @Transactional
    public void deleteUserProfile(UUID id) {
        long startTime = System.currentTimeMillis();
        
        try {
            log.info("ユーザープロファイル削除開始: id={}", id);

            Optional<User> userOpt = userRepository.findById(id);
            if (userOpt.isEmpty()) {
                // 冪等性のため、存在しない場合も成功とする
                log.info("削除対象ユーザーが見つかりません（冪等性により成功扱い）: id={}", id);
                return;
            }

            User user = userOpt.get();

            // 関連データの削除
            deleteRelatedData(user);

            // ユーザー削除
            userRepository.delete(user);

            // メトリクス記録
            long processingTime = System.currentTimeMillis() - startTime;
            metricsService.recordUserDeletion(true, processingTime);

            log.info("ユーザープロファイル削除完了: id={}, processingTime={}ms", id, processingTime);

        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            log.error("ユーザープロファイル削除失敗: id={}, processingTime={}ms, error={}", 
                    id, processingTime, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * ユーザー削除イベント処理
     */
    @Transactional
    public void handleUserDeletionEvent(UserDeletionPayload payload) {
        log.info("ユーザー削除イベント処理開始: id={}, reason={}", payload.getUserId(), payload.getReason());
        
        UUID userId = UUID.fromString(payload.getUserId());
        long startTime = System.currentTimeMillis();
        
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                // 冪等性のため、存在しない場合も成功とする
                log.info("削除対象ユーザーが見つかりません（冪等性により成功扱い）: id={}", userId);
                return;
            }

            User user = userOpt.get();

            // 関連データの削除
            deleteRelatedData(user);

            // ユーザー削除
            userRepository.delete(user);

            // メトリクス記録
            long processingTime = System.currentTimeMillis() - startTime;
            metricsService.recordUserDeletion(true, processingTime);

            log.info("ユーザー削除イベント処理完了: id={}, processingTime={}ms", payload.getUserId(), processingTime);
            
        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            log.error("ユーザー削除イベント処理失敗: id={}, processingTime={}ms, error={}", 
                    payload.getUserId(), processingTime, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 認証サービスからのユーザー登録イベントを処理
     */
    @Transactional
    public void handleUserRegisteredFromAuth(UUID userId, String email, String firstName, String lastName, String phoneNumber) {
        log.info("Handling user registration event from auth service for user: {}", userId);

        // ユーザーが既に存在するかチェック
        Optional<User> existingUser = userRepository.findById(userId);
        if (existingUser.isPresent()) {
            log.info("User {} already exists in user management service, updating if needed", userId);
            User user = existingUser.get();
            
            // 必要に応じて情報を更新
            boolean updated = false;
            if (!email.equals(user.getEmail())) {
                user.setEmail(email);
                updated = true;
            }
            if (!firstName.equals(user.getFirstName())) {
                user.setFirstName(firstName);
                updated = true;
            }
            if (!lastName.equals(user.getLastName())) {
                user.setLastName(lastName);
                updated = true;
            }
            
            if (updated) {
                user.setUpdatedAt(LocalDateTime.now());
                userRepository.save(user);
                log.info("Updated existing user {} in user management service", userId);
            }
        } else {
            // 新しいユーザーをユーザー管理サービスに作成
            User newUser = User.builder()
                .id(userId)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .phoneNumber(phoneNumber)
                .status(User.UserStatus.ACTIVE) // ユーザー管理サービス側では ACTIVE として管理
                .emailVerified(false) // 認証サービスで管理される
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
            
            userRepository.save(newUser);
            log.info("Created new user {} in user management service", userId);
            
            // 追加のユーザー管理サービス固有の処理
            initializeUserProfile(userId);
        }
    }

    /**
     * 認証サービスからのユーザー削除イベントを処理
     */
    @Transactional
    public void handleUserDeletedFromAuth(UUID userId, String reason) {
        log.info("Handling user deletion event from auth service for user: {} with reason: {}", userId, reason);

        Optional<User> user = userRepository.findById(userId);
        if (user.isPresent()) {
            User existingUser = user.get();
            
            // ユーザーを削除状態に変更
            existingUser.setStatus(User.UserStatus.DELETED);
            existingUser.setUpdatedAt(LocalDateTime.now());
            userRepository.save(existingUser);
            
            log.info("Marked user {} as deleted in user management service", userId);
            
            // 追加のクリーンアップ処理
            cleanupUserData(userId, reason);
        } else {
            log.warn("Received user deletion event for non-existent user: {}", userId);
        }
    }

    /**
     * ユーザープロファイル初期化
     */
    private void initializeUserProfile(UUID userId) {
        try {
            // デフォルトのユーザー設定作成
            createDefaultUserPreferences(userId);
            
            // アクティビティ記録
            recordUserActivity(userId, "PROFILE_INITIALIZED", "User profile initialized from auth service");
            
            log.info("User profile initialized: {}", userId);
        } catch (Exception e) {
            log.error("Failed to initialize user profile: {}, error: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * デフォルトユーザー設定作成
     */
    private void createDefaultUserPreferences(UUID userId) {
        try {
            // UserPreference の作成ロジックをここに実装
            log.debug("Created default user preferences for user: {}", userId);
        } catch (Exception e) {
            log.warn("Failed to create default user preferences: userId={}, error={}", 
                     userId, e.getMessage());
        }
    }

    /**
     * ユーザーデータクリーンアップ
     */
    private void cleanupUserData(UUID userId, String reason) {
        try {
            // 関連データの削除処理
            deleteRelatedData(userRepository.findById(userId).orElse(null));
            
            // アクティビティ記録
            recordUserActivity(userId, "DATA_CLEANUP", "User data cleaned up due to: " + reason);
            
            log.info("User data cleanup completed: {}", userId);
        } catch (Exception e) {
            log.error("Failed to cleanup user data: {}, error: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * 関連データの削除
     */
    private void deleteRelatedData(User user) {
        UUID userId = user.getId();
        
        log.info("関連データ削除開始: userId={}", userId);

        // ユーザープリファレンス削除
        int deletedPreferences = userPreferenceRepository.deleteByUserId(userId);
        log.info("ユーザープリファレンス削除完了: userId={}, count={}", userId, deletedPreferences);

        // ユーザーアクティビティ削除
        int deletedActivities = userActivityRepository.deleteByUserId(userId);
        log.info("ユーザーアクティビティ削除完了: userId={}, count={}", userId, deletedActivities);

        // その他の関連データがあれば追加

        log.info("関連データ削除完了: userId={}", userId);
    }

    /**
     * ユーザーアクティビティ記録
     */
    private void recordUserActivity(UUID userId, String action, String description) {
        try {
            // ユーザーを取得
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found: " + userId));
            
            UserActivity activity = UserActivity.builder()
                    .user(user)
                    .activityType(UserActivity.ActivityType.PROFILE_UPDATE) // デフォルトタイプを使用
                    .description(description)
                    .ipAddress("system")
                    .userAgent("event-driven")
                    .build();

            userActivityRepository.save(activity);
            log.debug("ユーザーアクティビティ記録: userId={}, action={}", userId, action);
        } catch (Exception e) {
            log.error("ユーザーアクティビティ記録失敗: userId={}, action={}, error={}", 
                    userId, action, e.getMessage());
            // アクティビティ記録失敗は主処理に影響しない
        }
    }

    /**
     * ユーザープロファイルの強制削除（補償処理用）
     * Saga補償処理で使用される、ユーザープロファイルの完全削除
     */
    @Transactional
    public void hardDeleteUserProfile(UUID id) {
        long startTime = System.currentTimeMillis();
        
        try {
            log.info("補償処理: ユーザープロファイル強制削除開始: id={}", id);

            Optional<User> userOpt = userRepository.findById(id);
            if (userOpt.isEmpty()) {
                log.info("補償処理: 削除対象ユーザーが見つかりません（冪等性により成功扱い）: id={}", id);
                return;
            }

            User user = userOpt.get();

            // 関連データの削除
            deleteRelatedData(user);

            // ユーザー完全削除
            userRepository.delete(user);

            // メトリクス記録
            long processingTime = System.currentTimeMillis() - startTime;
            metricsService.recordUserDeletion(true, processingTime);
            metricsService.recordCompensationExecuted("USER_REGISTRATION", "HARD_DELETE", processingTime, true);

            log.info("補償処理: ユーザープロファイル強制削除完了: id={}, processingTime={}ms", id, processingTime);

        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            log.error("補償処理: ユーザープロファイル強制削除失敗: id={}, processingTime={}ms, error={}", 
                    id, processingTime, e.getMessage(), e);
            
            metricsService.recordCompensationExecuted("USER_REGISTRATION", "HARD_DELETE", processingTime, false);
            throw e;
        }
    }

    /**
     * ユーザーIDによる存在チェック
     */
    public boolean existsByUserId(String userId) {
        try {
            UUID userUUID = UUID.fromString(userId);
            return userRepository.existsById(userUUID);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * メールアドレスによる存在チェック
     */
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * ユーザーIDによる検索
     */
    public Optional<User> findByUserId(String userId) {
        try {
            UUID userUUID = UUID.fromString(userId);
            return userRepository.findById(userUUID);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
