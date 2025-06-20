package com.skishop.coupon.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "campaigns")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@EntityListeners(AuditingEntityListener.class)
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "campaign_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private CampaignType campaignType;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = false;

    @Type(JsonType.class)
    @Column(name = "rules", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> rules = Map.of();

    @Column(name = "max_coupons")
    private Integer maxCoupons;

    @Column(name = "generated_coupons")
    @Builder.Default
    private Integer generatedCoupons = 0;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Coupon> coupons;

    public enum CampaignType {
        PERCENTAGE,
        FIXED_AMOUNT,
        BOGO,
        FREE_SHIPPING
    }

    public void incrementGeneratedCoupons() {
        this.generatedCoupons = (this.generatedCoupons == null ? 0 : this.generatedCoupons) + 1;
    }

    public boolean isActive() {
        LocalDateTime now = LocalDateTime.now();
        return Boolean.TRUE.equals(isActive) && 
               startDate != null && endDate != null &&
               (now.isEqual(startDate) || now.isAfter(startDate)) &&
               (now.isEqual(endDate) || now.isBefore(endDate));
    }

    public boolean hasReachedMaxCoupons() {
        return maxCoupons != null && generatedCoupons != null && generatedCoupons >= maxCoupons;
    }
}
