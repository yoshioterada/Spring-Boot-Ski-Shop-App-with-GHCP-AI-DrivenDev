package com.skishop.inventory.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * カテゴリ作成リクエスト
 */
public record CategoryCreateRequest(
    @NotBlank(message = "カテゴリ名は必須です")
    @Size(max = 100, message = "カテゴリ名は100文字以内で入力してください")
    String name,
    
    @Size(max = 500, message = "カテゴリ説明は500文字以内で入力してください")
    String description,
    
    String parentId,
    
    Integer sortOrder,
    
    Boolean isVisible,
    
    String imageUrl
) {
    public String getName() {
        return name;
    }
}
