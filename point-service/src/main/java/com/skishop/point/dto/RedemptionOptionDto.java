package com.skishop.point.dto;

import lombok.Data;

import java.util.UUID;

/**
 * ポイント交換オプション DTO
 */
@Data
public class RedemptionOptionDto {
    
    private UUID id;
    private String type;
    private String name;
    private String description;
    private Integer pointCost;
    private Integer value;
    private String category;
    private boolean available;
    private Integer maxRedemptions;
    private String terms;
}
