package com.skishop.sales.controller;

import com.skishop.sales.dto.request.ShipmentCreateRequest;
import com.skishop.sales.dto.request.ShipmentStatusUpdateRequest;
import com.skishop.sales.dto.request.TrackingUpdateRequest;
import com.skishop.sales.dto.response.ShipmentDetailResponse;
import com.skishop.sales.dto.response.ShipmentListResponse;
import com.skishop.sales.dto.response.ShipmentResponse;
import com.skishop.sales.service.ShipmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 配送管理API コントローラー
 */
@RestController
@RequestMapping("/api/v1/shipments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Shipment API", description = "配送管理API")
public class ShipmentController {

    private final ShipmentService shipmentService;

    /**
     * 配送一覧取得
     */
    @GetMapping
    @Operation(summary = "配送一覧取得", description = "配送情報の一覧を取得します")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "配送一覧の取得成功"),
        @ApiResponse(responseCode = "403", description = "アクセス権限がありません")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('LOGISTICS_MANAGER')")
    public ResponseEntity<ShipmentListResponse> getShipments(
            @PageableDefault(size = 20) Pageable pageable,
            @Parameter(description = "配送ステータスフィルター") @RequestParam(required = false) String status,
            @Parameter(description = "開始日") @RequestParam(required = false) String fromDate,
            @Parameter(description = "終了日") @RequestParam(required = false) String toDate) {
        
        log.info("Getting shipments with status: {}, page: {}", status, pageable);
        ShipmentListResponse response = shipmentService.getShipments(pageable, status, fromDate, toDate);
        return ResponseEntity.ok(response);
    }

    /**
     * 配送詳細取得
     */
    @GetMapping("/{id}")
    @Operation(summary = "配送詳細取得", description = "指定IDの配送詳細情報を取得します")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "配送詳細の取得成功"),
        @ApiResponse(responseCode = "404", description = "配送情報が見つかりません"),
        @ApiResponse(responseCode = "403", description = "アクセス権限がありません")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('LOGISTICS_MANAGER') or hasRole('CUSTOMER_SERVICE')")
    public ResponseEntity<ShipmentDetailResponse> getShipmentDetail(
            @Parameter(description = "配送ID") @PathVariable Long id) {
        
        log.info("Getting shipment detail for id: {}", id);
        ShipmentDetailResponse response = shipmentService.getShipmentDetail(id);
        return ResponseEntity.ok(response);
    }

    /**
     * 配送情報作成
     */
    @PostMapping
    @Operation(summary = "配送情報作成", description = "新しい配送情報を作成します")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "配送情報の作成成功"),
        @ApiResponse(responseCode = "400", description = "リクエスト内容が不正です"),
        @ApiResponse(responseCode = "403", description = "アクセス権限がありません")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('LOGISTICS_MANAGER')")
    public ResponseEntity<ShipmentDetailResponse> createShipment(
            @Parameter(description = "配送作成リクエスト") @Valid @RequestBody ShipmentCreateRequest request) {
        
        log.info("Creating shipment for order: {}", request.getOrderId());
        ShipmentDetailResponse response = shipmentService.createShipment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 配送ステータス更新
     */
    @PutMapping("/{id}/status")
    @Operation(summary = "配送ステータス更新", description = "配送のステータスを更新します")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "ステータス更新成功"),
        @ApiResponse(responseCode = "404", description = "配送情報が見つかりません"),
        @ApiResponse(responseCode = "400", description = "リクエスト内容が不正です"),
        @ApiResponse(responseCode = "403", description = "アクセス権限がありません")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('LOGISTICS_MANAGER')")
    public ResponseEntity<ShipmentResponse> updateShipmentStatus(
            @Parameter(description = "配送ID") @PathVariable Long id,
            @Parameter(description = "ステータス更新リクエスト") @Valid @RequestBody ShipmentStatusUpdateRequest request) {
        
        log.info("Updating shipment status for id: {}, status: {}", id, request.status());
        ShipmentResponse response = shipmentService.updateShipmentStatus(id.toString(), request);
        return ResponseEntity.ok(response);
    }

    /**
     * 注文配送情報取得
     */
    @GetMapping("/order/{orderId}")
    @Operation(summary = "注文配送情報取得", description = "指定注文IDの配送情報を取得します")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "配送情報の取得成功"),
        @ApiResponse(responseCode = "404", description = "注文が見つかりません"),
        @ApiResponse(responseCode = "403", description = "アクセス権限がありません")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('LOGISTICS_MANAGER') or hasRole('CUSTOMER_SERVICE')")
    public ResponseEntity<ShipmentListResponse> getShipmentsByOrder(
            @Parameter(description = "注文ID") @PathVariable Long orderId) {
        
        log.info("Getting shipments for order: {}", orderId);
        ShipmentListResponse response = shipmentService.getShipmentsByOrder(orderId);
        return ResponseEntity.ok(response);
    }

    /**
     * 追跡情報更新
     */
    @PutMapping("/{id}/tracking")
    @Operation(summary = "追跡情報更新", description = "配送の追跡情報を更新します")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "追跡情報更新成功"),
        @ApiResponse(responseCode = "404", description = "配送情報が見つかりません"),
        @ApiResponse(responseCode = "400", description = "リクエスト内容が不正です"),
        @ApiResponse(responseCode = "403", description = "アクセス権限がありません")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('LOGISTICS_MANAGER')")
    public ResponseEntity<ShipmentResponse> updateTracking(
            @Parameter(description = "配送ID") @PathVariable Long id,
            @Parameter(description = "追跡情報更新リクエスト") @Valid @RequestBody TrackingUpdateRequest request) {
        
        log.info("Updating tracking info for shipment: {}", id);
        ShipmentResponse response = shipmentService.updateTracking(id.toString(), request);
        return ResponseEntity.ok(response);
    }
}
