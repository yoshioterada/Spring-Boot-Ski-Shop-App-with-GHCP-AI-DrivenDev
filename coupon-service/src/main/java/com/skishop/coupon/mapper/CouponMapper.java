package com.skishop.coupon.mapper;

import com.skishop.coupon.dto.CouponDto;
import com.skishop.coupon.entity.Coupon;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CouponMapper {

    @Mapping(target = "campaignId", source = "campaign.id")
    @Mapping(target = "campaignName", source = "campaign.name")
    CouponDto.CouponResponse toResponse(Coupon coupon);

    CouponDto.CouponInfo toCouponInfo(Coupon coupon);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "campaign", ignore = true)
    @Mapping(target = "usages", ignore = true)
    @Mapping(target = "userCoupons", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "usedCount", constant = "0")
    @Mapping(target = "isActive", constant = "true")
    Coupon toEntity(CouponDto.CouponRequest request);
}
