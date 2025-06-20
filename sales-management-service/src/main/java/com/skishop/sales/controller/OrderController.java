package com.skishop.sales.controller;

import com.skishop.sales.dto.request.OrderCreateRequest;
import com.skishop.sales.dto.request.OrderStatusUpdateRequest;
import com.skishop.sales.dto.response.OrderResponse;
import com.skishop.sales.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 注文コントローラー
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Order Management", description = "注文管理API")
public class OrderController {

    private final OrderService orderService;

    /**
     * 注文作成
     */
    @PostMapping
    @Operation(summary = "注文作成", description = "新しい注文を作成します")
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody OrderCreateRequest request) {
        log.info("Creating order for customer: {}", request.customerId());
        
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 注文取得
     */
    @GetMapping("/{orderId}")
    @Operation(summary = "注文取得", description = "指定されたIDの注文を取得します")
    public ResponseEntity<OrderResponse> getOrder(
            @Parameter(description = "注文ID") @PathVariable UUID orderId) {
        log.info("Getting order: {}", orderId);
        
        OrderResponse response = orderService.getOrder(orderId);
        return ResponseEntity.ok(response);
    }

    /**
     * 注文番号で取得
     */
    @GetMapping("/number/{orderNumber}")
    @Operation(summary = "注文番号で取得", description = "注文番号で注文を取得します")
    public ResponseEntity<OrderResponse> getOrderByNumber(
            @Parameter(description = "注文番号") @PathVariable String orderNumber) {
        log.info("Getting order by number: {}", orderNumber);
        
        OrderResponse response = orderService.getOrderByNumber(orderNumber);
        return ResponseEntity.ok(response);
    }

    /**
     * 顧客の注文一覧取得
     */
    @GetMapping("/customer/{customerId}")
    @Operation(summary = "顧客の注文一覧取得", description = "指定された顧客の注文一覧を取得します")
    public ResponseEntity<Page<OrderResponse>> getOrdersByCustomer(
            @Parameter(description = "顧客ID") @PathVariable String customerId,
            @PageableDefault(size = 20) Pageable pageable) {
        log.info("Getting orders for customer: {}", customerId);
        
        Page<OrderResponse> response = orderService.getOrdersByCustomer(customerId, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * 注文状態更新
     */
    @PutMapping("/{orderId}/status")
    @Operation(summary = "注文状態更新", description = "注文の状態を更新します")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @Parameter(description = "注文ID") @PathVariable UUID orderId,
            @Valid @RequestBody OrderStatusUpdateRequest request) {
        log.info("Updating order status: {} to {}", orderId, request.status());
        
        OrderResponse response = orderService.updateOrderStatus(orderId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 注文キャンセル
     */
    @PutMapping("/{orderId}/cancel")
    @Operation(summary = "注文キャンセル", description = "注文をキャンセルします")
    public ResponseEntity<OrderResponse> cancelOrder(
            @Parameter(description = "注文ID") @PathVariable UUID orderId,
            @Parameter(description = "キャンセル理由") @RequestParam(required = false) String reason) {
        log.info("Cancelling order: {} with reason: {}", orderId, reason);
        
        OrderResponse response = orderService.cancelOrder(orderId, reason);
        return ResponseEntity.ok(response);
    }

    /**
     * 注文検索
     * Java 21のSwitch式を使用して検索条件を処理
     */
    @GetMapping("/search")
    @Operation(summary = "注文検索", description = "条件を指定して注文を検索します")
    public ResponseEntity<Page<OrderResponse>> searchOrders(
            @Parameter(description = "顧客ID") @RequestParam(required = false) String customerId,
            @Parameter(description = "注文状態") @RequestParam(required = false) String status,
            @Parameter(description = "支払い状態") @RequestParam(required = false) String paymentStatus,
            @PageableDefault(size = 20) Pageable pageable) {
        
        var searchCriteria = String.format("customerId=%s, status=%s, paymentStatus=%s", 
                customerId, status, paymentStatus);
        log.info("Searching orders with criteria: {}", searchCriteria);
        
        // Java 21のSwitch式を使用した検索ロジック（将来の実装用）
        var searchType = switch (customerId != null ? "HAS_CUSTOMER" : "NO_CUSTOMER") {
            case "HAS_CUSTOMER" -> status != null ? "CUSTOMER_AND_STATUS" : "CUSTOMER_ONLY";
            case "NO_CUSTOMER" -> status != null ? "STATUS_ONLY" : "ALL";
            default -> "ALL";
        };
        
        log.debug("Search type determined: {}", searchType);
        
        // 検索条件に応じた検索ロジック（現在は空の結果を返す）
        // 将来的には以下の検索タイプに応じた実装を行う予定：
        // - CUSTOMER_AND_STATUS: 顧客IDと状態での絞り込み
        // - CUSTOMER_ONLY: 顧客IDでの絞り込み  
        // - STATUS_ONLY: 状態での絞り込み
        // - ALL: 全件取得
        Page<OrderResponse> response = Page.empty();
        return ResponseEntity.ok(response);
    }
}
