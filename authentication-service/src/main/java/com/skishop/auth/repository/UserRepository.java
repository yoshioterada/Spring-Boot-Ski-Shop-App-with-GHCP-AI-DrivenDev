package com.skishop.auth.repository;

import com.skishop.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * User Repository
 * 
 * ユーザーエンティティのデータアクセス層
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * メールアドレスでユーザーを検索
     */
    Optional<User> findByEmail(String email);

    /**
     * メールアドレスの存在確認
     */
    boolean existsByEmail(String email);

    /**
     * アクティブなユーザーをメールアドレスで検索
     */
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.isActive = true")
    Optional<User> findActiveUserByEmail(@Param("email") String email);

    /**
     * ロックされていないユーザーをメールアドレスで検索
     */
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.accountLocked = false")
    Optional<User> findUnlockedUserByEmail(@Param("email") String email);

    /**
     * 指定期間内に作成されたユーザー数を取得
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :startDate")
    long countUsersCreatedSince(@Param("startDate") Instant startDate);

    /**
     * ロックされたユーザー一覧を取得
     */
    @Query("SELECT u FROM User u WHERE u.accountLocked = true")
    List<User> findLockedUsers();

    /**
     * メール未検証のユーザー一覧を取得
     */
    @Query("SELECT u FROM User u WHERE u.emailVerified = false")
    List<User> findUnverifiedUsers();
}
