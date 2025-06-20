package com.skishop.coupon.repository;

import com.skishop.coupon.entity.Campaign;
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
public interface CampaignRepository extends JpaRepository<Campaign, UUID> {

    @Query("SELECT c FROM Campaign c WHERE c.isActive = true AND c.startDate <= :now AND c.endDate >= :now")
    List<Campaign> findActiveCampaigns(@Param("now") LocalDateTime now);

    @Query("SELECT c FROM Campaign c WHERE c.isActive = :isActive")
    Page<Campaign> findByIsActive(@Param("isActive") Boolean isActive, Pageable pageable);

    @Query("SELECT c FROM Campaign c WHERE c.startDate >= :startDate AND c.endDate <= :endDate")
    List<Campaign> findByDateRange(@Param("startDate") LocalDateTime startDate, 
                                  @Param("endDate") LocalDateTime endDate);

    @Query("SELECT c FROM Campaign c WHERE c.campaignType = :type AND c.isActive = true")
    List<Campaign> findActiveByCampaignType(@Param("type") Campaign.CampaignType type);

    @Query("SELECT c FROM Campaign c WHERE c.endDate < :now AND c.isActive = true")
    List<Campaign> findExpiredActiveCampaigns(@Param("now") LocalDateTime now);

    Optional<Campaign> findByNameAndIsActive(String name, Boolean isActive);

    @Query("SELECT COUNT(c) FROM Campaign c WHERE c.isActive = true")
    long countActiveCampaigns();

    @Query("SELECT c FROM Campaign c LEFT JOIN FETCH c.coupons WHERE c.id = :id")
    Optional<Campaign> findByIdWithCoupons(@Param("id") UUID id);
}
