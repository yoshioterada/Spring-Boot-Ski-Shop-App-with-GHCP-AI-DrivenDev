package com.skishop.inventory.controller;

import com.skishop.inventory.entity.jpa.Inventory;
import com.skishop.inventory.service.InventoryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

/**
 * 在庫API コントローラー
 */
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Validated
@Slf4j
public class InventoryController {

    private final InventoryService inventoryService;

    /**
     * 商品の在庫情報取得
     */
    @GetMapping("/{productId}")
    public ResponseEntity<Inventory> getInventory(@PathVariable String productId) {
        log.info("在庫情報取得リクエスト - 商品ID: {}", productId);
        
        Inventory inventory = inventoryService.findByProductId(productId);
        return ResponseEntity.ok(inventory);
    }

    /**
     * 商品の在庫ステータス取得
     */
    @GetMapping("/status/{productId}")
    public ResponseEntity<InventoryStatusResponse> getInventoryStatus(@PathVariable String productId) {
        log.info("在庫ステータス取得リクエスト - 商品ID: {}", productId);
        
        Inventory inventory = inventoryService.findByProductId(productId);
        Integer availableQuantity = inventoryService.getAvailableQuantity(productId);
        
        InventoryStatusResponse response = InventoryStatusResponse.of(
            productId, 
            inventory.getStatus().name(), 
            inventory.getQuantity(), 
            inventory.getReservedQuantity(), 
            availableQuantity
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * 複数商品の在庫情報一括取得
     */
    @PostMapping("/batch")
    public ResponseEntity<Map<String, Inventory>> getInventoryBatch(@RequestBody List<String> productIds) {
        log.info("複数在庫情報取得リクエスト - 商品IDs: {}", productIds);
        
        Map<String, Inventory> inventories = inventoryService.findByProductIds(productIds);
        return ResponseEntity.ok(inventories);
    }

    /**
     * 在庫予約
     */
    @PostMapping("/reserve")
    public ResponseEntity<String> reserveStock(@Valid @RequestBody StockReserveRequest request) {
        log.info("在庫予約リクエスト - 商品ID: {}, 数量: {}", request.productId(), request.quantity());
        
        inventoryService.reserveStock(request.productId(), request.quantity());
        return ResponseEntity.ok("在庫予約が完了しました");
    }

    /**
     * 在庫予約解除
     */
    @PostMapping("/release")
    public ResponseEntity<String> releaseStock(@Valid @RequestBody StockReleaseRequest request) {
        log.info("在庫予約解除リクエスト - 商品ID: {}, 数量: {}", request.productId(), request.quantity());
        
        inventoryService.releaseStock(request.productId(), request.quantity());
        return ResponseEntity.ok("在庫予約解除が完了しました");
    }

    /**
     * 入荷処理
     */
    @PostMapping("/stock-in")
    public ResponseEntity<String> stockIn(@Valid @RequestBody StockInRequest request) {
        log.info("入荷処理リクエスト - 商品ID: {}, 数量: {}", request.productId(), request.quantity());
        
        inventoryService.stockIn(request.productId(), request.quantity());
        return ResponseEntity.ok("入荷処理が完了しました");
    }

    /**
     * 出荷処理
     */
    @PostMapping("/stock-out")
    public ResponseEntity<String> stockOut(@Valid @RequestBody StockOutRequest request) {
        log.info("出荷処理リクエスト - 商品ID: {}, 数量: {}", request.productId(), request.quantity());
        
        inventoryService.stockOut(request.productId(), request.quantity());
        return ResponseEntity.ok("出荷処理が完了しました");
    }

    /**
     * 在庫不足商品一覧取得
     */
    @GetMapping("/low-stock")
    public ResponseEntity<List<Inventory>> getLowStockItems(
            @RequestParam(defaultValue = "5") @Min(0) Integer threshold) {
        log.info("在庫不足商品取得リクエスト - 閾値: {}", threshold);
        
        List<Inventory> lowStockItems = inventoryService.findLowStockItems(threshold);
        return ResponseEntity.ok(lowStockItems);
    }

    // リクエスト・レスポンスクラス
    public record StockReserveRequest(
        @NotBlank String productId,
        @Min(1) Integer quantity
    ) {}

    public record StockReleaseRequest(
        @NotBlank String productId,
        @Min(1) Integer quantity
    ) {}

    public record StockInRequest(
        @NotBlank String productId,
        @Min(1) Integer quantity
    ) {}

    public record StockOutRequest(
        @NotBlank String productId,
        @Min(1) Integer quantity
    ) {}

    public record InventoryStatusResponse(
        String productId,
        String status,
        Integer quantity,
        Integer reservedQuantity,
        Integer availableQuantity,
        Boolean inStock
    ) {
        // Java 21のrecordを活用したビルダーパターン代替
        public static InventoryStatusResponse of(String productId, String status, Integer quantity, 
                                                 Integer reservedQuantity, Integer availableQuantity) {
            return new InventoryStatusResponse(
                productId, 
                status, 
                quantity, 
                reservedQuantity, 
                availableQuantity, 
                availableQuantity > 0
            );
        }
    }
}
