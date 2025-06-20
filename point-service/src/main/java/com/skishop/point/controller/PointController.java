package com.skishop.point.controller;

import com.skishop.point.dto.*;
import com.skishop.point.service.PointService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/points")
@RequiredArgsConstructor
@Tag(name = "Point Management", description = "Point earning, redemption and balance management")
public class PointController {
    
    private final PointService pointService;
    
    @PostMapping("/award")
    @Operation(summary = "Award points to user", description = "Awards points to a user for various activities")
    public ResponseEntity<PointTransactionDto> awardPoints(@Valid @RequestBody PointAwardRequest request) {
        PointTransactionDto transaction = pointService.awardPoints(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(transaction);
    }
    
    @GetMapping("/balance/{userId}")
    @Operation(summary = "Get point balance", description = "Retrieves the current point balance and tier information for a user")
    public ResponseEntity<PointBalanceResponse> getPointBalance(@PathVariable UUID userId) {
        PointBalanceResponse balance = pointService.getPointBalance(userId);
        return ResponseEntity.ok(balance);
    }
    
    @GetMapping("/history/{userId}")
    @Operation(summary = "Get point history", description = "Retrieves the complete point transaction history for a user")
    public ResponseEntity<List<PointTransactionDto>> getPointHistory(@PathVariable UUID userId) {
        List<PointTransactionDto> history = pointService.getPointHistory(userId);
        return ResponseEntity.ok(history);
    }
    
    @GetMapping("/history/{userId}/range")
    @Operation(summary = "Get point history by date range", description = "Retrieves point transaction history for a specific date range")
    public ResponseEntity<List<PointTransactionDto>> getPointHistoryByDateRange(
            @PathVariable UUID userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        List<PointTransactionDto> history = pointService.getPointHistoryByDateRange(userId, startDate, endDate);
        return ResponseEntity.ok(history);
    }
    
    @PostMapping("/redeem")
    @Operation(summary = "Redeem points", description = "Redeems points for discounts, products, or cashback")
    public ResponseEntity<Void> redeemPoints(@Valid @RequestBody PointRedemptionRequest request) {
        pointService.redeemPoints(request);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/expiring/{userId}")
    @Operation(summary = "Get expiring points", description = "Retrieves points that will expire within specified days")
    public ResponseEntity<List<PointTransactionDto>> getExpiringPoints(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "30") int days) {
        List<PointTransactionDto> expiringPoints = pointService.getExpiringPoints(userId, days);
        return ResponseEntity.ok(expiringPoints);
    }
    
    @PostMapping("/transfer")
    @Operation(summary = "Transfer points", description = "Transfers points between users")
    public ResponseEntity<Void> transferPoints(
            @RequestParam UUID fromUserId,
            @RequestParam UUID toUserId,
            @RequestParam Integer amount,
            @RequestParam String reason) {
        pointService.transferPoints(fromUserId, toUserId, amount, reason);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/process-expired")
    @Operation(summary = "Process expired points", description = "Process all expired points (admin operation)")
    public ResponseEntity<Void> processExpiredPoints() {
        pointService.processExpiredPoints();
        return ResponseEntity.ok().build();
    }
}
