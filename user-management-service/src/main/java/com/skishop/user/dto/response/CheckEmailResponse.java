package com.skishop.user.dto.response;

/**
 * メールチェックレスポンスDTO
 */
public record CheckEmailResponse(
    String email,
    boolean exists,
    String message
) {
}
