package com.skishop.inventory.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * カテゴリDTO（Java 21のrecord使用）
 */
public record CategoryDTO(
    String id,
    
    @NotBlank(message = "カテゴリ名は必須です")
    @Size(max = 100, message = "カテゴリ名は100文字以内で入力してください")
    String name,
    
    @Size(max = 500, message = "カテゴリ説明は500文字以内で入力してください")
    String description,
    
    /**
     * 親カテゴリID
     */
    String parentId,
    
    /**
     * 親カテゴリ情報
     */
    CategoryDTO parent,
    
    /**
     * 子カテゴリリスト
     */
    List<CategoryDTO> children,
    
    /**
     * 階層レベル
     */
    Integer level,
    
    /**
     * カテゴリパス
     */
    String path,
    
    /**
     * アクティブ状態
     */
    Boolean active,
    
    /**
     * 作成日時
     */
    LocalDateTime createdAt,
    
    /**
     * 更新日時
     */
    LocalDateTime updatedAt,
    
    /**
     * 商品数（このカテゴリに属する商品の数）
     */
    Long productCount
) {
    /**
     * ルートカテゴリかどうかの判定
     */
    public boolean isRoot() {
        return parentId == null || parentId.isBlank();
    }
    
    /**
     * 子カテゴリを持つかどうかの判定
     */
    public boolean hasChildren() {
        return children != null && !children.isEmpty();
    }
    
    /**
     * アクティブかつ利用可能なカテゴリかどうかの判定
     */
    public boolean isAvailable() {
        return Boolean.TRUE.equals(active);
    }
}
