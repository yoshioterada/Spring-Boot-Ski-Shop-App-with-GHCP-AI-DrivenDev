package com.skishop.sales.repository.jpa;

import com.skishop.sales.entity.jpa.OrderItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * 注文明細リポジトリ
 */
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    /**
     * 注文IDで注文明細を検索
     */
    List<OrderItem> findByOrderId(UUID orderId);

    /**
     * 注文IDで注文明細を検索（ページング）
     */
    Page<OrderItem> findByOrderId(UUID orderId, Pageable pageable);

    /**
     * 商品IDで注文明細を検索
     */
    Page<OrderItem> findByProductId(String productId, Pageable pageable);

    /**
     * 商品SKUで注文明細を検索
     */
    Page<OrderItem> findBySku(String sku, Pageable pageable);

    /**
     * 注文IDリストで注文明細を検索
     */
    @Query("SELECT oi FROM OrderItem oi WHERE oi.orderId IN :orderIds")
    List<OrderItem> findByOrderIdIn(@Param("orderIds") List<UUID> orderIds);

    /**
     * 特定の商品の販売数量を集計
     */
    @Query("SELECT COALESCE(SUM(oi.quantity), 0) FROM OrderItem oi WHERE oi.productId = :productId")
    Integer getTotalQuantitySoldByProductId(@Param("productId") String productId);

    /**
     * 特定のSKUの販売数量を集計
     */
    @Query("SELECT COALESCE(SUM(oi.quantity), 0) FROM OrderItem oi WHERE oi.sku = :sku")
    Integer getTotalQuantitySoldBySku(@Param("sku") String sku);

    /**
     * 商品別売上ランキング取得
     */
    @Query("SELECT oi.productId, oi.productName, SUM(oi.quantity) as totalQuantity, SUM(oi.subtotal) as totalSales " +
           "FROM OrderItem oi " +
           "GROUP BY oi.productId, oi.productName " +
           "ORDER BY totalSales DESC")
    List<Object[]> getProductSalesRanking(Pageable pageable);

    /**
     * クーポンが適用された注文明細を検索
     */
    List<OrderItem> findByAppliedCouponIdIsNotNull();

    /**
     * ポイントが使用された注文明細を検索
     */
    List<OrderItem> findByUsedPointsGreaterThan(Integer points);

    /**
     * 注文IDで注文明細数をカウント
     */
    long countByOrderId(UUID orderId);

    /**
     * 特定のクーポンが適用された注文明細を検索
     */
    List<OrderItem> findByAppliedCouponId(String couponId);

    /**
     * 商品IDリストで注文明細を検索
     */
    @Query("SELECT oi FROM OrderItem oi WHERE oi.productId IN :productIds")
    List<OrderItem> findByProductIdIn(@Param("productIds") List<String> productIds);

    /**
     * 注文明細の削除
     */
    void deleteByOrderId(UUID orderId);
}
