package com.skishop.user.repository;

import com.skishop.user.entity.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ユーザー設定リポジトリ
 */
@Repository
public interface UserPreferenceRepository extends JpaRepository<UserPreference, UUID> {

    /**
     * ユーザーIDで設定を検索
     */
    List<UserPreference> findByUserId(UUID userId);

    /**
     * ユーザーIDと設定キーで検索
     */
    Optional<UserPreference> findByUserIdAndPrefKey(UUID userId, String prefKey);

    /**
     * 設定キーで検索
     */
    List<UserPreference> findByPrefKey(String prefKey);

    /**
     * ユーザーIDと設定キーで存在確認
     */
    boolean existsByUserIdAndPrefKey(UUID userId, String prefKey);

    /**
     * ユーザーIDと設定キーで削除
     */
    void deleteByUserIdAndPrefKey(UUID userId, String prefKey);

    /**
     * ユーザーIDで全設定削除
     */
    void deleteByUserId(UUID userId);
}
