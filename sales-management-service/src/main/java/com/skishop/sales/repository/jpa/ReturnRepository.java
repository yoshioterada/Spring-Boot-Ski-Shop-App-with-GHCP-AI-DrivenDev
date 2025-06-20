package com.skishop.sales.repository.jpa;

import com.skishop.sales.entity.jpa.Return;
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
 * 返品リポジトリ
 */
@Repository
public interface ReturnRepository extends JpaRepository<Return, UUID> {

    /**
     * 返品番号で検索
     */
    Optional<Return> findByReturnNumber(String returnNumber);

    /**
     * 注文IDで返品を検索
     */
    List<Return> findByOrderId(UUID orderId);

    /**
     * 注文明細IDで返品を検索
     */
    List<Return> findByOrderItemId(UUID orderItemId);

    /**
     * 顧客IDで返品を検索
     */
    Page<Return> findByCustomerId(String customerId, Pageable pageable);

    /**
     * 返品状態で検索
     */
    Page<Return> findByStatus(Return.ReturnStatus status, Pageable pageable);

    /**
     * 返品理由で検索
     */
    Page<Return> findByReason(Return.ReturnReason reason, Pageable pageable);

    /**
     * 顧客IDと返品状態で検索
     */
    Page<Return> findByCustomerIdAndStatus(String customerId, Return.ReturnStatus status, Pageable pageable);

    /**
     * 申請期間で検索
     */
    Page<Return> findByRequestedAtBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    /**
     * 返品状態のリストで検索
     */
    @Query("SELECT r FROM Return r WHERE r.status IN :statuses")
    Page<Return> findByStatusIn(@Param("statuses") List<Return.ReturnStatus> statuses, Pageable pageable);

    /**
     * 承認待ちの返品を検索
     */
    @Query("SELECT r FROM Return r WHERE r.status = 'REQUESTED' ORDER BY r.requestedAt ASC")
    Page<Return> findPendingReturns(Pageable pageable);

    /**
     * 期限切れの返品申請を検索
     */
    @Query("SELECT r FROM Return r WHERE r.status = 'REQUESTED' AND r.requestedAt < :expiryTime")
    List<Return> findExpiredReturnRequests(@Param("expiryTime") LocalDateTime expiryTime);

    /**
     * 返品理由別の返品数をカウント
     */
    @Query("SELECT r.reason, COUNT(r) FROM Return r GROUP BY r.reason")
    List<Object[]> countReturnsByReason();

    /**
     * 返品状態別の返品数をカウント
     */
    @Query("SELECT r.status, COUNT(r) FROM Return r GROUP BY r.status")
    List<Object[]> countReturnsByStatus();

    /**
     * 顧客の返品履歴数をカウント
     */
    long countByCustomerId(String customerId);

    /**
     * 指定期間の返品数をカウント
     */
    @Query("SELECT COUNT(r) FROM Return r WHERE r.requestedAt BETWEEN :startDate AND :endDate")
    long countReturnsBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * 指定期間の返金総額を取得
     */
    @Query("SELECT COALESCE(SUM(r.refundAmount), 0) FROM Return r WHERE r.status = 'REFUNDED' AND r.refundedAt BETWEEN :startDate AND :endDate")
    java.math.BigDecimal getTotalRefundsBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * 返品番号の存在確認
     */
    boolean existsByReturnNumber(String returnNumber);

    /**
     * 注文IDリストで返品を検索
     */
    @Query("SELECT r FROM Return r WHERE r.orderId IN :orderIds")
    List<Return> findByOrderIdIn(@Param("orderIds") List<UUID> orderIds);

    /**
     * 顧客の最新返品を取得
     */
    @Query("SELECT r FROM Return r WHERE r.customerId = :customerId ORDER BY r.requestedAt DESC")
    Page<Return> findLatestReturnsByCustomerId(@Param("customerId") String customerId, Pageable pageable);
}
