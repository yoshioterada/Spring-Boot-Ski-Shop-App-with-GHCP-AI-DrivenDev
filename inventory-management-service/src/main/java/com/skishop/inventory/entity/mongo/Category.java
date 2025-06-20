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

/**
 * カテゴリエンティティ（MongoDB）
 * 商品カテゴリの階層構造を管理
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
@Document(collection = "categories")
public class Category {

    @Id
    private String id;

    @Indexed
    private String name;

    private String description;

    /**
     * 親カテゴリID（階層構造用）
     */
    @Indexed
    private String parentId;

    /**
     * 階層レベル（0: ルートカテゴリ）
     */
    @Indexed
    private Integer level;

    /**
     * カテゴリパス（例: "スポーツ用品/ウィンタースポーツ/スキー"）
     */
    private String path;

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
        if (level == null) {
            level = 0;
        }
    }

    /**
     * エンティティ更新前処理
     */
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
