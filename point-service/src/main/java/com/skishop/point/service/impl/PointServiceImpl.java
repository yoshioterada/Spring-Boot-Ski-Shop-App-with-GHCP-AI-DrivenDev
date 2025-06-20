package com.skishop.point.service.impl;

import com.skishop.point.dto.*;
import com.skishop.point.entity.PointExpiry;
import com.skishop.point.entity.PointRedemption;
import com.skishop.point.entity.PointTransaction;
import com.skishop.point.entity.UserTier;
import com.skishop.point.repository.*;
import com.skishop.point.service.PointService;
import com.skishop.point.service.TierService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PointServiceImpl implements PointService {
    
    private final PointTransactionRepository pointTransactionRepository;
    private final PointRedemptionRepository pointRedemptionRepository;
    private final PointExpiryRepository pointExpiryRepository;
    private final UserTierRepository userTierRepository;
    private final TierService tierService;
    
    @Override
    public PointTransactionDto awardPoints(PointAwardRequest request) {
        log.info("Awarding {} points to user {}", request.getAmount(), request.getUserId());
        
        // Get current balance
        Integer currentBalance = getCurrentBalance(request.getUserId());
        
        // Create point transaction
        PointTransaction transaction = PointTransaction.builder()
                .userId(request.getUserId())
                .transactionType(PointTransaction.TransactionType.EARNED)
                .amount(request.getAmount())
                .balanceAfter(currentBalance + request.getAmount())
                .reason(request.getReason())
                .referenceId(request.getReferenceId())
                .isExpired(false)
                .build();
        
        // Set expiry date if specified
        if (request.getExpiryDays() != null && request.getExpiryDays() > 0) {
            transaction.setExpiresAt(LocalDateTime.now().plusDays(request.getExpiryDays()));
            
            // Schedule expiry
            PointExpiry expiry = PointExpiry.builder()
                    .transactionId(transaction.getId())
                    .expiredPoints(request.getAmount())
                    .scheduledAt(transaction.getExpiresAt())
                    .status("scheduled")
                    .build();
            pointExpiryRepository.save(expiry);
        }
        
        PointTransaction savedTransaction = pointTransactionRepository.save(transaction);
        
        // Update user tier
        tierService.updateUserTier(request.getUserId(), getCurrentTotalEarned(request.getUserId()));
        
        return mapToDto(savedTransaction);
    }
    
    @Override
    @Transactional(readOnly = true)
    public PointBalanceResponse getPointBalance(UUID userId) {
        Integer totalEarned = pointTransactionRepository.getEarnedBalance(userId);
        Integer totalRedeemed = pointTransactionRepository.getRedeemedBalance(userId);
        
        if (totalEarned == null) totalEarned = 0;
        if (totalRedeemed == null) totalRedeemed = 0;
        
        Integer currentBalance = totalEarned - Math.abs(totalRedeemed);
        
        // Get expiring points in next 30 days
        LocalDateTime thirtyDaysFromNow = LocalDateTime.now().plusDays(30);
        List<PointTransaction> expiringTransactions = 
                pointTransactionRepository.findExpiringTransactionsByUser(userId, thirtyDaysFromNow);
        Integer expiringPoints = expiringTransactions.stream()
                .mapToInt(PointTransaction::getAmount)
                .sum();
        
        // Get user tier info
        UserTier userTier = userTierRepository.findByUserId(userId).orElse(null);
        String tierLevel = userTier != null ? userTier.getTierLevel() : "bronze";
        String tierName = tierLevel.substring(0, 1).toUpperCase() + tierLevel.substring(1);
        
        return new PointBalanceResponse(
                userId,
                totalEarned,
                Math.abs(totalRedeemed),
                currentBalance,
                expiringPoints,
                tierLevel,
                tierName
        );
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<PointTransactionDto> getPointHistory(UUID userId) {
        List<PointTransaction> transactions = pointTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return transactions.stream()
                .map(this::mapToDto)
                .toList();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<PointTransactionDto> getPointHistoryByDateRange(UUID userId, LocalDateTime startDate, LocalDateTime endDate) {
        List<PointTransaction> transactions = pointTransactionRepository
                .findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(userId, startDate, endDate);
        return transactions.stream()
                .map(this::mapToDto)
                .toList();
    }
    
    @Override
    public void redeemPoints(PointRedemptionRequest request) {
        log.info("Redeeming {} points for user {}", request.getPointsToRedeem(), request.getUserId());
        
        // Check if user has enough points
        Integer currentBalance = getCurrentBalance(request.getUserId());
        if (currentBalance < request.getPointsToRedeem()) {
            throw new IllegalArgumentException("Insufficient points balance");
        }
        
        // Create redemption transaction
        PointTransaction transaction = PointTransaction.builder()
                .userId(request.getUserId())
                .transactionType(PointTransaction.TransactionType.REDEEMED)
                .amount(-request.getPointsToRedeem())
                .balanceAfter(currentBalance - request.getPointsToRedeem())
                .reason("Point redemption: " + request.getRedemptionType())
                .isExpired(false)
                .build();
        
        PointTransaction savedTransaction = pointTransactionRepository.save(transaction);
        
        // Create redemption record
        BigDecimal valueRedeemed = calculateRedemptionValue(request.getPointsToRedeem(), request.getRedemptionType());
        
        PointRedemption redemption = PointRedemption.builder()
                .userId(request.getUserId())
                .transactionId(savedTransaction.getId())
                .redemptionType(request.getRedemptionType())
                .pointsUsed(request.getPointsToRedeem())
                .valueRedeemed(valueRedeemed)
                .status("completed")
                .details(request.getDetails())
                .build();
        
        pointRedemptionRepository.save(redemption);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<PointTransactionDto> getExpiringPoints(UUID userId, int days) {
        LocalDateTime expiryDate = LocalDateTime.now().plusDays(days);
        List<PointTransaction> expiringTransactions = 
                pointTransactionRepository.findExpiringTransactionsByUser(userId, expiryDate);
        
        return expiringTransactions.stream()
                .map(this::mapToDto)
                .toList();
    }
    
    @Override
    public void processExpiredPoints() {
        log.info("Processing expired points");
        
        List<PointExpiry> scheduledExpiries = pointExpiryRepository
                .findScheduledExpiriesByDate(LocalDateTime.now());
        
        for (PointExpiry expiry : scheduledExpiries) {
            // Mark points as expired
            PointTransaction transaction = pointTransactionRepository.findById(expiry.getTransactionId())
                    .orElse(null);
            
            if (transaction != null && !transaction.getIsExpired()) {
                transaction.setIsExpired(true);
                pointTransactionRepository.save(transaction);
                
                expiry.setStatus("processed");
                expiry.setProcessedAt(LocalDateTime.now());
                pointExpiryRepository.save(expiry);
                
                log.info("Expired {} points for user {}", expiry.getExpiredPoints(), transaction.getUserId());
            }
        }
    }
    
    @Override
    public void transferPoints(UUID fromUserId, UUID toUserId, Integer amount, String reason) {
        log.info("Transferring {} points from user {} to user {}", amount, fromUserId, toUserId);
        
        // Check if sender has enough points
        Integer fromBalance = getCurrentBalance(fromUserId);
        if (fromBalance < amount) {
            throw new IllegalArgumentException("Insufficient points balance for transfer");
        }
        
        // Deduct from sender
        PointTransaction deductTransaction = PointTransaction.builder()
                .userId(fromUserId)
                .transactionType(PointTransaction.TransactionType.TRANSFERRED_OUT)
                .amount(-amount)
                .balanceAfter(fromBalance - amount)
                .reason("Point transfer to user: " + toUserId + " - " + reason)
                .referenceId("transfer_" + UUID.randomUUID().toString())
                .isExpired(false)
                .build();
        pointTransactionRepository.save(deductTransaction);
        
        // Add to receiver
        Integer toBalance = getCurrentBalance(toUserId);
        PointTransaction addTransaction = PointTransaction.builder()
                .userId(toUserId)
                .transactionType(PointTransaction.TransactionType.TRANSFERRED_IN)
                .amount(amount)
                .balanceAfter(toBalance + amount)
                .reason("Point transfer from user: " + fromUserId + " - " + reason)
                .referenceId("transfer_" + deductTransaction.getReferenceId())
                .isExpired(false)
                .build();
        pointTransactionRepository.save(addTransaction);
        
        // Update tiers for both users
        tierService.updateUserTier(fromUserId, getCurrentTotalEarned(fromUserId));
        tierService.updateUserTier(toUserId, getCurrentTotalEarned(toUserId));
    }
    
    @Override
    public List<RedemptionOptionDto> getRedemptionOptions(UUID userId) {
        log.info("Getting redemption options for user: {}", userId);
        
        // モックデータを返す
        RedemptionOptionDto discountOption = new RedemptionOptionDto();
        discountOption.setId(UUID.randomUUID());
        discountOption.setType("discount");
        discountOption.setName("10% Discount");
        discountOption.setDescription("Get 10% off your next purchase");
        discountOption.setPointCost(100);
        discountOption.setValue(10);
        discountOption.setCategory("discount");
        discountOption.setAvailable(true);
        discountOption.setMaxRedemptions(1);
        discountOption.setTerms("Valid for 30 days");
        
        RedemptionOptionDto cashbackOption = new RedemptionOptionDto();
        cashbackOption.setId(UUID.randomUUID());
        cashbackOption.setType("cashback");
        cashbackOption.setName("$5 Cashback");
        cashbackOption.setDescription("Get $5 cashback to your account");
        cashbackOption.setPointCost(500);
        cashbackOption.setValue(500);
        cashbackOption.setCategory("cashback");
        cashbackOption.setAvailable(true);
        cashbackOption.setMaxRedemptions(1);
        cashbackOption.setTerms("Valid for 30 days");
        
        return List.of(discountOption, cashbackOption);
    }
    
    private Integer getCurrentBalance(UUID userId) {
        Integer earned = pointTransactionRepository.getEarnedBalance(userId);
        Integer redeemed = pointTransactionRepository.getRedeemedBalance(userId);
        
        if (earned == null) earned = 0;
        if (redeemed == null) redeemed = 0;
        
        return earned - Math.abs(redeemed);
    }
    
    private Integer getCurrentTotalEarned(UUID userId) {
        Integer earned = pointTransactionRepository.getEarnedBalance(userId);
        return earned != null ? earned : 0;
    }
    
    private BigDecimal calculateRedemptionValue(Integer points, String redemptionType) {
        // Simple calculation - could be more complex based on business rules
        final BigDecimal pointValue = new BigDecimal("0.01"); // 1 point = $0.01
        final BigDecimal pointsDecimal = new BigDecimal(points);
        
        return switch (redemptionType.toLowerCase()) {
            case "discount" -> pointsDecimal.multiply(pointValue);
            case "cashback" -> pointsDecimal.multiply(pointValue.multiply(new BigDecimal("0.8")));
            case "product" -> pointsDecimal.multiply(pointValue.multiply(new BigDecimal("1.2")));
            default -> pointsDecimal.multiply(pointValue);
        };
    }
    
    private PointTransactionDto mapToDto(PointTransaction transaction) {
        return new PointTransactionDto(
                transaction.getId(),
                transaction.getUserId(),
                transaction.getTransactionType().name().toLowerCase(),
                transaction.getAmount(),
                transaction.getBalanceAfter(),
                transaction.getReason(),
                transaction.getReferenceId(),
                transaction.getExpiresAt(),
                transaction.getIsExpired(),
                transaction.getCreatedAt()
        );
    }

    @Override
    public PointRedemptionResponse redeemPointsV2(PointRedemptionRequest request) {
        log.info("Redeeming {} points for user {} (v2)", request.getPointsToRedeem(), request.getUserId());
        
        // 現在残高を取得
        Integer currentBalance = getCurrentBalance(request.getUserId());
        
        // 残高チェック
        if (currentBalance < request.getPointsToRedeem()) {
            throw new RuntimeException("Insufficient points. Current balance: " + currentBalance);
        }
        
        // ポイント使用処理のモック実装
        Integer balanceAfter = currentBalance - request.getPointsToRedeem();
        
        // レスポンスを作成
        return PointRedemptionResponse.builder()
                .redemptionId(UUID.randomUUID())
                .pointsUsed(request.getPointsToRedeem())
                .valueRedeemed(request.getPointsToRedeem() * 10) // 仮の換算レート
                .balanceAfter(balanceAfter)
                .redemptionType(request.getRedemptionType())
                .redeemedAt(LocalDateTime.now())
                .details(request.getDetails())
                .build();
    }
}
