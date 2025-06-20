package com.skishop.user.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.util.Set;

/**
 * ユーザーロール更新リクエストDTO
 */
public record UserRoleUpdateRequest(
    
    @NotBlank(message = "User ID is required")
    String userId,
    
    Set<String> roleIds
) {
}
