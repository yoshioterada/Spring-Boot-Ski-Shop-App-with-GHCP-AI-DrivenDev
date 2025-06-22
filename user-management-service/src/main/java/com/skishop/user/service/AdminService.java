package com.skishop.user.service;

import com.skishop.user.dto.request.*;
import com.skishop.user.dto.response.*;
import com.skishop.user.entity.Role;
import com.skishop.user.entity.User;
import com.skishop.user.exception.UserNotFoundException;
import com.skishop.user.mapper.RoleMapper;
import com.skishop.user.mapper.UserMapper;
import com.skishop.user.repository.RoleRepository;
import com.skishop.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 管理者サービス
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserQueryService userQueryService;
    private final UserDataService userDataService;

    /**
     * ユーザー一覧取得（高度なフィルタリング）
     */
    @Transactional(readOnly = true)
    public UserListResponse getUsersWithAdvancedFilters(Pageable pageable, String search, String status, String role) {
        log.info("Getting users with advanced filters: search={}, status={}, role={}", search, status, role);
        
        UserQueryService.UserSearchCriteria criteria = new UserQueryService.UserSearchCriteria();
        criteria.setSearch(search);
        criteria.setStatus(status);
        criteria.setRole(role);
        
        Page<User> userPage = userQueryService.searchUsers(criteria, pageable);
        
        List<UserResponse> users = userPage.getContent().stream()
                .map(userMapper::toResponse)
                .toList();
                
        return UserListResponse.builder()
                .users(users)
                .totalCount((int) userPage.getTotalElements())
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .build();
    }

    /**
     * ユーザー一覧取得（レガシー互換性のため）
     * @deprecated getUsersWithAdvancedFilters を使用してください
     */
    @Deprecated
    @Transactional(readOnly = true)
    public UserListResponse getUsers(Pageable pageable, String search, String status, String role) {
        return getUsersWithAdvancedFilters(pageable, search, status, role);
    }

    /**
     * ユーザーロール更新
     */
    public UserResponse updateUserRole(UUID userId, UserRoleUpdateRequest request) {
        log.info("Updating user role: {} -> {}", userId, request.roleIds());
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
        
        // ロール更新処理は本来ここで実装
        // 現在はモック応答を返す
        return userMapper.toResponse(user);
    }

    /**
     * ユーザーロール更新
     */
    public UserResponse updateUserRoles(UUID userId, UserRoleUpdateRequest request) {
        log.info("Updating user roles: userId={}, roleIds={}", userId, request.roleIds());
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
        
        // ロール更新処理は本来ここで実装
        // 現在はMapperを使用してレスポンスを生成
        return userMapper.toResponse(user);
    }

    /**
     * ユーザーステータス更新
     */
    @Transactional
    public UserResponse updateUserStatus(UUID userId, UserStatusUpdateRequest request) {
        log.info("Updating user status: {} -> active: {}", userId, request.active());
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
        
        // ステータス更新
        user.setStatus(request.active() ? User.UserStatus.ACTIVE : User.UserStatus.INACTIVE);
        user.setUpdatedAt(LocalDateTime.now());
        
        user = userRepository.save(user);
        
        return userMapper.toResponse(user);
    }

    /**
     * ロール一覧取得
     */
    @Transactional(readOnly = true)
    public RoleListResponse getRoles(Pageable pageable) {
        log.info("Getting roles with pagination: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        
        Page<Role> rolePage = roleRepository.findAll(pageable);
        
        List<RoleResponse> roleResponses = rolePage.getContent().stream()
                .map(roleMapper::toResponse)
                .toList();
        
        return RoleListResponse.builder()
                .roles(roleResponses)
                .totalCount((int) rolePage.getTotalElements())
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .build();
    }

    /**
     * ロール作成
     */
    @Transactional
    public RoleResponse createRole(RoleCreateRequest request) {
        log.info("Creating role: {}", request.name());
        
        // 既存ロールの重複チェック
        if (roleRepository.findByName(request.name()).isPresent()) {
            throw new IllegalArgumentException("Role with name '" + request.name() + "' already exists");
        }
        
        Role role = roleMapper.toEntity(request);
        role = roleRepository.save(role);
        
        return roleMapper.toResponse(role);
    }

    /**
     * ロール更新
     */
    @Transactional
    public RoleResponse updateRole(String roleId, RoleUpdateRequest request) {
        log.info("Updating role: {} -> {}", roleId, request.name());
        
        Role role = roleRepository.findById(UUID.fromString(roleId))
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleId));
        
        // 名前変更時の重複チェック
        if (!role.getName().equals(request.name()) && 
            roleRepository.findByName(request.name()).isPresent()) {
            throw new IllegalArgumentException("Role with name '" + request.name() + "' already exists");
        }
        
        roleMapper.updateEntity(role, request);
        role = roleRepository.save(role);
        
        return roleMapper.toResponse(role);
    }

    /**
     * ロール削除
     */
    @Transactional
    public void deleteRole(String roleId) {
        log.info("Deleting role: {}", roleId);
        
        Role role = roleRepository.findById(UUID.fromString(roleId))
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleId));
        
        // このロールを使用しているユーザーがいないかチェック
        if (role.getUsers() != null && !role.getUsers().isEmpty()) {
            throw new IllegalStateException("Cannot delete role that is assigned to users");
        }
        
        roleRepository.delete(role);
    }

    /**
     * ユーザーアクティベート
     */
    @Transactional
    public UserResponse activateUser(UUID userId) {
        log.info("Activating user: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
        
        user.setStatus(User.UserStatus.ACTIVE);
        user.setUpdatedAt(LocalDateTime.now());
        
        user = userRepository.save(user);
        
        return userMapper.toResponse(user);
    }

    /**
     * ユーザーデアクティベート
     */
    @Transactional
    public UserResponse deactivateUser(UUID userId) {
        log.info("Deactivating user: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
        
        user.setStatus(User.UserStatus.INACTIVE);
        user.setUpdatedAt(LocalDateTime.now());
        
        user = userRepository.save(user);
        
        return userMapper.toResponse(user);
    }

    /**
     * ユーザー物理削除（管理者専用）
     */
    @Transactional
    public void hardDeleteUser(UUID userId) {
        log.info("Hard deleting user: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
        
        // 関連データのクリーンアップ（UserDataServiceに委譲）
        userDataService.cleanupUserData(userId, UserDataService.CleanupType.HARD_DELETE, "ADMIN_REQUESTED");
        
        // ユーザー物理削除
        userRepository.delete(user);
        
        log.info("Successfully hard deleted user and related data: {}", userId);
    }

    /**
     * ユーザー削除（レガシー互換性のため）
     * @deprecated hardDeleteUser を使用してください
     */
    @Deprecated(since = "1.0", forRemoval = true)
    @Transactional
    public void deleteUser(UUID userId) {
        hardDeleteUser(userId);
    }

    /**
     * システム統計取得
     */
    @Transactional(readOnly = true)
    public SystemStatsResponse getSystemStats() {
        log.info("Getting system stats");
        
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByStatus(User.UserStatus.ACTIVE);
        long inactiveUsers = userRepository.countByStatus(User.UserStatus.INACTIVE);
        long pendingUsers = userRepository.countByStatus(User.UserStatus.PENDING_VERIFICATION);
        long totalRoles = roleRepository.count();
        
        // 最近1週間の登録数を取得
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusWeeks(1);
        long recentRegistrations = userRepository.countByCreatedAtAfter(oneWeekAgo);
        
        return SystemStatsResponse.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .pendingUsers(pendingUsers)
                .inactiveUsers(inactiveUsers)
                .totalRoles(totalRoles)
                .recentRegistrations(recentRegistrations)
                .build();
    }
}
