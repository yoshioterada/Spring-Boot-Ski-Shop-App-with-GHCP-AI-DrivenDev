package com.skishop.inventory.entity.jpa;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 商品画像エンティティ（PostgreSQL）
 * 商品の画像情報を管理
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "product_images", indexes = {
    @Index(name = "idx_product_images_product_id", columnList = "productId"),
    @Index(name = "idx_product_images_type", columnList = "type"),
    @Index(name = "idx_product_images_sort_order", columnList = "sortOrder")
})
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * 商品ID（MongoDBのProduct.idを参照）
     */
    @Column(nullable = false)
    private String productId;

    /**
     * 画像URL
     */
    @Column(nullable = false, length = 500)
    private String url;

    /**
     * サムネイル画像URL
     */
    @Column(length = 500)
    private String thumbnailUrl;

    /**
     * 画像タイプ
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ImageType type;

    /**
     * 表示順序
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    /**
     * 代替テキスト
     */
    @Column(length = 200)
    private String altText;

    /**
     * 作成日時
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新日時
     */
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 作成者
     */
    private String createdBy;

    /**
     * 更新者
     */
    private String updatedBy;

    /**
     * エンティティ作成前処理
     */
    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (sortOrder == null) {
            sortOrder = 0;
        }
        if (type == null) {
            type = ImageType.MAIN;
        }
    }

    /**
     * エンティティ更新前処理
     */
    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 画像タイプ列挙型
     */
    public enum ImageType {
        MAIN("メイン画像"),
        GALLERY("ギャラリー画像"),
        DETAIL("詳細画像"),
        THUMBNAIL("サムネイル画像");

        private final String displayName;

        ImageType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
