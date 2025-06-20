package com.skishop.user.repository;

import com.skishop.user.entity.User;
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
 * ユーザーリポジトリ
 * ユーザーデータへのアクセスを提供
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
     * 電話番号でユーザーを検索
     */
    Optional<User> findByPhoneNumber(String phoneNumber);

    /**
     * 電話番号の存在確認
     */
    boolean existsByPhoneNumber(String phoneNumber);

    /**
     * ステータスでユーザーを検索
     */
    List<User> findByStatus(User.UserStatus status);

    /**
     * アクティブなユーザーを検索
     */
    List<User> findByStatusAndEmailVerified(User.UserStatus status, Boolean emailVerified);

    /**
     * 名前で部分一致検索
     */
    @Query("""
           SELECT u FROM User u WHERE 
           LOWER(u.firstName) LIKE LOWER(CONCAT('%', :name, '%')) OR 
           LOWER(u.lastName) LIKE LOWER(CONCAT('%', :name, '%'))
           """)
    Page<User> findByNameContaining(@Param("name") String name, Pageable pageable);

    /**
     * 作成日時範囲でユーザーを検索
     */
    @Query("SELECT u FROM User u WHERE u.createdAt BETWEEN :startDate AND :endDate")
    List<User> findUsersCreatedBetween(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * 指定日以降に作成されたユーザー数を取得
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :date")
    Long countUsersCreatedAfter(@Param("date") LocalDateTime date);

    /**
     * ロール別ユーザー数を取得
     */
    @Query("SELECT u.role.name, COUNT(u) FROM User u GROUP BY u.role.name")
    List<Object[]> countUsersByRole();

    /**
     * メール未認証のユーザーを検索
     */
    @Query("SELECT u FROM User u WHERE u.emailVerified = false AND u.createdAt < :cutoffDate")
    List<User> findUnverifiedUsersOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * 最近ログインしたアクティブユーザーを検索
     */
    @Query("""
           SELECT DISTINCT u FROM User u 
           JOIN u.activities a 
           WHERE u.status = :status 
           AND a.activityType = 'LOGIN' 
           AND a.createdAt >= :since
           """)
    List<User> findActiveUsersWithRecentLogin(
        @Param("status") User.UserStatus status,
        @Param("since") LocalDateTime since
    );

    /**
     * 年齢範囲でユーザーを検索
     */
    @Query("""
           SELECT u FROM User u WHERE 
           EXTRACT(YEAR FROM CURRENT_DATE) - EXTRACT(YEAR FROM u.birthDate) BETWEEN :minAge AND :maxAge
           AND u.birthDate IS NOT NULL
           """)
    List<User> findByAgeRange(@Param("minAge") int minAge, @Param("maxAge") int maxAge);

    /**
     * 性別とステータスでユーザーを検索
     */
    List<User> findByGenderAndStatus(User.Gender gender, User.UserStatus status);

    /**
     * 複数のIDでユーザーを検索
     */
    @Query("SELECT u FROM User u WHERE u.id IN :ids")
    List<User> findByIds(@Param("ids") List<UUID> ids);
}
