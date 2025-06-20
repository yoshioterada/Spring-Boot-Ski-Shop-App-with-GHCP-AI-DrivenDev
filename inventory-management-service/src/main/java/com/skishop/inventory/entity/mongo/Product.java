package com.skishop.inventory.entity.mongo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 商品エンティティ（MongoDB）
 * 商品の基本情報、属性、カテゴリ情報を管理
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
@Document(collection = "products")
public class Product {

    @Id
    private String id;

    @Indexed(unique = true)
    private String sku;

    private String name;

    private String description;

    private String brand;

    /**
     * 商品属性（JSON形式）
     * 例: {"length": "180cm", "width": "100mm", "color": "Red/Black"}
     */
    private Map<String, Object> attributes;

    /**
     * 商品タグ
     */
    private List<String> tags;

    /**
     * カテゴリID
     */
    @Indexed
    private String categoryId;

    /**
     * アクティブ状態
     */
    @Indexed
    private Boolean active;

    /**
     * 作成日時
     */
    private LocalDateTime createdAt;

    /**
     * 更新日時
     */
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
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (active == null) {
            active = true;
        }
    }

    /**
     * エンティティ更新前処理
     */
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
