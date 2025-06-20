package com.skishop.coupon.controller;

import com.skishop.coupon.dto.CouponDto;
import com.skishop.coupon.service.CouponService;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Coupon Management", description = "クーポン管理API")
public class CouponController {

    private final CouponService couponService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "クーポン作成", description = "新しいクーポンを作成します")
    public ResponseEntity<Map<String, Object>> createCoupon(
            @Valid @RequestBody CouponDto.CouponRequest request) {
        
        log.info("Creating coupon with code: {}", request.getCode());
        CouponDto.CouponResponse response = couponService.createCoupon(request);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "success", true,
            "data", response
        ));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "クーポン一覧取得", description = "キャンペーンIDによるクーポン一覧を取得します")
    public ResponseEntity<Map<String, Object>> getCoupons(
            @Parameter(description = "キャンペーンID") @RequestParam UUID campaignId,
            @PageableDefault(size = 20) Pageable pageable) {
        
        Page<CouponDto.CouponResponse> coupons = couponService.getCouponsByCampaign(campaignId, pageable);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", coupons.getContent(),
            "pagination", Map.of(
                "page", coupons.getNumber(),
                "size", coupons.getSize(),
                "totalElements", coupons.getTotalElements(),
                "totalPages", coupons.getTotalPages()
            )
        ));
    }

    @PostMapping("/validate")
    @Operation(summary = "クーポン検証", description = "クーポンの有効性を検証します")
    public ResponseEntity<Map<String, Object>> validateCoupon(
            @Valid @RequestBody CouponDto.CouponValidationRequest request,
            Principal principal) {
        
        UUID userId = UUID.fromString(principal.getName());
        CouponDto.CouponValidationResponse response = couponService.validateCoupon(request, userId);
        
        return ResponseEntity.ok(Map.of(
            "success", response.getIsValid(),
            "data", response
        ));
    }

    @PostMapping("/redeem")
    @Operation(summary = "クーポン使用", description = "クーポンを使用して割引を適用します")
    public ResponseEntity<Map<String, Object>> redeemCoupon(
            @Valid @RequestBody CouponDto.CouponRedeemRequest request,
            Principal principal) {
        
        UUID userId = UUID.fromString(principal.getName());
        CouponDto.CouponRedeemResponse response = couponService.redeemCoupon(request, userId);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", response
        ));
    }

    @GetMapping("/usage/{couponId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "クーポン使用状況取得", description = "指定クーポンの使用状況を取得します")
    public ResponseEntity<Map<String, Object>> getCouponUsage(
            @Parameter(description = "クーポンID") @PathVariable UUID couponId,
            @PageableDefault(size = 20) Pageable pageable) {
        
        Page<CouponDto.CouponResponse> usage = couponService.getCouponUsage(couponId, pageable);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", usage.getContent(),
            "pagination", Map.of(
                "page", usage.getNumber(),
                "size", usage.getSize(),
                "totalElements", usage.getTotalElements(),
                "totalPages", usage.getTotalPages()
            )
        ));
    }

    @PostMapping("/bulk-generate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "一括クーポン生成", description = "指定された数のクーポンを一括生成します")
    public ResponseEntity<Map<String, Object>> bulkGenerateCoupons(
            @Valid @RequestBody CouponDto.BulkCouponRequest request) {
        
        log.info("Bulk generating {} coupons for campaign: {}", request.getCount(), request.getCampaignId());
        CouponDto.BulkCouponResponse response = couponService.generateBulkCoupons(request);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "success", true,
            "data", response
        ));
    }

    @GetMapping("/user/available")
    @Operation(summary = "利用可能クーポン取得", description = "ユーザーが利用可能なクーポン一覧を取得します")
    public ResponseEntity<Map<String, Object>> getAvailableCoupons(Principal principal) {
        
        UUID userId = UUID.fromString(principal.getName());
        List<CouponDto.CouponResponse> coupons = couponService.getAvailableCouponsByUser(userId);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", coupons
        ));
    }

    @GetMapping("/{code}")
    @Operation(summary = "クーポン詳細取得", description = "クーポンコードで詳細情報を取得します")
    public ResponseEntity<Map<String, Object>> getCouponByCode(
            @Parameter(description = "クーポンコード") @PathVariable String code) {
        
        CouponDto.CouponResponse coupon = couponService.getCouponByCode(code);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", coupon
        ));
    }
}
