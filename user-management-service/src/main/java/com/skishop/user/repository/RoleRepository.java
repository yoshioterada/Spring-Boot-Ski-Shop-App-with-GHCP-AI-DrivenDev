package com.skishop.user.repository;

import com.skishop.user.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ロールリポジトリ
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    /**
     * ロール名で検索
     */
    Optional<Role> findByName(String name);

    /**
     * ロール名の存在確認
     */
    boolean existsByName(String name);

    /**
     * 指定した権限を持つロールを検索
     */
    @Query("SELECT r FROM Role r JOIN r.permissions p WHERE p.name = :permissionName")
    List<Role> findByPermissionName(@Param("permissionName") String permissionName);

    /**
     * すべてのロールを名前順で取得
     */
    List<Role> findAllByOrderByName();
}
