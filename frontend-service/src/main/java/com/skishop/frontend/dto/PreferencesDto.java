package com.skishop.frontend.dto;

/**
 * ユーザー設定DTO
 */
public record PreferencesDto(
    String language,
    String currency,
    boolean emailNotifications,
    boolean smsNotifications
) {}
