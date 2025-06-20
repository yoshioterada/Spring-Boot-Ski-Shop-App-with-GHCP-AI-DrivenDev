package com.skishop.sales.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skishop.sales.entity.jpa.Order;
import com.skishop.sales.entity.jpa.OrderItem;
import com.skishop.sales.entity.jpa.Shipment;
import com.skishop.sales.entity.jpa.Return;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * イベント発行サービス
 */
@Service
@RequiredArgsConstructor
public class EventPublisherService {

    private static final Logger log = LoggerFactory.getLogger(EventPublisherService.class);
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String ORDER_TOPIC = "sales.orders";
    private static final String SHIPMENT_TOPIC = "sales.shipments";
    private static final String RETURN_TOPIC = "sales.returns";

    /**
     * 注文作成イベント発行
     * Java 21のMap.of()とString Templateを使用
     */
    public void publishOrderCreatedEvent(Order order, List<OrderItem> orderItems) {
        try {
            var event = Map.of(
                    "eventType", "ORDER_CREATED",
                    "orderId", order.getId().toString(),
                    "orderNumber", order.getOrderNumber(),
                    "customerId", order.getCustomerId(),
                    "totalAmount", order.getTotalAmount(),
                    "itemCount", orderItems.size(),
                    "timestamp", System.currentTimeMillis(),
                    "status", order.getStatus().name(),
                    "paymentMethod", order.getPaymentMethod()
            );

            var eventJson = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(ORDER_TOPIC, order.getId().toString(), eventJson);
            
            log.info("Published ORDER_CREATED event for order: {}", order.getOrderNumber());
        } catch (Exception e) {
            log.error("Failed to publish ORDER_CREATED event for order: {}", order.getOrderNumber(), e);
        }
    }

    /**
     * 注文状態更新イベント発行
     */
    public void publishOrderStatusUpdatedEvent(Order order) {
        try {
            Map<String, Object> event = Map.of(
                    "eventType", "ORDER_STATUS_UPDATED",
                    "orderId", order.getId().toString(),
                    "orderNumber", order.getOrderNumber(),
                    "customerId", order.getCustomerId(),
                    "status", order.getStatus().toString(),
                    "paymentStatus", order.getPaymentStatus().toString(),
                    "timestamp", System.currentTimeMillis()
            );

            String eventJson = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(ORDER_TOPIC, order.getId().toString(), eventJson);
            
            log.info("Published ORDER_STATUS_UPDATED event for order: {}", order.getOrderNumber());
        } catch (Exception e) {
            log.error("Failed to publish ORDER_STATUS_UPDATED event for order: {}", order.getOrderNumber(), e);
        }
    }

    /**
     * 注文キャンセルイベント発行
     */
    public void publishOrderCancelledEvent(Order order) {
        try {
            Map<String, Object> event = Map.of(
                    "eventType", "ORDER_CANCELLED",
                    "orderId", order.getId().toString(),
                    "orderNumber", order.getOrderNumber(),
                    "customerId", order.getCustomerId(),
                    "totalAmount", order.getTotalAmount(),
                    "timestamp", System.currentTimeMillis()
            );

            String eventJson = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(ORDER_TOPIC, order.getId().toString(), eventJson);
            
            log.info("Published ORDER_CANCELLED event for order: {}", order.getOrderNumber());
        } catch (Exception e) {
            log.error("Failed to publish ORDER_CANCELLED event for order: {}", order.getOrderNumber(), e);
        }
    }

    /**
     * 配送作成イベント発行
     */
    public void publishShipmentCreatedEvent(Shipment shipment) {
        try {
            Map<String, Object> event = Map.of(
                    "eventType", "SHIPMENT_CREATED",
                    "shipmentId", shipment.getId().toString(),
                    "orderId", shipment.getOrderId().toString(),
                    "carrier", shipment.getCarrier(),
                    "trackingNumber", shipment.getTrackingNumber() != null ? shipment.getTrackingNumber() : "",
                    "status", shipment.getStatus().toString(),
                    "timestamp", System.currentTimeMillis()
            );

            String eventJson = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(SHIPMENT_TOPIC, shipment.getId().toString(), eventJson);
            
            log.info("Published SHIPMENT_CREATED event for shipment: {}", shipment.getId());
        } catch (Exception e) {
            log.error("Failed to publish SHIPMENT_CREATED event for shipment: {}", shipment.getId(), e);
        }
    }

    /**
     * 配送状態更新イベント発行
     */
    public void publishShipmentStatusUpdatedEvent(Shipment shipment) {
        try {
            Map<String, Object> event = Map.of(
                    "eventType", "SHIPMENT_STATUS_UPDATED",
                    "shipmentId", shipment.getId().toString(),
                    "orderId", shipment.getOrderId().toString(),
                    "carrier", shipment.getCarrier(),
                    "trackingNumber", shipment.getTrackingNumber() != null ? shipment.getTrackingNumber() : "",
                    "status", shipment.getStatus().toString(),
                    "timestamp", System.currentTimeMillis()
            );

            String eventJson = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(SHIPMENT_TOPIC, shipment.getId().toString(), eventJson);
            
            log.info("Published SHIPMENT_STATUS_UPDATED event for shipment: {}", shipment.getId());
        } catch (Exception e) {
            log.error("Failed to publish SHIPMENT_STATUS_UPDATED event for shipment: {}", shipment.getId(), e);
        }
    }

    /**
     * 返品申請イベント発行
     */
    public void publishReturnRequestedEvent(Return returnEntity) {
        try {
            Map<String, Object> event = Map.of(
                    "eventType", "RETURN_REQUESTED",
                    "returnId", returnEntity.getId().toString(),
                    "returnNumber", returnEntity.getReturnNumber(),
                    "orderId", returnEntity.getOrderId().toString(),
                    "customerId", returnEntity.getCustomerId(),
                    "reason", returnEntity.getReason().toString(),
                    "quantity", returnEntity.getQuantity(),
                    "refundAmount", returnEntity.getRefundAmount(),
                    "timestamp", System.currentTimeMillis()
            );

            String eventJson = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(RETURN_TOPIC, returnEntity.getId().toString(), eventJson);
            
            log.info("Published RETURN_REQUESTED event for return: {}", returnEntity.getReturnNumber());
        } catch (Exception e) {
            log.error("Failed to publish RETURN_REQUESTED event for return: {}", returnEntity.getReturnNumber(), e);
        }
    }

    /**
     * 返品状態更新イベント発行
     */
    public void publishReturnStatusUpdatedEvent(Return returnEntity) {
        try {
            Map<String, Object> event = Map.of(
                    "eventType", "RETURN_STATUS_UPDATED",
                    "returnId", returnEntity.getId().toString(),
                    "returnNumber", returnEntity.getReturnNumber(),
                    "orderId", returnEntity.getOrderId().toString(),
                    "customerId", returnEntity.getCustomerId(),
                    "status", returnEntity.getStatus().toString(),
                    "refundAmount", returnEntity.getRefundAmount(),
                    "timestamp", System.currentTimeMillis()
            );

            String eventJson = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(RETURN_TOPIC, returnEntity.getId().toString(), eventJson);
            
            log.info("Published RETURN_STATUS_UPDATED event for return: {}", returnEntity.getReturnNumber());
        } catch (Exception e) {
            log.error("Failed to publish RETURN_STATUS_UPDATED event for return: {}", returnEntity.getReturnNumber(), e);
        }
    }
}
