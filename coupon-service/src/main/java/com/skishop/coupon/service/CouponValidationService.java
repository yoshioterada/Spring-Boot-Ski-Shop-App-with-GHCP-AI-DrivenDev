package com.skishop.coupon.service;

import com.skishop.coupon.entity.Coupon;
import com.skishop.coupon.exception.CouponException;
import com.skishop.coupon.repository.CouponUsageRepository;
import com.skishop.coupon.repository.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponValidationService {

    private final CouponUsageRepository couponUsageRepository;
    private final UserCouponRepository userCouponRepository;

    public void validateCoupon(Coupon coupon, UUID userId, BigDecimal cartAmount) {
        log.debug("Validating coupon: {} for user: {}", coupon.getCode(), userId);

        // 基本バリデーション
        validateBasicCouponConditions(coupon);
        
        // 最小購入金額チェック
        validateMinimumPurchaseAmount(coupon, cartAmount);
        
        // ユーザー使用回数制限チェック
        validateUserUsageLimit(coupon, userId);
        
        // ユーザーに割り当てられているかチェック（必要に応じて）
        validateUserCouponAssignment(coupon, userId);

        log.debug("Coupon validation passed for: {}", coupon.getCode());
    }

    public void validateCouponForRedemption(Coupon coupon, UUID userId, BigDecimal orderAmount, UUID orderId) {
        log.debug("Validating coupon for redemption: {} for user: {}", coupon.getCode(), userId);

        // 基本バリデーション
        validateCoupon(coupon, userId, orderAmount);
        
        // 同一注文での重複使用チェック
        validateOrderDuplication(coupon, orderId);

        log.debug("Coupon redemption validation passed for: {}", coupon.getCode());
    }

    private void validateBasicCouponConditions(Coupon coupon) {
        // アクティブ状態チェック
        if (!Boolean.TRUE.equals(coupon.getIsActive())) {
            throw new CouponException("Coupon is not active: " + coupon.getCode());
        }

        // 有効期限チェック
        if (coupon.isExpired()) {
            throw new CouponException("Coupon has expired: " + coupon.getCode());
        }

        // 使用回数上限チェック
        if (coupon.isExhausted()) {
            throw new CouponException("Coupon usage limit has been reached: " + coupon.getCode());
        }

        // キャンペーンの有効性チェック
        if (coupon.getCampaign() == null || !coupon.getCampaign().isActive()) {
            throw new CouponException("Associated campaign is not active: " + coupon.getCode());
        }
    }

    private void validateMinimumPurchaseAmount(Coupon coupon, BigDecimal cartAmount) {
        if (coupon.getMinimumAmount() != null && 
            cartAmount.compareTo(coupon.getMinimumAmount()) < 0) {
            throw new CouponException(
                String.format("Minimum purchase amount not met. Required: %s, Current: %s", 
                    coupon.getMinimumAmount(), cartAmount));
        }
    }

    private void validateUserUsageLimit(Coupon coupon, UUID userId) {
        // ユーザーごとの使用回数制限（通常は1回）
        long userUsageCount = couponUsageRepository.countByCouponIdAndUserId(coupon.getId(), userId);
        
        if (userUsageCount > 0) {
            throw new CouponException("User has already used this coupon: " + coupon.getCode());
        }
    }

    private void validateUserCouponAssignment(Coupon coupon, UUID userId) {
        // ユーザーに特別に割り当てられたクーポンの場合のチェック
        boolean hasAssignment = userCouponRepository.existsByUserIdAndCouponId(userId, coupon.getId());
        
        // 公開クーポンの場合は割り当てチェックをスキップ
        // この判定は campaign.rules や coupon.couponType で行う
        if (!hasAssignment && isUserSpecificCoupon(coupon)) {
            throw new CouponException("Coupon is not assigned to this user: " + coupon.getCode());
        }
    }

    private void validateOrderDuplication(Coupon coupon, UUID orderId) {
        if (couponUsageRepository.existsByCouponIdAndOrderId(coupon.getId(), orderId)) {
            throw new CouponException("Coupon has already been used for this order: " + coupon.getCode());
        }
    }

    private boolean isUserSpecificCoupon(Coupon coupon) {
        // キャンペーンのルールに基づいてユーザー固有クーポンかどうかを判定
        // 簡略化のため、ここでは常にfalseを返す（公開クーポンとして扱う）
        return false;
    }

    public void validateCouponCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new CouponException("Coupon code cannot be empty");
        }
        
        if (code.length() < 3 || code.length() > 50) {
            throw new CouponException("Coupon code must be between 3 and 50 characters");
        }
        
        // 英数字のみ許可
        if (!code.matches("^[A-Za-z0-9\\-_]+$")) {
            throw new CouponException("Coupon code can only contain letters, numbers, hyphens, and underscores");
        }
    }

    public void validateTimeConstraints(Coupon coupon) {
        LocalDateTime now = LocalDateTime.now();
        
        if (coupon.getExpiresAt() != null && now.isAfter(coupon.getExpiresAt())) {
            throw new CouponException("Coupon has expired");
        }
        
        if (coupon.getCampaign() != null) {
            LocalDateTime campaignStart = coupon.getCampaign().getStartDate();
            LocalDateTime campaignEnd = coupon.getCampaign().getEndDate();
            
            if (campaignStart != null && now.isBefore(campaignStart)) {
                throw new CouponException("Campaign has not started yet");
            }
            
            if (campaignEnd != null && now.isAfter(campaignEnd)) {
                throw new CouponException("Campaign has ended");
            }
        }
    }
}
