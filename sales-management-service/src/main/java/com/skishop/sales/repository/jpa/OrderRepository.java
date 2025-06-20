package com.skishop.sales.repository.jpa;

import com.skishop.sales.entity.jpa.Order;
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
 * 注文リポジトリ
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    /**
     * 注文番号で検索
     */
    Optional<Order> findByOrderNumber(String orderNumber);

    /**
     * 顧客IDで注文を検索
     */
    Page<Order> findByCustomerId(String customerId, Pageable pageable);

    /**
     * 注文状態で検索
     */
    Page<Order> findByStatus(Order.OrderStatus status, Pageable pageable);

    /**
     * 顧客IDと注文状態で検索
     */
    Page<Order> findByCustomerIdAndStatus(String customerId, Order.OrderStatus status, Pageable pageable);

    /**
     * 期間で検索
     */
    Page<Order> findByOrderDateBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    /**
     * 顧客IDと期間で検索
     */
    Page<Order> findByCustomerIdAndOrderDateBetween(
        String customerId, 
        LocalDateTime startDate, 
        LocalDateTime endDate, 
        Pageable pageable
    );

    /**
     * 注文状態のリストで検索
     */
    @Query("SELECT o FROM Order o WHERE o.status IN :statuses")
    Page<Order> findByStatusIn(@Param("statuses") List<Order.OrderStatus> statuses, Pageable pageable);

    /**
     * 顧客の最新注文を取得
     */
    @Query("SELECT o FROM Order o WHERE o.customerId = :customerId ORDER BY o.orderDate DESC")
    Page<Order> findLatestOrdersByCustomerId(@Param("customerId") String customerId, Pageable pageable);

    /**
     * 指定期間の注文数をカウント
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.orderDate BETWEEN :startDate AND :endDate")
    long countOrdersBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * 指定期間の売上合計を取得
     */
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.orderDate BETWEEN :startDate AND :endDate AND o.status = 'COMPLETED'")
    java.math.BigDecimal getTotalSalesBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * 顧客の注文履歴数をカウント
     */
    long countByCustomerId(String customerId);

    /**
     * 支払い状態で検索
     */
    Page<Order> findByPaymentStatus(Order.PaymentStatus paymentStatus, Pageable pageable);

    /**
     * 注文番号の存在確認
     */
    boolean existsByOrderNumber(String orderNumber);

    /**
     * 顧客IDと注文状態のリストで検索
     */
    @Query("SELECT o FROM Order o WHERE o.customerId = :customerId AND o.status IN :statuses ORDER BY o.orderDate DESC")
    Page<Order> findByCustomerIdAndStatusIn(
        @Param("customerId") String customerId, 
        @Param("statuses") List<Order.OrderStatus> statuses, 
        Pageable pageable
    );

    /**
     * 期限切れの未払い注文を検索
     */
    @Query("SELECT o FROM Order o WHERE o.paymentStatus = 'PENDING' AND o.createdAt < :expiryTime")
    List<Order> findExpiredPendingOrders(@Param("expiryTime") LocalDateTime expiryTime);
}
