package com.skishop.coupon.repository;

import com.skishop.coupon.entity.CouponUsage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface CouponUsageRepository extends JpaRepository<CouponUsage, UUID> {

    @Query("SELECT cu FROM CouponUsage cu WHERE cu.coupon.id = :couponId")
    Page<CouponUsage> findByCouponId(@Param("couponId") UUID couponId, Pageable pageable);

    @Query("SELECT cu FROM CouponUsage cu WHERE cu.userId = :userId")
    Page<CouponUsage> findByUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT cu FROM CouponUsage cu WHERE cu.coupon.id = :couponId AND cu.userId = :userId")
    List<CouponUsage> findByCouponIdAndUserId(@Param("couponId") UUID couponId, 
                                             @Param("userId") UUID userId);

    @Query("SELECT COUNT(cu) FROM CouponUsage cu WHERE cu.coupon.id = :couponId AND cu.userId = :userId")
    long countByCouponIdAndUserId(@Param("couponId") UUID couponId, @Param("userId") UUID userId);

    @Query("SELECT cu FROM CouponUsage cu WHERE cu.orderId = :orderId")
    List<CouponUsage> findByOrderId(@Param("orderId") UUID orderId);

    @Query("SELECT cu FROM CouponUsage cu WHERE cu.usedAt BETWEEN :startDate AND :endDate")
    List<CouponUsage> findByUsedAtBetween(@Param("startDate") LocalDateTime startDate, 
                                         @Param("endDate") LocalDateTime endDate);

    @Query("SELECT SUM(cu.discountApplied) FROM CouponUsage cu WHERE cu.coupon.id = :couponId")
    Double getTotalDiscountByCouponId(@Param("couponId") UUID couponId);

    @Query("SELECT COUNT(cu) FROM CouponUsage cu WHERE cu.coupon.campaign.id = :campaignId")
    long countByCampaignId(@Param("campaignId") UUID campaignId);

    @Query("SELECT SUM(cu.discountApplied) FROM CouponUsage cu WHERE cu.coupon.campaign.id = :campaignId")
    Double getTotalDiscountByCampaignId(@Param("campaignId") UUID campaignId);

    boolean existsByCouponIdAndOrderId(UUID couponId, UUID orderId);
}
