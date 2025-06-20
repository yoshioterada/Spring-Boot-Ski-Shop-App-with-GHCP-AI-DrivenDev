package com.skishop.inventory.repository.jpa;

import com.skishop.inventory.entity.jpa.Price;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 価格リポジトリ（PostgreSQL）
 */
@Repository
public interface PriceRepository extends JpaRepository<Price, UUID> {

    /**
     * 商品IDでアクティブな価格を検索
     */
    Optional<Price> findByProductIdAndIsActiveTrue(String productId);

    /**
     * 商品IDで価格履歴を検索
     */
    Page<Price> findByProductIdOrderByCreatedAtDesc(String productId, Pageable pageable);

    /**
     * 複数商品IDでアクティブな価格を検索
     */
    List<Price> findByProductIdInAndIsActiveTrue(List<String> productIds);

    /**
     * セール中の商品価格を検索
     */
    @Query("SELECT p FROM Price p WHERE p.isActive = true AND p.salePrice IS NOT NULL AND " +
           "p.saleStartDate <= :now AND p.saleEndDate >= :now")
    List<Price> findActiveSalePrices(@Param("now") LocalDateTime now);

    /**
     * 価格範囲で商品を検索
     */
    @Query("SELECT p FROM Price p WHERE p.isActive = true AND " +
           "((p.salePrice IS NOT NULL AND p.saleStartDate <= :now AND p.saleEndDate >= :now AND " +
           "p.salePrice BETWEEN :minPrice AND :maxPrice) OR " +
           "(p.salePrice IS NULL OR p.saleStartDate > :now OR p.saleEndDate < :now) AND " +
           "p.regularPrice BETWEEN :minPrice AND :maxPrice)")
    Page<Price> findByPriceRange(@Param("minPrice") BigDecimal minPrice, 
                                @Param("maxPrice") BigDecimal maxPrice,
                                @Param("now") LocalDateTime now,
                                Pageable pageable);

    /**
     * 通貨コードで価格を検索
     */
    Page<Price> findByCurrencyCodeAndIsActiveTrue(String currencyCode, Pageable pageable);

    /**
     * セール期間が終了した価格を検索
     */
    @Query("SELECT p FROM Price p WHERE p.isActive = true AND p.saleEndDate < :now")
    List<Price> findExpiredSalePrices(@Param("now") LocalDateTime now);

    /**
     * 商品の現在有効価格を取得
     */
    @Query("SELECT CASE " +
           "WHEN p.salePrice IS NOT NULL AND p.saleStartDate <= :now AND p.saleEndDate >= :now " +
           "THEN p.salePrice " +
           "ELSE p.regularPrice END " +
           "FROM Price p WHERE p.productId = :productId AND p.isActive = true")
    Optional<BigDecimal> getCurrentPriceByProductId(@Param("productId") String productId, 
                                                   @Param("now") LocalDateTime now);

    /**
     * 商品がセール中かチェック
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END " +
           "FROM Price p WHERE p.productId = :productId AND p.isActive = true AND " +
           "p.salePrice IS NOT NULL AND p.saleStartDate <= :now AND p.saleEndDate >= :now")
    boolean isProductOnSale(@Param("productId") String productId, @Param("now") LocalDateTime now);

    /**
     * 商品価格が存在するかチェック
     */
    boolean existsByProductIdAndIsActiveTrue(String productId);

    /**
     * 複数商品の現在価格を一括取得
     */
    @Query("SELECT p.productId, " +
           "CASE WHEN p.salePrice IS NOT NULL AND p.saleStartDate <= :now AND p.saleEndDate >= :now " +
           "THEN p.salePrice ELSE p.regularPrice END as currentPrice, " +
           "CASE WHEN p.salePrice IS NOT NULL AND p.saleStartDate <= :now AND p.saleEndDate >= :now " +
           "THEN true ELSE false END as onSale " +
           "FROM Price p WHERE p.productId IN :productIds AND p.isActive = true")
    List<Object[]> getCurrentPricesByProductIds(@Param("productIds") List<String> productIds,
                                               @Param("now") LocalDateTime now);
}
