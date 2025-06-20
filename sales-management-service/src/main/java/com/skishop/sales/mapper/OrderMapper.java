package com.skishop.sales.mapper;

import com.skishop.sales.dto.response.OrderResponse;
import com.skishop.sales.entity.jpa.Order;
import com.skishop.sales.entity.jpa.OrderItem;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 注文マッパー
 * Java 21のRecordに対応した手動実装
 */
@Component
public class OrderMapper {

    /**
     * 注文エンティティをレスポンスDTOに変換
     * RecordのコンストラクタにマッピングするためBuilderは使用しない
     */
    public OrderResponse toResponse(Order order, List<OrderItem> orderItems) {
        if (order == null) {
            return null;
        }

        return new OrderResponse(
            order.getId() != null ? order.getId().toString() : null,
            order.getOrderNumber(),
            order.getCustomerId(),
            order.getOrderDate(),
            order.getStatus() != null ? order.getStatus().name() : null,
            order.getPaymentStatus() != null ? order.getPaymentStatus().name() : null,
            order.getPaymentMethod(),
            order.getSubtotalAmount(),
            order.getTaxAmount(),
            order.getShippingFee(),
            order.getDiscountAmount(),
            order.getTotalAmount(),
            order.getCouponCode(),
            order.getUsedPoints(),
            order.getPointDiscountAmount(),
            toShippingAddressResponse(order.getShippingAddress()),
            order.getNotes(),
            order.getCreatedAt(),
            order.getUpdatedAt(),
            orderItems != null ? orderItems.stream().map(this::toItemResponse).toList() : List.of()
        );
    }

    /**
     * 注文明細エンティティをレスポンスDTOに変換
     */
    public OrderResponse.OrderItemResponse toItemResponse(OrderItem orderItem) {
        if (orderItem == null) {
            return null;
        }

        return new OrderResponse.OrderItemResponse(
            orderItem.getId() != null ? orderItem.getId().toString() : null,
            orderItem.getProductId(),
            orderItem.getProductName(),
            orderItem.getSku(),
            orderItem.getUnitPrice(),
            orderItem.getQuantity(),
            orderItem.getSubtotal(),
            orderItem.getAppliedCouponId(),
            orderItem.getCouponDiscountAmount(),
            orderItem.getUsedPoints(),
            orderItem.getPointDiscountAmount(),
            orderItem.getSubtotal() // actualAmountがない場合は subtotal を使用
        );
    }

    /**
     * 配送先住所エンティティをレスポンスDTOに変換
     */
    public OrderResponse.ShippingAddressResponse toShippingAddressResponse(Order.ShippingAddress shippingAddress) {
        if (shippingAddress == null) {
            return null;
        }

        return new OrderResponse.ShippingAddressResponse(
            shippingAddress.getPostalCode(),
            shippingAddress.getPrefecture(),
            shippingAddress.getCity(),
            shippingAddress.getAddressLine1(),
            shippingAddress.getAddressLine2(),
            shippingAddress.getRecipientName(),
            shippingAddress.getPhone() // EntityのphoneをDTOのphoneNumberにマッピング
        );
    }
}
