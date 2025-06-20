package com.skishop.point.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "point_redemptions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointRedemption {
    
    @Id
    @GeneratedValue(generator = "UUID")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;
    
    @Column(name = "redemption_type", nullable = false, length = 50)
    private String redemptionType; // 'discount', 'product', 'cashback'
    
    @Column(name = "points_used", nullable = false)
    private Integer pointsUsed;
    
    @Column(name = "value_redeemed", nullable = false, precision = 10, scale = 2)
    private BigDecimal valueRedeemed;
    
    @Column(name = "status", length = 20)
    @Builder.Default
    private String status = "completed";
    
    @ElementCollection
    @CollectionTable(name = "point_redemption_details", joinColumns = @JoinColumn(name = "redemption_id"))
    @MapKeyColumn(name = "detail_key")
    @Column(name = "detail_value")
    private Map<String, String> details;
    
    @CreationTimestamp
    @Column(name = "redeemed_at")
    private LocalDateTime redeemedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", insertable = false, updatable = false)
    private PointTransaction transaction;
}
