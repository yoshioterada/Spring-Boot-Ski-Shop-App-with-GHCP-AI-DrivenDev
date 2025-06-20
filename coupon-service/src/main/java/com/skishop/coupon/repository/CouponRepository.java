package com.skishop.coupon.repository;

import com.skishop.coupon.entity.Coupon;
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
public interface CouponRepository extends JpaRepository<Coupon, UUID> {

    Optional<Coupon> findByCode(String code);

    @Query("SELECT c FROM Coupon c WHERE c.code = :code AND c.isActive = true")
    Optional<Coupon> findActiveByCode(@Param("code") String code);

    @Query("SELECT c FROM Coupon c WHERE c.campaign.id = :campaignId")
    Page<Coupon> findByCampaignId(@Param("campaignId") UUID campaignId, Pageable pageable);

    @Query("SELECT c FROM Coupon c WHERE c.campaign.id = :campaignId AND c.isActive = true")
    List<Coupon> findActiveByCampaignId(@Param("campaignId") UUID campaignId);

    @Query("SELECT c FROM Coupon c WHERE c.isActive = true AND c.expiresAt > :now")
    List<Coupon> findActiveAndNotExpired(@Param("now") LocalDateTime now);

    @Query("SELECT c FROM Coupon c WHERE c.expiresAt <= :now AND c.isActive = true")
    List<Coupon> findExpiredActiveCoupons(@Param("now") LocalDateTime now);

    @Query("SELECT c FROM Coupon c WHERE c.usedCount >= c.usageLimit AND c.isActive = true")
    List<Coupon> findExhaustedActiveCoupons();

    @Query("SELECT c FROM Coupon c JOIN c.userCoupons uc WHERE uc.userId = :userId AND uc.isRedeemed = false")
    List<Coupon> findAvailableByUser(@Param("userId") UUID userId);

    @Query("SELECT c FROM Coupon c WHERE c.couponType = :couponType AND c.isActive = true")
    List<Coupon> findActiveByCouponType(@Param("couponType") Coupon.CouponType couponType);

    @Query("SELECT COUNT(c) FROM Coupon c WHERE c.campaign.id = :campaignId")
    long countByCampaignId(@Param("campaignId") UUID campaignId);

    @Query("SELECT COUNT(c) FROM Coupon c WHERE c.campaign.id = :campaignId AND c.usedCount > 0")
    long countUsedByCampaignId(@Param("campaignId") UUID campaignId);

    @Query("SELECT c FROM Coupon c LEFT JOIN FETCH c.usages WHERE c.id = :id")
    Optional<Coupon> findByIdWithUsages(@Param("id") UUID id);

    boolean existsByCode(String code);
}
