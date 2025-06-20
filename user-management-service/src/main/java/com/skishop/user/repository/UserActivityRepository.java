package com.skishop.user.repository;

import com.skishop.user.entity.UserActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ユーザーアクティビティリポジトリ
 */
@Repository
public interface UserActivityRepository extends JpaRepository<UserActivity, UUID> {

    /**
     * ユーザーIDでアクティビティを検索（作成日時降順）
     */
    Page<UserActivity> findByUser_IdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /**
     * ユーザーIDとアクティビティタイプで検索
     */
    List<UserActivity> findByUser_IdAndActivityType(UUID userId, UserActivity.ActivityType activityType);

    /**
     * アクティビティタイプで検索
     */
    Page<UserActivity> findByActivityType(UserActivity.ActivityType activityType, Pageable pageable);

    /**
     * 指定期間のアクティビティを検索
     */
    @Query("SELECT a FROM UserActivity a WHERE a.createdAt BETWEEN :startDate AND :endDate")
    List<UserActivity> findActivitiesBetween(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * ユーザーの最新ログイン時刻を取得
     */
    @Query("SELECT MAX(a.createdAt) FROM UserActivity a WHERE a.user.id = :userId AND a.activityType = 'LOGIN'")
    Optional<LocalDateTime> findLatestLoginByUserId(@Param("userId") UUID userId);

    /**
     * セキュリティ関連アクティビティを検索
     */
    @Query("""
           SELECT a FROM UserActivity a WHERE a.user.id = :userId 
           AND a.activityType IN ('LOGIN', 'LOGOUT', 'PASSWORD_CHANGE', 'EMAIL_VERIFY', 'PHONE_VERIFY') 
           ORDER BY a.createdAt DESC
           """)
    List<UserActivity> findSecurityActivitiesByUserId(@Param("userId") UUID userId);

    /**
     * IPアドレス別アクティビティ統計
     */
    @Query("SELECT a.ipAddress, COUNT(a) FROM UserActivity a WHERE a.user.id = :userId GROUP BY a.ipAddress")
    List<Object[]> countActivitiesByIpAddress(@Param("userId") UUID userId);

    /**
     * アクティビティタイプ別統計
     */
    @Query("SELECT a.activityType, COUNT(a) FROM UserActivity a WHERE a.createdAt >= :since GROUP BY a.activityType")
    List<Object[]> countActivitiesByTypeSince(@Param("since") LocalDateTime since);
}
