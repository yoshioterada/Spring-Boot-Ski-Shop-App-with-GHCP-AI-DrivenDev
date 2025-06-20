package com.skishop.user.service;

import com.skishop.user.dto.request.*;
import com.skishop.user.dto.response.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 管理者サービス
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    /**
     * ユーザー一覧取得
     */
    public UserListResponse getUsers(Pageable pageable, String search, String status, String role) {
        log.info("Getting users: search={}, status={}, role={}", search, status, role);
        
        // モック実装
        UserResponse mockUser = UserResponse.builder()
                .id(UUID.randomUUID().toString())
                .username("testuser")
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .status("ACTIVE")
                .emailVerified(true)
                .roles(Set.of("USER"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
                
        return UserListResponse.builder()
                .users(List.of(mockUser))
                .totalCount(1)
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .build();
    }

    /**
     * ユーザーロール更新
     */
    public UserResponse updateUserRole(String userId, UserRoleUpdateRequest request) {
        log.info("Updating user role: {} -> {}", userId, request.roleIds());
        
        return UserResponse.builder()
                .id(userId)
                .username("testuser")
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .status("ACTIVE")
                .emailVerified(true)
                .roles(request.roleIds())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * ユーザーロール更新
     */
    public UserResponse updateUserRoles(UUID userId, UserRoleUpdateRequest request) {
        log.info("Updating user roles: userId={}, roleIds={}", userId, request.roleIds());
        
        return UserResponse.builder()
                .id(userId.toString())
                .username("updated_user")
                .email("user@example.com")
                .firstName("Updated")
                .lastName("User")
                .status("ACTIVE")
                .emailVerified(true)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * ユーザーステータス更新
     */
    public UserResponse updateUserStatus(String userId, UserStatusUpdateRequest request) {
        log.info("Updating user status: {} -> active: {}", userId, request.active());
        
        return UserResponse.builder()
                .id(userId)
                .username("testuser")
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .status(request.active() ? "ACTIVE" : "INACTIVE")
                .emailVerified(true)
                .roles(Set.of("USER"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * ロール一覧取得
     */
    public RoleListResponse getRoles(Pageable pageable) {
        log.info("Getting roles");
        
        // モック実装
        RoleResponse mockRole = RoleResponse.builder()
                .id(UUID.randomUUID().toString())
                .name("USER")
                .description("Standard user role")
                .permissions(Set.of("READ", "WRITE"))
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
                
        return RoleListResponse.builder()
                .roles(List.of(mockRole))
                .totalCount(1)
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .build();
    }

    /**
     * ロール作成
     */
    public RoleResponse createRole(RoleCreateRequest request) {
        log.info("Creating role: {}", request.name());
        
        return RoleResponse.builder()
                .id(UUID.randomUUID().toString())
                .name(request.name())
                .description(request.description())
                .permissions(request.permissions())
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * ロール更新
     */
    public RoleResponse updateRole(String roleId, RoleUpdateRequest request) {
        log.info("Updating role: {} -> {}", roleId, request.name());
        
        return RoleResponse.builder()
                .id(roleId)
                .name(request.name())
                .description(request.description())
                .permissions(request.permissions())
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * ロール削除
     */
    public void deleteRole(String roleId) {
        log.info("Deleting role: {}", roleId);
        // モック実装 - 実際にはデータベースから削除
    }
}
