package com.skishop.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

/**
 * ロール作成リクエストDTO
 */
public record RoleCreateRequest(
    
    @NotBlank(message = "Role name is required")
    @Size(max = 50, message = "Role name must not exceed 50 characters")
    String name,
    
    @Size(max = 255, message = "Description must not exceed 255 characters")
    String description,
    
    Set<String> permissions
) {
}
