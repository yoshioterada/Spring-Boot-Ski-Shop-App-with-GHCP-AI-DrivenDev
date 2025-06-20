package com.skishop.sales.controller;

import com.skishop.sales.dto.request.ReturnCreateRequest;
import com.skishop.sales.dto.request.ReturnStatusUpdateRequest;
import com.skishop.sales.dto.response.ReturnDetailResponse;
import com.skishop.sales.dto.response.ReturnListResponse;
import com.skishop.sales.dto.response.ReturnResponse;
import com.skishop.sales.service.ReturnService;
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
 * 返品管理API コントローラー
 */
@RestController
@RequestMapping("/api/v1/returns")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Return API", description = "返品管理API")
public class ReturnController {

    private final ReturnService returnService;

    /**
     * 返品一覧取得
     */
    @GetMapping
    @Operation(summary = "返品一覧取得", description = "返品情報の一覧を取得します")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "返品一覧の取得成功"),
        @ApiResponse(responseCode = "403", description = "アクセス権限がありません")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER_SERVICE') or hasRole('RETURN_PROCESSOR')")
    public ResponseEntity<ReturnListResponse> getReturns(
            @PageableDefault(size = 20) Pageable pageable,
            @Parameter(description = "返品ステータスフィルター") @RequestParam(required = false) String status,
            @Parameter(description = "開始日") @RequestParam(required = false) String fromDate,
            @Parameter(description = "終了日") @RequestParam(required = false) String toDate) {
        
        log.info("Getting returns with status: {}, page: {}", status, pageable);
        ReturnListResponse response = returnService.getReturns(pageable, status, fromDate, toDate);
        return ResponseEntity.ok(response);
    }

    /**
     * 返品詳細取得
     */
    @GetMapping("/{id}")
    @Operation(summary = "返品詳細取得", description = "指定IDの返品詳細情報を取得します")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "返品詳細の取得成功"),
        @ApiResponse(responseCode = "404", description = "返品情報が見つかりません"),
        @ApiResponse(responseCode = "403", description = "アクセス権限がありません")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER_SERVICE') or hasRole('RETURN_PROCESSOR')")
    public ResponseEntity<ReturnDetailResponse> getReturnDetail(
            @Parameter(description = "返品ID") @PathVariable Long id) {
        
        log.info("Getting return detail for id: {}", id);
        ReturnDetailResponse response = returnService.getReturnDetail(id);
        return ResponseEntity.ok(response);
    }

    /**
     * 返品申請作成
     */
    @PostMapping
    @Operation(summary = "返品申請作成", description = "新しい返品申請を作成します")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "返品申請の作成成功"),
        @ApiResponse(responseCode = "400", description = "リクエスト内容が不正です"),
        @ApiResponse(responseCode = "403", description = "アクセス権限がありません")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER_SERVICE') or hasRole('USER')")
    public ResponseEntity<ReturnDetailResponse> createReturn(
            @Parameter(description = "返品作成リクエスト") @Valid @RequestBody ReturnCreateRequest request) {
        
        log.info("Creating return for order: {}", request.getOrderId());
        ReturnDetailResponse response = returnService.createReturn(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 返品ステータス更新
     */
    @PutMapping("/{id}/status")
    @Operation(summary = "返品ステータス更新", description = "返品のステータスを更新します")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "ステータス更新成功"),
        @ApiResponse(responseCode = "404", description = "返品情報が見つかりません"),
        @ApiResponse(responseCode = "400", description = "リクエスト内容が不正です"),
        @ApiResponse(responseCode = "403", description = "アクセス権限がありません")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER_SERVICE') or hasRole('RETURN_PROCESSOR')")
    public ResponseEntity<ReturnResponse> updateReturnStatus(
            @Parameter(description = "返品ID") @PathVariable Long id,
            @Parameter(description = "ステータス更新リクエスト") @Valid @RequestBody ReturnStatusUpdateRequest request) {
        
        log.info("Updating return status for id: {}, status: {}", id, request.status());
        ReturnResponse response = returnService.updateReturnStatus(id.toString(), request);
        return ResponseEntity.ok(response);
    }

    /**
     * 注文返品情報取得
     */
    @GetMapping("/order/{orderId}")
    @Operation(summary = "注文返品情報取得", description = "指定注文IDの返品情報を取得します")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "返品情報の取得成功"),
        @ApiResponse(responseCode = "404", description = "注文が見つかりません"),
        @ApiResponse(responseCode = "403", description = "アクセス権限がありません")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER_SERVICE') or hasRole('RETURN_PROCESSOR')")
    public ResponseEntity<ReturnListResponse> getReturnsByOrder(
            @Parameter(description = "注文ID") @PathVariable Long orderId) {
        
        log.info("Getting returns for order: {}", orderId);
        ReturnListResponse response = returnService.getReturnsByOrder(orderId);
        return ResponseEntity.ok(response);
    }
}
