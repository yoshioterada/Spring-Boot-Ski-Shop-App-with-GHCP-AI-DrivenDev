package com.skishop.user.service;

import com.skishop.user.dto.request.*;
import com.skishop.user.dto.response.*;
import com.skishop.user.entity.User;
import com.skishop.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * ユーザーサービス
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserListResponse getUsers(Pageable pageable, String search, String status) {
        log.info("Getting users with search: {}, status: {}", search, status);
        
        return UserListResponse.builder()
                .users(Collections.emptyList())
                .totalCount(0)
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .build();
    }

    public UserResponse getUserById(UUID userId) {
        log.info("Getting user by ID: {}", userId);
        
        return UserResponse.builder()
                .id(userId.toString())
                .username("mock_user")
                .email("mock@example.com")
                .firstName("Mock")
                .lastName("User")
                .status("ACTIVE")
                .emailVerified(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public UserResponse createUser(UserCreateRequest request) {
        log.info("Creating user with username: {}", request.username());
        
        return UserResponse.builder()
                .id(UUID.randomUUID().toString())
                .username(request.username())
                .email(request.email())
                .firstName(request.firstName())
                .lastName(request.lastName())
                .status("PENDING")
                .emailVerified(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public UserResponse updateUser(UUID userId, UserUpdateRequest request) {
        log.info("Updating user: {}", userId);
        
        return UserResponse.builder()
                .id(userId.toString())
                .username("updated_user")
                .email(request.email())
                .firstName(request.firstName())
                .lastName(request.lastName())
                .status("ACTIVE")
                .emailVerified(true)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public void deleteUser(UUID userId) {
        log.info("Deleting user: {}", userId);
        // Mock implementation
    }

    public CheckEmailResponse checkEmailAvailability(String email) {
        log.info("Checking email availability: {}", email);
        
        return new CheckEmailResponse(email, false, "Email is available");
    }

    /**
     * 認証サービスからのユーザー登録イベントを処理
     */
    @Transactional
    public void handleUserRegisteredFromAuth(UUID userId, String username, String email) {
        log.info("Handling user registration event from auth service for user: {}", userId);

        // ユーザーが既に存在するかチェック
        Optional<User> existingUser = userRepository.findById(userId);
        if (existingUser.isPresent()) {
            log.info("User {} already exists in user management service, updating if needed", userId);
            User user = existingUser.get();
            
            // 必要に応じて情報を更新
            boolean updated = false;
            if (!username.equals(user.getUsername())) {
                user.setUsername(username);
                updated = true;
            }
            if (!email.equals(user.getEmail())) {
                user.setEmail(email);
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
                .username(username)
                .email(email)
                .status("ACTIVE") // ユーザー管理サービス側では ACTIVE として管理
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
            existingUser.setStatus("DELETED");
            existingUser.setUpdatedAt(LocalDateTime.now());
            userRepository.save(existingUser);
            
            log.info("Marked user {} as deleted in user management service", userId);
            
            // 追加のクリーンアップ処理
            cleanupUserData(userId, reason);
        } else {
            log.warn("Received user deletion event for non-existent user: {}", userId);
        }
    }

    public void resendVerification(ResendVerificationRequest request) {
        log.info("Resending verification email to: {}", request.email());
        // Mock implementation
    }

    /**
     * ユーザープロファイルの初期化
     */
    private void initializeUserProfile(UUID userId) {
        try {
            // ユーザープロファイル、設定、アクティビティログなどの初期化
            log.info("Initializing user profile for user: {}", userId);
            
            // 例: デフォルトの設定を作成
            // 例: ウェルカムメッセージを送信
            // 例: 初期ポイントの付与（他サービスとの連携）
            
        } catch (Exception e) {
            log.error("Failed to initialize user profile for user {}: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * ユーザーデータのクリーンアップ
     */
    private void cleanupUserData(UUID userId, String reason) {
        try {
            log.info("Cleaning up user data for user: {} with reason: {}", userId, reason);
            
            // ユーザーに関連するデータのクリーンアップ
            // 例: ユーザー設定の削除
            // 例: アクティビティログのアーカイブ
            // 例: 個人情報の匿名化
            
            if (reason.startsWith("HARD_DELETE")) {
                log.info("Performing hard cleanup for user: {}", userId);
                // 物理削除の場合は完全にデータを削除
                userRepository.deleteById(userId);
            }
            
        } catch (Exception e) {
            log.error("Failed to cleanup user data for user {}: {}", userId, e.getMessage(), e);
        }
    }
        log.info("Resending verification email to: {}", request.email());
        // Mock implementation
    }

    public void verifyEmail(String token) {
        log.info("Verifying email with token: {}", token);
        // Mock implementation
    }

    @Transactional
    public UserResponse registerUser(UserCreateRequest request) {
        log.info("Registering user with username: {} and email: {}", request.username(), request.email());
        
        // メールアドレスの重複チェック
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already exists: " + request.email());
        }
        
        // Userエンティティを作成
        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .firstName(request.firstName())
                .lastName(request.lastName())
                .phoneNumber(request.phoneNumber())
                .status(User.UserStatus.PENDING_VERIFICATION)
                .emailVerified(false)
                .phoneVerified(false)
                .build();
        
        // データベースに保存
        User savedUser = userRepository.save(user);
        log.info("User registered successfully with ID: {}", savedUser.getId());
        
        return convertToUserResponse(savedUser);
    }

    public UserResponse getCurrentUser(String username) {
        log.info("Getting current user: {}", username);
        
        return UserResponse.builder()
                .id(UUID.randomUUID().toString())
                .username(username)
                .email("current@example.com")
                .firstName("Current")
                .lastName("User")
                .status("ACTIVE")
                .emailVerified(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public void changePassword(String username, ChangePasswordRequest request) {
        log.info("Changing password for user: {}", username);
        // Mock implementation
    }

    public void resendVerificationEmail(ResendVerificationRequest request) {
        log.info("Resending verification email to: {}", request.email());
        // Mock implementation
    }

    public CheckEmailResponse checkEmailExists(String email) {
        log.info("Checking if email exists: {}", email);
        boolean exists = userRepository.existsByEmail(email);
        String message = exists ? "Email already exists" : "Email does not exist";
        return new CheckEmailResponse(email, exists, message);
    }

    /**
     * UserエンティティをUserResponseに変換
     */
    private UserResponse convertToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId().toString())
                .username(user.getEmail()) // usernameとしてemailを使用
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .birthDate(user.getBirthDate())
                .gender(user.getGender() != null ? user.getGender().name() : null)
                .status(user.getStatus().name())
                .emailVerified(user.isEmailVerified())
                .phoneVerified(user.isPhoneVerified())
                .roles(user.getRole() != null ? Collections.singleton(user.getRole().getName()) : null)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
