package com.skishop.frontend.dto;

/**
 * 住所DTO
 */
public record AddressDto(
    String firstName,
    String lastName,
    String street,
    String city,
    String state,
    String zipCode,
    String country,
    String phoneNumber
) {}
