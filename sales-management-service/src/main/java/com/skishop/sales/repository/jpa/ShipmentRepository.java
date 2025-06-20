package com.skishop.sales.repository.jpa;

import com.skishop.sales.entity.jpa.Shipment;
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
 * 配送リポジトリ
 */
@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, UUID> {

    /**
     * 注文IDで配送情報を検索
     */
    Optional<Shipment> findByOrderId(UUID orderId);

    /**
     * 追跡番号で配送情報を検索
     */
    Optional<Shipment> findByTrackingNumber(String trackingNumber);

    /**
     * 配送状態で検索
     */
    Page<Shipment> findByStatus(Shipment.ShipmentStatus status, Pageable pageable);

    /**
     * 配送業者で検索
     */
    Page<Shipment> findByCarrier(String carrier, Pageable pageable);

    /**
     * 配送業者と配送状態で検索
     */
    Page<Shipment> findByCarrierAndStatus(String carrier, Shipment.ShipmentStatus status, Pageable pageable);

    /**
     * 発送日期間で検索
     */
    @Query("SELECT s FROM Shipment s WHERE s.shippedAt BETWEEN :startDate AND :endDate")
    Page<Shipment> findByShippedAtBetween(
        @Param("startDate") LocalDateTime startDate, 
        @Param("endDate") LocalDateTime endDate, 
        Pageable pageable
    );

    /**
     * 配達予定日期間で検索
     */
    @Query("SELECT s FROM Shipment s WHERE s.estimatedDeliveryAt BETWEEN :startDate AND :endDate")
    Page<Shipment> findByEstimatedDeliveryAtBetween(
        @Param("startDate") LocalDateTime startDate, 
        @Param("endDate") LocalDateTime endDate, 
        Pageable pageable
    );

    /**
     * 配達予定日を過ぎた配送を検索
     */
    @Query("SELECT s FROM Shipment s WHERE s.estimatedDeliveryAt < :currentTime AND s.status NOT IN ('DELIVERED', 'RETURNED')")
    List<Shipment> findOverdueShipments(@Param("currentTime") LocalDateTime currentTime);

    /**
     * 配送状態のリストで検索
     */
    @Query("SELECT s FROM Shipment s WHERE s.status IN :statuses")
    Page<Shipment> findByStatusIn(@Param("statuses") List<Shipment.ShipmentStatus> statuses, Pageable pageable);

    /**
     * 追跡番号の存在確認
     */
    boolean existsByTrackingNumber(String trackingNumber);

    /**
     * 配送業者別の配送数をカウント
     */
    @Query("SELECT s.carrier, COUNT(s) FROM Shipment s GROUP BY s.carrier")
    List<Object[]> countShipmentsByCarrier();

    /**
     * 配送状態別の配送数をカウント
     */
    @Query("SELECT s.status, COUNT(s) FROM Shipment s GROUP BY s.status")
    List<Object[]> countShipmentsByStatus();

    /**
     * 注文IDリストで配送情報を検索
     */
    @Query("SELECT s FROM Shipment s WHERE s.orderId IN :orderIds")
    List<Shipment> findByOrderIdIn(@Param("orderIds") List<UUID> orderIds);
}
