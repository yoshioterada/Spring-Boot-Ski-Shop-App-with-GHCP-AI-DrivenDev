package com.skishop.coupon.service;

import com.skishop.coupon.dto.CampaignDto;
import com.skishop.coupon.entity.Campaign;
import com.skishop.coupon.exception.CouponException;
import com.skishop.coupon.mapper.CampaignMapper;
import com.skishop.coupon.repository.CampaignRepository;
import com.skishop.coupon.repository.CouponUsageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final CouponUsageRepository couponUsageRepository;
    private final CampaignMapper campaignMapper;

    @Transactional
    @CacheEvict(value = "campaigns", allEntries = true)
    public CampaignDto.CampaignResponse createCampaign(CampaignDto.CampaignRequest request) {
        log.info("Creating campaign: {}", request.getName());

        // 同名のアクティブキャンペーンの重複確認
        if (campaignRepository.findByNameAndIsActive(request.getName(), true).isPresent()) {
            throw new CouponException("Active campaign with name already exists: " + request.getName());
        }

        Campaign campaign = Campaign.builder()
                .name(request.getName())
                .description(request.getDescription())
                .campaignType(request.getCampaignType())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .rules(request.getRules())
                .maxCoupons(request.getMaxCoupons())
                .isActive(false) // 初期状態は非アクティブ
                .build();

        campaign = campaignRepository.save(campaign);
        log.info("Created campaign with ID: {}", campaign.getId());
        
        return campaignMapper.toResponse(campaign);
    }

    @Cacheable(value = "campaigns", key = "#id")
    public CampaignDto.CampaignResponse getCampaignById(UUID id) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new CouponException("Campaign not found: " + id));
        return campaignMapper.toResponse(campaign);
    }

    public Page<CampaignDto.CampaignResponse> getCampaigns(CampaignDto.CampaignListRequest request) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), 
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Campaign> campaigns;
        
        if (request.getIsActive() != null) {
            campaigns = campaignRepository.findByIsActive(request.getIsActive(), pageable);
        } else if (request.getStartDate() != null && request.getEndDate() != null) {
            // 簡単な実装のため、全てのキャンペーンを返す
            campaigns = campaignRepository.findAll(pageable);
        } else {
            campaigns = campaignRepository.findAll(pageable);
        }

        return campaigns.map(campaignMapper::toResponse);
    }

    @Transactional
    @CacheEvict(value = "campaigns", key = "#campaignId")
    public CampaignDto.CampaignResponse updateCampaign(UUID campaignId, CampaignDto.CampaignUpdateRequest request) {
        log.info("Updating campaign: {}", campaignId);

        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new CouponException("Campaign not found: " + campaignId));

        // アクティブなキャンペーンの重要なフィールドは変更不可
        if (Boolean.TRUE.equals(campaign.getIsActive())) {
            if (request.getStartDate() != null || request.getEndDate() != null) {
                throw new CouponException("Cannot modify dates of active campaign");
            }
        }

        // 更新可能なフィールドのみ更新
        if (request.getName() != null) {
            campaign.setName(request.getName());
        }
        if (request.getDescription() != null) {
            campaign.setDescription(request.getDescription());
        }
        if (request.getRules() != null) {
            campaign.setRules(request.getRules());
        }
        if (request.getMaxCoupons() != null) {
            if (request.getMaxCoupons() < campaign.getGeneratedCoupons()) {
                throw new CouponException("Cannot set max coupons below already generated count");
            }
            campaign.setMaxCoupons(request.getMaxCoupons());
        }

        campaign = campaignRepository.save(campaign);
        log.info("Updated campaign: {}", campaignId);
        
        return campaignMapper.toResponse(campaign);
    }

    @Transactional
    @CacheEvict(value = "campaigns", key = "#campaignId")
    public CampaignDto.CampaignResponse activateCampaign(UUID campaignId) {
        log.info("Activating campaign: {}", campaignId);

        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new CouponException("Campaign not found: " + campaignId));

        // 日程確認
        LocalDateTime now = LocalDateTime.now();
        if (campaign.getStartDate().isAfter(now)) {
            throw new CouponException("Cannot activate campaign before start date");
        }
        if (campaign.getEndDate().isBefore(now)) {
            throw new CouponException("Cannot activate expired campaign");
        }

        campaign.setIsActive(true);
        campaign = campaignRepository.save(campaign);
        
        log.info("Activated campaign: {}", campaignId);
        return campaignMapper.toResponse(campaign);
    }

    @Cacheable(value = "campaign-analytics", key = "#campaignId")
    public CampaignDto.CampaignAnalyticsResponse getCampaignAnalytics(UUID campaignId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new CouponException("Campaign not found: " + campaignId));

        // 統計情報の取得
        long totalCoupons = campaign.getGeneratedCoupons();
        long usedCoupons = couponUsageRepository.countByCampaignId(campaignId);
        Double totalDiscount = couponUsageRepository.getTotalDiscountByCampaignId(campaignId);
        
        double usageRate = totalCoupons > 0 ? (double) usedCoupons / totalCoupons * 100 : 0;
        double averageDiscount = usedCoupons > 0 ? (totalDiscount != null ? totalDiscount / usedCoupons : 0) : 0;

        return CampaignDto.CampaignAnalyticsResponse.builder()
                .campaignId(campaignId)
                .campaignName(campaign.getName())
                .totalCoupons((int) totalCoupons)
                .usedCoupons((int) usedCoupons)
                .usageRate(usageRate)
                .totalDiscount(totalDiscount)
                .averageDiscount(averageDiscount)
                .totalOrders((int) usedCoupons) // 簡略化: 1クーポン1オーダーと仮定
                .revenue(0.0) // 外部システムから取得する必要があるため0で初期化
                .conversionRate(0.0) // 外部システムから取得する必要があるため0で初期化
                .periodStart(campaign.getStartDate())
                .periodEnd(campaign.getEndDate())
                .build();
    }

    public List<CampaignDto.CampaignResponse> getActiveCampaigns() {
        List<Campaign> campaigns = campaignRepository.findActiveCampaigns(LocalDateTime.now());
        return campaigns.stream()
                .map(campaignMapper::toResponse)
                .toList();
    }

    @Transactional
    public void expireExpiredCampaigns() {
        log.info("Expiring expired campaigns");
        List<Campaign> expiredCampaigns = campaignRepository.findExpiredActiveCampaigns(LocalDateTime.now());
        
        expiredCampaigns.forEach(campaign -> campaign.setIsActive(false));
        campaignRepository.saveAll(expiredCampaigns);
        
        log.info("Expired {} campaigns", expiredCampaigns.size());
    }
}
