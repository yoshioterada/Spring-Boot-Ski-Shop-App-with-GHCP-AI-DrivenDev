package com.skishop.inventory.repository.jpa;

import com.skishop.inventory.entity.jpa.Inventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 在庫リポジトリ（PostgreSQL）
 */
@Repository
public interface InventoryRepository extends JpaRepository<Inventory, UUID> {

    /**
     * 商品IDで在庫を検索
     */
    Optional<Inventory> findByProductId(String productId);

    /**
     * 商品IDとロケーションで在庫を検索
     */
    Optional<Inventory> findByProductIdAndLocationCode(String productId, String locationCode);

    /**
     * 複数商品IDで在庫を検索
     */
    List<Inventory> findByProductIdIn(List<String> productIds);

    /**
     * ロケーションコードで在庫を検索
     */
    Page<Inventory> findByLocationCode(String locationCode, Pageable pageable);

    /**
     * 在庫ステータスで検索
     */
    Page<Inventory> findByStatus(Inventory.InventoryStatus status, Pageable pageable);

    /**
     * 在庫数量が指定値以下のものを検索
     */
    @Query("SELECT i FROM Inventory i WHERE i.quantity <= :threshold")
    List<Inventory> findLowStockItems(@Param("threshold") Integer threshold);

    /**
     * 利用可能在庫数量が指定値以下のものを検索
     */
    @Query("SELECT i FROM Inventory i WHERE (i.quantity - i.reservedQuantity) <= :threshold")
    List<Inventory> findLowAvailableStockItems(@Param("threshold") Integer threshold);

    /**
     * 在庫数量を更新
     */
    @Modifying
    @Query("UPDATE Inventory i SET i.quantity = :quantity, i.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE i.productId = :productId")
    int updateQuantityByProductId(@Param("productId") String productId, 
                                  @Param("quantity") Integer quantity);

    /**
     * 予約数量を増加
     */
    @Modifying
    @Query("UPDATE Inventory i SET i.reservedQuantity = i.reservedQuantity + :amount, " +
           "i.updatedAt = CURRENT_TIMESTAMP WHERE i.productId = :productId AND " +
           "(i.quantity - i.reservedQuantity) >= :amount")
    int increaseReservedQuantity(@Param("productId") String productId, 
                                @Param("amount") Integer amount);

    /**
     * 予約数量を減少
     */
    @Modifying
    @Query("UPDATE Inventory i SET i.reservedQuantity = i.reservedQuantity - :amount, " +
           "i.updatedAt = CURRENT_TIMESTAMP WHERE i.productId = :productId AND " +
           "i.reservedQuantity >= :amount")
    int decreaseReservedQuantity(@Param("productId") String productId, 
                                @Param("amount") Integer amount);

    /**
     * 在庫ステータスを更新
     */
    @Modifying
    @Query("UPDATE Inventory i SET i.status = :status, i.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE i.productId = :productId")
    int updateStatusByProductId(@Param("productId") String productId, 
                               @Param("status") Inventory.InventoryStatus status);

    /**
     * 商品の利用可能在庫数量を取得
     */
    @Query("SELECT (i.quantity - i.reservedQuantity) FROM Inventory i WHERE i.productId = :productId")
    Optional<Integer> getAvailableQuantityByProductId(@Param("productId") String productId);

    /**
     * 在庫が存在するかチェック
     */
    boolean existsByProductId(String productId);

    /**
     * 複数商品の在庫状況を一括取得
     */
    @Query("SELECT i.productId, i.quantity, i.reservedQuantity, i.status FROM Inventory i " +
           "WHERE i.productId IN :productIds")
    List<Object[]> findInventoryStatusByProductIds(@Param("productIds") List<String> productIds);
}
