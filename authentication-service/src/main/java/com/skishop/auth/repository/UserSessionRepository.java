package com.skishop.auth.repository;

import com.skishop.auth.entity.User;
import com.skishop.auth.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * User Session Repository
 * 
 * ユーザーセッションエンティティのデータアクセス層
 */
@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {

    /**
     * セッショントークンでセッションを検索
     */
    Optional<UserSession> findBySessionToken(String sessionToken);

    /**
     * リフレッシュトークンでセッションを検索
     */
    Optional<UserSession> findByRefreshToken(String refreshToken);

    /**
     * ユーザーのアクティブセッション一覧を取得
     */
    @Query("SELECT s FROM UserSession s WHERE s.user = :user AND s.isActive = true AND s.expiresAt > :now")
    List<UserSession> findActiveSessionsByUser(@Param("user") User user, @Param("now") Instant now);

    /**
     * ユーザーの全セッション一覧を取得
     */
    List<UserSession> findByUserOrderByCreatedAtDesc(User user);

    /**
     * ユーザーIDで全セッションを削除
     */
    @Modifying
    @Query("DELETE FROM UserSession s WHERE s.userId = :userId")
    void deleteByUserId(@Param("userId") UUID userId);

    /**
     * 期限切れセッションを無効化
     */
    @Modifying
    @Query("UPDATE UserSession s SET s.isActive = false WHERE s.expiresAt < :now")
    int deactivateExpiredSessions(@Param("now") Instant now);

    /**
     * ユーザーの全セッションを無効化
     */
    @Modifying
    @Query("UPDATE UserSession s SET s.isActive = false WHERE s.user = :user")
    int deactivateAllUserSessions(@Param("user") User user);

    /**
     * 指定セッション以外のユーザーセッションを無効化
     */
    @Modifying
    @Query("UPDATE UserSession s SET s.isActive = false WHERE s.user = :user AND s.id != :sessionId")
    int deactivateOtherUserSessions(@Param("user") User user, @Param("sessionId") UUID sessionId);

    /**
     * 期限切れセッションを削除
     */
    @Modifying
    @Query("DELETE FROM UserSession s WHERE s.expiresAt < :cutoffDate")
    int deleteExpiredSessions(@Param("cutoffDate") Instant cutoffDate);
}
