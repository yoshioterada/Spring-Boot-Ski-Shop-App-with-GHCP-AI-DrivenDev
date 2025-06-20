package com.skishop.inventory.repository.mongo;

import com.skishop.inventory.entity.mongo.Category;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * カテゴリリポジトリ（MongoDB）
 */
@Repository
public interface CategoryRepository extends MongoRepository<Category, String> {

    /**
     * 名前でカテゴリを検索
     */
    Optional<Category> findByNameAndActiveTrue(String name);

    /**
     * 親カテゴリIDでカテゴリを検索
     */
    List<Category> findByParentIdAndActiveTrueOrderByName(String parentId);

    /**
     * ルートカテゴリを検索
     */
    List<Category> findByParentIdIsNullAndActiveTrueOrderByName();

    /**
     * レベル別でカテゴリを検索
     */
    List<Category> findByLevelAndActiveTrueOrderByName(Integer level);

    /**
     * アクティブなカテゴリをすべて取得
     */
    List<Category> findByActiveTrueOrderByPathAsc();

    /**
     * パスでカテゴリを検索
     */
    Optional<Category> findByPathAndActiveTrue(String path);

    /**
     * 階層パスの部分一致検索
     */
    @Query("{'path': {$regex: ?0, $options: 'i'}, 'active': true}")
    List<Category> findByPathContainingIgnoreCase(String pathPart);

    /**
     * 子カテゴリが存在するかチェック
     */
    boolean existsByParentIdAndActiveTrue(String parentId);

    /**
     * 親カテゴリとその全ての子カテゴリを取得
     */
    @Query("{'path': {$regex: ?0}, 'active': true}")
    List<Category> findCategoryHierarchy(String parentPath);
}
