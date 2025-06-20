package com.skishop.inventory.service;

import com.skishop.inventory.entity.jpa.Inventory;
import com.skishop.inventory.repository.jpa.InventoryRepository;
import com.skishop.inventory.exception.ResourceNotFoundException;
import com.skishop.inventory.exception.InsufficientStockException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 在庫サービス
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final EventPublisherService eventPublisherService;

    /**
     * 商品の在庫情報取得
     */
    @Cacheable(value = "inventory", key = "#productId")
    public Inventory findByProductId(String productId) {
        log.debug("在庫情報取得 - 商品ID: {}", productId);
        return inventoryRepository.findByProductId(productId)
            .orElseThrow(() -> new ResourceNotFoundException("在庫情報が見つかりません: " + productId));
    }

    /**
     * 商品の利用可能在庫数量取得
     */
    @Cacheable(value = "availableQuantity", key = "#productId")
    public Integer getAvailableQuantity(String productId) {
        log.debug("利用可能在庫数量取得 - 商品ID: {}", productId);
        return inventoryRepository.getAvailableQuantityByProductId(productId)
            .orElseThrow(() -> new ResourceNotFoundException("在庫情報が見つかりません: " + productId));
    }

    /**
     * 複数商品の在庫状況を一括取得（Java 21のStream API改善）
     */
    public Map<String, Inventory> findByProductIds(List<String> productIds) {
        log.debug("複数商品在庫取得 - 商品IDs: {}", productIds);
        List<Inventory> inventories = inventoryRepository.findByProductIdIn(productIds);
        
        // Java 21のtoMap()の簡潔な記述
        return inventories.stream()
            .collect(Collectors.toMap(Inventory::getProductId, identity -> identity));
    }

    /**
     * 在庫予約
     */
    @Transactional
    @CacheEvict(value = {"inventory", "availableQuantity"}, key = "#productId")
    public void reserveStock(String productId, Integer quantity) {
        log.info("在庫予約開始 - 商品ID: {}, 数量: {}", productId, quantity);

        // 利用可能在庫チェック（直接リポジトリを呼び出し）
        Integer availableQuantity = inventoryRepository.getAvailableQuantityByProductId(productId)
            .orElseThrow(() -> new ResourceNotFoundException("在庫情報が見つかりません: " + productId));
        if (availableQuantity < quantity) {
            throw new InsufficientStockException(
                String.format("在庫不足です。要求数量: %d, 利用可能数量: %d", quantity, availableQuantity));
        }

        // 予約数量更新
        int updated = inventoryRepository.increaseReservedQuantity(productId, quantity);
        if (updated == 0) {
            throw new InsufficientStockException("在庫予約に失敗しました");
        }

        // 在庫ステータス更新
        updateInventoryStatus(productId);

        // イベント発行
        eventPublisherService.publishStockReservedEvent(productId, quantity);

        log.info("在庫予約完了 - 商品ID: {}, 数量: {}", productId, quantity);
    }

    /**
     * 在庫予約解除
     */
    @Transactional
    @CacheEvict(value = {"inventory", "availableQuantity"}, key = "#productId")
    public void releaseStock(String productId, Integer quantity) {
        log.info("在庫予約解除開始 - 商品ID: {}, 数量: {}", productId, quantity);

        // 予約数量減少
        int updated = inventoryRepository.decreaseReservedQuantity(productId, quantity);
        if (updated == 0) {
            throw new ResourceNotFoundException("在庫予約の解除に失敗しました");
        }

        // 在庫ステータス更新
        updateInventoryStatus(productId);

        // イベント発行
        eventPublisherService.publishStockReleasedEvent(productId, quantity);

        log.info("在庫予約解除完了 - 商品ID: {}, 数量: {}", productId, quantity);
    }

    /**
     * 入荷処理
     */
    @Transactional
    @CacheEvict(value = {"inventory", "availableQuantity"}, key = "#productId")
    public void stockIn(String productId, Integer quantity) {
        log.info("入荷処理開始 - 商品ID: {}, 数量: {}", productId, quantity);

        Inventory inventory = inventoryRepository.findByProductId(productId)
            .orElseThrow(() -> new ResourceNotFoundException("在庫情報が見つかりません: " + productId));
        inventory.setQuantity(inventory.getQuantity() + quantity);
        inventory.preUpdate();
        inventoryRepository.save(inventory);

        // 在庫ステータス更新
        updateInventoryStatus(productId);

        // イベント発行
        eventPublisherService.publishStockInEvent(productId, quantity);

        log.info("入荷処理完了 - 商品ID: {}, 数量: {}", productId, quantity);
    }

    /**
     * 出荷処理
     */
    @Transactional
    @CacheEvict(value = {"inventory", "availableQuantity"}, key = "#productId")
    public void stockOut(String productId, Integer quantity) {
        log.info("出荷処理開始 - 商品ID: {}, 数量: {}", productId, quantity);

        Inventory inventory = inventoryRepository.findByProductId(productId)
            .orElseThrow(() -> new ResourceNotFoundException("在庫情報が見つかりません: " + productId));
        
        // 予約数量から減算
        if (inventory.getReservedQuantity() >= quantity) {
            inventory.setReservedQuantity(inventory.getReservedQuantity() - quantity);
            inventory.setQuantity(inventory.getQuantity() - quantity);
        } else {
            throw new InsufficientStockException("予約数量が不足しています");
        }

        inventory.preUpdate();
        inventoryRepository.save(inventory);

        // 在庫ステータス更新
        updateInventoryStatus(productId);

        // イベント発行
        eventPublisherService.publishStockOutEvent(productId, quantity);

        log.info("出荷処理完了 - 商品ID: {}, 数量: {}", productId, quantity);
    }

    /**
     * 在庫ステータス更新（Java 21のswitch式を使用）
     */
    private void updateInventoryStatus(String productId) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
            .orElseThrow(() -> new ResourceNotFoundException("在庫情報が見つかりません: " + productId));

        int availableQuantity = inventory.getAvailableQuantity();
        int lowStockThreshold = 5; // 設定可能にする
        
        // Java 21のswitch式とパターンマッチングを活用
        Inventory.InventoryStatus newStatus = Inventory.InventoryStatus.fromQuantity(
            availableQuantity, 
            lowStockThreshold
        );

        if (!newStatus.equals(inventory.getStatus())) {
            inventoryRepository.updateStatusByProductId(productId, newStatus);
            eventPublisherService.publishInventoryStatusChangedEvent(productId, newStatus.name());
        }
    }

    /**
     * 在庫不足商品一覧取得
     */
    public List<Inventory> findLowStockItems(Integer threshold) {
        log.debug("在庫不足商品取得 - 閾値: {}", threshold);
        return inventoryRepository.findLowAvailableStockItems(threshold);
    }
}
