package com.skishop.sales.service;

import com.skishop.sales.dto.request.OrderCreateRequest;
import com.skishop.sales.dto.request.OrderStatusUpdateRequest;
import com.skishop.sales.dto.response.OrderResponse;
import com.skishop.sales.entity.jpa.Order;
import com.skishop.sales.entity.jpa.OrderItem;
import com.skishop.sales.exception.ResourceNotFoundException;
import com.skishop.sales.exception.InvalidOrderStateException;
import com.skishop.sales.mapper.OrderMapper;
import com.skishop.sales.repository.jpa.OrderRepository;
import com.skishop.sales.repository.jpa.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 注文サービス
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderMapper orderMapper;
    private final OrderNumberGenerator orderNumberGenerator;
    private final EventPublisherService eventPublisherService;

    /**
     * 注文作成
     */
    @Transactional
    public OrderResponse createOrder(OrderCreateRequest request) {
        log.info("Creating order for customer: {}", request.customerId());

        // 注文番号生成
        String orderNumber = orderNumberGenerator.generate();

        // 注文エンティティ作成
        Order order = Order.builder()
                .orderNumber(orderNumber)
                .customerId(request.customerId())
                .orderDate(LocalDateTime.now())
                .status(Order.OrderStatus.PENDING)
                .paymentStatus(Order.PaymentStatus.PENDING)
                .paymentMethod(request.paymentMethod())
                .couponCode(request.couponCode())
                .usedPoints(request.usedPoints())
                .notes(request.notes())
                .build();

        // 配送先住所設定
        if (request.shippingAddress() != null) {
            Order.ShippingAddress shippingAddress = new Order.ShippingAddress(
                    request.shippingAddress().postalCode(),
                    request.shippingAddress().prefecture(),
                    request.shippingAddress().city(),
                    request.shippingAddress().addressLine1(),
                    request.shippingAddress().addressLine2(),
                    request.shippingAddress().recipientName(),
                    request.shippingAddress().phoneNumber()
            );
            order.setShippingAddress(shippingAddress);
        }

        // 注文保存
        Order savedOrder = orderRepository.save(order);

        // 注文明細作成
        List<OrderItem> orderItems = request.items().stream()
                .map(itemRequest -> {
                    OrderItem orderItem = OrderItem.builder()
                            .orderId(savedOrder.getId())
                            .productId(itemRequest.productId())
                            .productName(itemRequest.productName())
                            .sku(itemRequest.sku())
                            .unitPrice(itemRequest.unitPrice())
                            .quantity(itemRequest.quantity())
                            .build();
                    orderItem.calculateSubtotal();
                    return orderItem;
                })
                .toList();

        orderItemRepository.saveAll(orderItems);

        // 金額計算
        calculateOrderAmounts(savedOrder, orderItems);
        orderRepository.save(savedOrder);

        // イベント発行
        eventPublisherService.publishOrderCreatedEvent(savedOrder, orderItems);

        log.info("Order created successfully: {}", orderNumber);
        return orderMapper.toResponse(savedOrder, orderItems);
    }

    /**
     * 注文取得
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
        return orderMapper.toResponse(order, orderItems);
    }

    /**
     * 注文番号で取得
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrderByNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderNumber));

        List<OrderItem> orderItems = orderItemRepository.findByOrderId(order.getId());
        return orderMapper.toResponse(order, orderItems);
    }

    /**
     * 顧客の注文一覧取得
     */
    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrdersByCustomer(String customerId, Pageable pageable) {
        Page<Order> orders = orderRepository.findByCustomerId(customerId, pageable);
        return orders.map(order -> {
            List<OrderItem> orderItems = orderItemRepository.findByOrderId(order.getId());
            return orderMapper.toResponse(order, orderItems);
        });
    }

    /**
     * 注文状態更新
     */
    @Transactional
    public OrderResponse updateOrderStatus(UUID orderId, OrderStatusUpdateRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        Order.OrderStatus newStatus = Order.OrderStatus.valueOf(request.status());
        
        // 状態遷移の妥当性チェック
        validateStatusTransition(order.getStatus(), newStatus);

        order.setStatus(newStatus);
        if (request.notes() != null) {
            order.setNotes(order.getNotes() + "\n" + request.notes());
        }

        Order savedOrder = orderRepository.save(order);

        // イベント発行
        eventPublisherService.publishOrderStatusUpdatedEvent(savedOrder);

        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
        return orderMapper.toResponse(savedOrder, orderItems);
    }

    /**
     * 注文キャンセル
     */
    @Transactional
    public OrderResponse cancelOrder(UUID orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if (!canCancelOrder(order.getStatus())) {
            throw new InvalidOrderStateException("Cannot cancel order in status: " + order.getStatus());
        }

        order.setStatus(Order.OrderStatus.CANCELLED);
        order.setNotes(order.getNotes() + "\nCancelled: " + reason);

        Order savedOrder = orderRepository.save(order);

        // イベント発行
        eventPublisherService.publishOrderCancelledEvent(savedOrder);

        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
        return orderMapper.toResponse(savedOrder, orderItems);
    }

    /**
     * 注文金額計算
     */
    private void calculateOrderAmounts(Order order, List<OrderItem> orderItems) {
        BigDecimal subtotal = orderItems.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal taxAmount = subtotal.multiply(BigDecimal.valueOf(0.1)); // 10%税率
        BigDecimal shippingFee = calculateShippingFee(subtotal);
        BigDecimal discountAmount = calculateDiscountAmount(order);

        order.setSubtotalAmount(subtotal);
        order.setTaxAmount(taxAmount);
        order.setShippingFee(shippingFee);
        order.setDiscountAmount(discountAmount);
        order.setTotalAmount(subtotal.add(taxAmount).add(shippingFee).subtract(discountAmount));
    }

    /**
     * 送料計算
     * Java 21のSwitch式を使用して段階的な送料計算を実装
     */
    private BigDecimal calculateShippingFee(BigDecimal subtotal) {
        return switch (subtotal.compareTo(BigDecimal.ZERO)) {
            case -1 -> BigDecimal.ZERO; // 負の値の場合
            case 0 -> BigDecimal.valueOf(800); // 0円の場合
            default -> switch (subtotal.compareTo(BigDecimal.valueOf(5000))) {
                case -1 -> BigDecimal.valueOf(500); // 5000円未満
                default -> switch (subtotal.compareTo(BigDecimal.valueOf(10000))) {
                    case -1 -> BigDecimal.valueOf(300); // 5000円以上10000円未満
                    default -> BigDecimal.ZERO; // 10000円以上で送料無料
                };
            };
        };
    }

    /**
     * 割引額計算
     */
    private BigDecimal calculateDiscountAmount(Order order) {
        BigDecimal discount = BigDecimal.ZERO;

        // ポイント割引
        if (order.getUsedPoints() != null && order.getUsedPoints() > 0) {
            BigDecimal pointDiscount = BigDecimal.valueOf(order.getUsedPoints());
            order.setPointDiscountAmount(pointDiscount);
            discount = discount.add(pointDiscount);
        }

        return discount;
    }

    /**
     * 状態遷移の妥当性チェック
     * Java 21のパターンマッチングとSwitch式を使用
     */
    private void validateStatusTransition(Order.OrderStatus currentStatus, Order.OrderStatus newStatus) {
        boolean isValidTransition = switch (currentStatus) {
            case CANCELLED -> newStatus == Order.OrderStatus.CANCELLED;
            case DELIVERED -> newStatus == Order.OrderStatus.RETURNED || newStatus == Order.OrderStatus.DELIVERED;
            case PENDING -> newStatus != Order.OrderStatus.DELIVERED;
            case CONFIRMED -> newStatus != Order.OrderStatus.PENDING;
            case PROCESSING -> newStatus != Order.OrderStatus.PENDING && newStatus != Order.OrderStatus.CONFIRMED;
            case SHIPPED -> newStatus == Order.OrderStatus.DELIVERED || newStatus == Order.OrderStatus.RETURNED || newStatus == Order.OrderStatus.SHIPPED;
            case RETURNED -> newStatus == Order.OrderStatus.RETURNED;
        };

        if (!isValidTransition) {
            throw new InvalidOrderStateException("Cannot change status from " + currentStatus + " to " + newStatus);
        }
    }

    /**
     * キャンセル可能かチェック
     * Java 21のSwitch式を使用してより簡潔に記述
     */
    private boolean canCancelOrder(Order.OrderStatus status) {
        return switch (status) {
            case PENDING, CONFIRMED -> true;
            case PROCESSING, SHIPPED, DELIVERED, CANCELLED, RETURNED -> false;
        };
    }
}
