package com.skishop.user.service;

import com.skishop.user.entity.User;
import com.skishop.user.repository.UserRepository;
import com.skishop.user.repository.UserActivityRepository;
import com.skishop.user.repository.UserPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * ユーザーデータ管理専用サービス
 * データクリーンアップ、バリデーション、関連データ処理を担当
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDataService {

    private final UserRepository userRepository;
    private final UserActivityRepository userActivityRepository;
    private final UserPreferenceRepository userPreferenceRepository;

    /**
     * ユーザーデータクリーンアップ
     */
    @Transactional
    public void cleanupUserData(UUID userId, CleanupType type, String reason) {
        log.info("ユーザーデータクリーンアップ開始: userId={}, type={}, reason={}", userId, type, reason);
        
        switch (type) {
            case SOFT_DELETE -> performSoftDeleteCleanup(userId, reason);
            case HARD_DELETE -> performHardDeleteCleanup(userId, reason);
            case EVENT_DRIVEN -> performEventDrivenCleanup(userId, reason);
            default -> throw new IllegalArgumentException("Unknown cleanup type: " + type);
        }
        
        log.info("ユーザーデータクリーンアップ完了: userId={}, type={}", userId, type);
    }

    /**
     * ユーザープリファレンス削除
     */
    @Transactional
    public int deleteUserPreferences(UUID userId) {
        log.info("ユーザープリファレンス削除開始: userId={}", userId);
        
        int deletedCount = userPreferenceRepository.deleteByUserId(userId);
        
        log.info("ユーザープリファレンス削除完了: userId={}, deletedCount={}", userId, deletedCount);
        return deletedCount;
    }

    /**
     * ユーザーアクティビティ削除
     */
    @Transactional
    public int deleteUserActivities(UUID userId) {
        log.info("ユーザーアクティビティ削除開始: userId={}", userId);
        
        int deletedCount = userActivityRepository.deleteByUserId(userId);
        
        log.info("ユーザーアクティビティ削除完了: userId={}, deletedCount={}", userId, deletedCount);
        return deletedCount;
    }

    /**
     * ユーザーセッション削除（今後の拡張用）
     */
    @Transactional
    public int deleteUserSessions(UUID userId) {
        log.info("ユーザーセッション削除: userId={}", userId);
        // TODO: セッション管理機能が追加された際に実装
        return 0;
    }

    /**
     * ユーザーデータ検証
     */
    public void validateUserData(User user) {
        log.debug("ユーザーデータ検証開始: userId={}", user.getId());
        
        validateEmail(user.getEmail());
        validatePhoneNumber(user.getPhoneNumber());
        validateUserProfile(user);
        
        log.debug("ユーザーデータ検証完了: userId={}", user.getId());
    }

    /**
     * 論理削除時のクリーンアップ
     */
    private void performSoftDeleteCleanup(UUID userId, String reason) {
        log.info("論理削除クリーンアップ実行: userId={}, reason={}", userId, reason);
        
        // ユーザープリファレンスは保持（復旧時のため）
        // アクティビティは保持（監査のため）
        
        // キャッシュやセッションのクリア
        clearUserCache(userId);
        deleteUserSessions(userId);
    }

    /**
     * 物理削除時のクリーンアップ
     */
    private void performHardDeleteCleanup(UUID userId, String reason) {
        log.info("物理削除クリーンアップ実行: userId={}, reason={}", userId, reason);
        
        // 全ての関連データを削除
        deleteUserPreferences(userId);
        deleteUserActivities(userId);
        deleteUserSessions(userId);
        clearUserCache(userId);
    }

    /**
     * イベント駆動削除時のクリーンアップ
     */
    private void performEventDrivenCleanup(UUID userId, String reason) {
        log.info("イベント駆動削除クリーンアップ実行: userId={}, reason={}", userId, reason);
        
        // 認証サービスからの削除イベント時の処理
        deleteUserPreferences(userId);
        deleteUserActivities(userId);
        deleteUserSessions(userId);
        clearUserCache(userId);
    }

    /**
     * ユーザーキャッシュクリア
     */
    private void clearUserCache(UUID userId) {
        log.debug("ユーザーキャッシュクリア: userId={}", userId);
        // TODO: Redis等のキャッシュがある場合にクリア処理を実装
    }

    /**
     * メール形式検証
     */
    private void validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new IllegalArgumentException("Invalid email format: " + email);
        }
    }

    /**
     * 電話番号形式検証
     */
    private void validatePhoneNumber(String phoneNumber) {
        if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
            // 日本の電話番号形式の簡易チェック
            if (!phoneNumber.matches("^(\\+81|0)[0-9]{1,4}-?[0-9]{1,4}-?[0-9]{3,4}$")) {
                throw new IllegalArgumentException("Invalid phone number format: " + phoneNumber);
            }
        }
    }

    /**
     * ユーザープロファイル検証
     */
    private void validateUserProfile(User user) {
        if (user.getFirstName() == null || user.getFirstName().trim().isEmpty()) {
            throw new IllegalArgumentException("First name is required");
        }
        
        if (user.getLastName() == null || user.getLastName().trim().isEmpty()) {
            throw new IllegalArgumentException("Last name is required");
        }
        
        if (user.getStatus() == null) {
            throw new IllegalArgumentException("User status is required");
        }
    }

    /**
     * クリーンアップタイプ
     */
    public enum CleanupType {
        SOFT_DELETE,    // 論理削除時
        HARD_DELETE,    // 物理削除時
        EVENT_DRIVEN    // イベント駆動削除時
    }
}
