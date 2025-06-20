package com.skishop.coupon.mapper;

import com.skishop.coupon.dto.CampaignDto;
import com.skishop.coupon.entity.Campaign;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CampaignMapper {

    CampaignDto.CampaignResponse toResponse(Campaign campaign);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "coupons", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "generatedCoupons", constant = "0")
    @Mapping(target = "isActive", constant = "false")
    Campaign toEntity(CampaignDto.CampaignRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "coupons", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "generatedCoupons", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "campaignType", ignore = true)
    void updateEntity(CampaignDto.CampaignUpdateRequest request, @MappingTarget Campaign campaign);
}
