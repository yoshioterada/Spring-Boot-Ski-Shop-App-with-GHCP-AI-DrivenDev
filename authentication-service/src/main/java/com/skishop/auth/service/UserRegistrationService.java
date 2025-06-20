package com.skishop.auth.service;

import com.skishop.auth.dto.request.UserCreateRequest;
import com.skishop.auth.dto.response.UserResponse;
import com.skishop.auth.entity.User;
import com.skishop.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * ユーザー登録サービス
 * ユーザーの登録と削除を管理し、イベント発行を行う
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserRegistrationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EventPublishingService eventPublishingService;

    /**
     * ユーザー登録
     */
    @Transactional
    public UserResponse registerUser(UserCreateRequest request) {
        log.info("Registering new user with username: {}", request.getUsername());

        // メールアドレスの重複チェック
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + request.getEmail());
        }

        // ユーザー名の重複チェック
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists: " + request.getUsername());
        }

        // ユーザーエンティティ作成
        User user = User.builder()
            .id(UUID.randomUUID())
            .username(request.getUsername())
            .email(request.getEmail())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .status("PENDING")
            .emailVerified(false)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        // データベースに保存
        User savedUser = userRepository.save(user);
        log.info("User saved with ID: {}", savedUser.getId());

        try {
            // ユーザー登録イベントを発行
            eventPublishingService.publishUserRegisteredEvent(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail()
            );
            log.info("User registration event published for user: {}", savedUser.getId());
        } catch (Exception e) {
            log.error("Failed to publish user registration event for user {}: {}", savedUser.getId(), e.getMessage());
            // イベント発行に失敗してもユーザー登録は成功とする（補償トランザクションで対応）
        }

        // レスポンス作成
        return UserResponse.builder()
            .id(savedUser.getId().toString())
            .username(savedUser.getUsername())
            .email(savedUser.getEmail())
            .firstName(savedUser.getFirstName())
            .lastName(savedUser.getLastName())
            .status(savedUser.getStatus())
            .emailVerified(savedUser.isEmailVerified())
            .createdAt(savedUser.getCreatedAt())
            .updatedAt(savedUser.getUpdatedAt())
            .build();
    }

    /**
     * ユーザー削除
     */
    @Transactional
    public void deleteUser(UUID userId, String reason) {
        log.info("Deleting user: {} with reason: {}", userId, reason);

        // ユーザー存在確認
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        try {
            // ユーザー削除イベントを先に発行
            eventPublishingService.publishUserDeletedEvent(userId, reason);
            log.info("User deletion event published for user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to publish user deletion event for user {}: {}", userId, e.getMessage());
            // イベント発行に失敗した場合は削除処理を中止
            throw new RuntimeException("Failed to publish user deletion event", e);
        }

        // ユーザーを論理削除（物理削除ではなく状態変更）
        user.setStatus("DELETED");
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        
        log.info("User {} marked as deleted", userId);
    }

    /**
     * ユーザー物理削除（管理者専用）
     */
    @Transactional
    public void hardDeleteUser(UUID userId, String reason) {
        log.info("Hard deleting user: {} with reason: {}", userId, reason);

        // ユーザー存在確認
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        try {
            // ユーザー削除イベントを先に発行
            eventPublishingService.publishUserDeletedEvent(userId, "HARD_DELETE: " + reason);
            log.info("User hard deletion event published for user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to publish user hard deletion event for user {}: {}", userId, e.getMessage());
            // イベント発行に失敗した場合は削除処理を中止
            throw new RuntimeException("Failed to publish user deletion event", e);
        }

        // ユーザーを物理削除
        userRepository.delete(user);
        log.info("User {} physically deleted", userId);
    }
}
