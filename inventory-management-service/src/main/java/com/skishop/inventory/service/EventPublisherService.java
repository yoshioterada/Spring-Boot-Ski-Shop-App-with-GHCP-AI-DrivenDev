package com.skishop.inventory.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * イベント発行サービス
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventPublisherService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * 商品作成イベント発行
     */
    public void publishProductCreatedEvent(String productId) {
        try {
            ProductCreatedEvent event = new ProductCreatedEvent(productId);
            kafkaTemplate.send("inventory.product.created", productId, event);
            log.info("商品作成イベント発行完了 - 商品ID: {}", productId);
        } catch (Exception e) {
            log.error("商品作成イベント発行失敗 - 商品ID: {}", productId, e);
        }
    }

    /**
     * 在庫予約イベント発行
     */
    public void publishStockReservedEvent(String productId, Integer quantity) {
        try {
            StockReservedEvent event = new StockReservedEvent(productId, quantity);
            kafkaTemplate.send("inventory.stock.reserved", productId, event);
            log.info("在庫予約イベント発行完了 - 商品ID: {}, 数量: {}", productId, quantity);
        } catch (Exception e) {
            log.error("在庫予約イベント発行失敗 - 商品ID: {}, 数量: {}", productId, quantity, e);
        }
    }

    /**
     * 在庫予約解除イベント発行
     */
    public void publishStockReleasedEvent(String productId, Integer quantity) {
        try {
            StockReleasedEvent event = new StockReleasedEvent(productId, quantity);
            kafkaTemplate.send("inventory.stock.released", productId, event);
            log.info("在庫予約解除イベント発行完了 - 商品ID: {}, 数量: {}", productId, quantity);
        } catch (Exception e) {
            log.error("在庫予約解除イベント発行失敗 - 商品ID: {}, 数量: {}", productId, quantity, e);
        }
    }

    /**
     * 入荷イベント発行
     */
    public void publishStockInEvent(String productId, Integer quantity) {
        try {
            StockInEvent event = new StockInEvent(productId, quantity);
            kafkaTemplate.send("inventory.stock.in", productId, event);
            log.info("入荷イベント発行完了 - 商品ID: {}, 数量: {}", productId, quantity);
        } catch (Exception e) {
            log.error("入荷イベント発行失敗 - 商品ID: {}, 数量: {}", productId, quantity, e);
        }
    }

    /**
     * 出荷イベント発行
     */
    public void publishStockOutEvent(String productId, Integer quantity) {
        try {
            StockOutEvent event = new StockOutEvent(productId, quantity);
            kafkaTemplate.send("inventory.stock.out", productId, event);
            log.info("出荷イベント発行完了 - 商品ID: {}, 数量: {}", productId, quantity);
        } catch (Exception e) {
            log.error("出荷イベント発行失敗 - 商品ID: {}, 数量: {}", productId, quantity, e);
        }
    }

    /**
     * 在庫ステータス変更イベント発行
     */
    public void publishInventoryStatusChangedEvent(String productId, String status) {
        try {
            InventoryStatusChangedEvent event = new InventoryStatusChangedEvent(productId, status);
            kafkaTemplate.send("inventory.status.changed", productId, event);
            log.info("在庫ステータス変更イベント発行完了 - 商品ID: {}, ステータス: {}", productId, status);
        } catch (Exception e) {
            log.error("在庫ステータス変更イベント発行失敗 - 商品ID: {}, ステータス: {}", productId, status, e);
        }
    }

    // イベントクラス定義
    public record ProductCreatedEvent(String productId) {}
    public record StockReservedEvent(String productId, Integer quantity) {}
    public record StockReleasedEvent(String productId, Integer quantity) {}
    public record StockInEvent(String productId, Integer quantity) {}
    public record StockOutEvent(String productId, Integer quantity) {}
    public record InventoryStatusChangedEvent(String productId, String status) {}
}
