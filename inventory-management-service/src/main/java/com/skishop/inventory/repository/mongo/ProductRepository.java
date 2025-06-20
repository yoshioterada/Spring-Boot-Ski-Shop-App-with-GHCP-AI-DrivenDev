package com.skishop.inventory.repository.mongo;

import com.skishop.inventory.entity.mongo.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 商品リポジトリ（MongoDB）
 */
@Repository
public interface ProductRepository extends MongoRepository<Product, String> {

    /**
     * SKUで商品を検索
     */
    Optional<Product> findBySku(String sku);

    /**
     * カテゴリIDで商品を検索
     */
    Page<Product> findByCategoryIdAndActiveTrue(String categoryId, Pageable pageable);

    /**
     * アクティブな商品を検索
     */
    Page<Product> findByActiveTrue(Pageable pageable);

    /**
     * ブランドで商品を検索
     */
    Page<Product> findByBrandAndActiveTrue(String brand, Pageable pageable);

    /**
     * 商品名での部分一致検索
     */
    @Query("{'name': {$regex: ?0, $options: 'i'}, 'active': true}")
    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /**
     * タグでの商品検索
     */
    Page<Product> findByTagsInAndActiveTrue(List<String> tags, Pageable pageable);

    /**
     * 複合検索（名前、説明、ブランド、タグ）- Java 21テキストブロック使用
     */
    @Query("""
           {
               $and: [
                   {'active': true},
                   {
                       $or: [
                           {'name': {$regex: ?0, $options: 'i'}},
                           {'description': {$regex: ?0, $options: 'i'}},
                           {'brand': {$regex: ?0, $options: 'i'}},
                           {'tags': {$in: [?0]}}
                       ]
                   }
               ]
           }
           """)
    Page<Product> searchProducts(String keyword, Pageable pageable);

    /**
     * カテゴリ・キーワード複合検索 - Java 21テキストブロック使用
     */
    @Query("""
           {
               $and: [
                   {'categoryId': ?0},
                   {'active': true},
                   {
                       $or: [
                           {'name': {$regex: ?1, $options: 'i'}},
                           {'description': {$regex: ?1, $options: 'i'}},
                           {'brand': {$regex: ?1, $options: 'i'}},
                           {'tags': {$in: [?1]}}
                       ]
                   }
               ]
           }
           """)
    Page<Product> searchProductsByCategory(String categoryId, String keyword, Pageable pageable);

    /**
     * 商品IDのリストで複数商品を検索
     */
    List<Product> findByIdInAndActiveTrue(List<String> ids);

    /**
     * ブランド一覧を取得
     */
    @Query(value = "{}", fields = "{'brand': 1}")
    List<String> findDistinctBrands();

    /**
     * 商品が存在するかチェック
     */
    boolean existsBySkuAndActiveTrue(String sku);
}
