package com.skishop.frontend.dto;

/**
 * ユーザープロファイルDTO
 */
public record UserProfileDto(
    String id,
    String email,
    String firstName,
    String lastName,
    String phoneNumber,
    AddressDto defaultAddress,
    PreferencesDto preferences
) {}
