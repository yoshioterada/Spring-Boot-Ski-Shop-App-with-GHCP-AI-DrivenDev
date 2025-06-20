package com.skishop.coupon.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_coupons",
       indexes = {
           @Index(name = "idx_user_coupon_user", columnList = "user_id"),
           @Index(name = "idx_user_coupon_coupon", columnList = "coupon_id"),
           @Index(name = "idx_user_coupon_redeemed", columnList = "is_redeemed"),
           @Index(name = "idx_user_coupon_assigned", columnList = "assigned_at")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@EntityListeners(AuditingEntityListener.class)
public class UserCoupon {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    @JsonBackReference
    private Coupon coupon;

    @Column(name = "is_redeemed")
    @Builder.Default
    private Boolean isRedeemed = false;

    @CreatedDate
    @Column(name = "assigned_at", nullable = false, updatable = false)
    private LocalDateTime assignedAt;

    @LastModifiedDate
    @Column(name = "redeemed_at")
    private LocalDateTime redeemedAt;

    public void markAsRedeemed() {
        this.isRedeemed = true;
        this.redeemedAt = LocalDateTime.now();
    }

    public boolean canRedeem() {
        return !Boolean.TRUE.equals(isRedeemed) && coupon != null && coupon.canBeUsed();
    }
}
