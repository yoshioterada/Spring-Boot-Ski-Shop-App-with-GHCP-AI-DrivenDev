package com.skishop.coupon.repository;

import com.skishop.coupon.entity.UserCoupon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserCouponRepository extends JpaRepository<UserCoupon, UUID> {

    @Query("SELECT uc FROM UserCoupon uc WHERE uc.userId = :userId")
    Page<UserCoupon> findByUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT uc FROM UserCoupon uc WHERE uc.userId = :userId AND uc.isRedeemed = false")
    List<UserCoupon> findAvailableByUserId(@Param("userId") UUID userId);

    @Query("SELECT uc FROM UserCoupon uc WHERE uc.coupon.id = :couponId")
    List<UserCoupon> findByCouponId(@Param("couponId") UUID couponId);

    @Query("SELECT uc FROM UserCoupon uc WHERE uc.userId = :userId AND uc.coupon.id = :couponId")
    Optional<UserCoupon> findByUserIdAndCouponId(@Param("userId") UUID userId, 
                                                @Param("couponId") UUID couponId);

    @Query("SELECT uc FROM UserCoupon uc WHERE uc.userId = :userId AND uc.isRedeemed = :isRedeemed")
    List<UserCoupon> findByUserIdAndIsRedeemed(@Param("userId") UUID userId, 
                                              @Param("isRedeemed") Boolean isRedeemed);

    @Query("SELECT uc FROM UserCoupon uc WHERE uc.coupon.campaign.id = :campaignId")
    List<UserCoupon> findByCampaignId(@Param("campaignId") UUID campaignId);

    @Query("SELECT uc FROM UserCoupon uc WHERE uc.assignedAt BETWEEN :startDate AND :endDate")
    List<UserCoupon> findByAssignedAtBetween(@Param("startDate") LocalDateTime startDate, 
                                           @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(uc) FROM UserCoupon uc WHERE uc.userId = :userId AND uc.isRedeemed = false")
    long countAvailableByUserId(@Param("userId") UUID userId);

    @Query("SELECT COUNT(uc) FROM UserCoupon uc WHERE uc.coupon.campaign.id = :campaignId")
    long countByCampaignId(@Param("campaignId") UUID campaignId);

    @Query("SELECT COUNT(uc) FROM UserCoupon uc WHERE uc.coupon.campaign.id = :campaignId AND uc.isRedeemed = true")
    long countRedeemedByCampaignId(@Param("campaignId") UUID campaignId);

    boolean existsByUserIdAndCouponId(UUID userId, UUID couponId);
}
