package com.skishop.payment.dto;

/**
 * 住所リクエスト
 * 
 * @param line1 住所1行目
 * @param line2 住所2行目
 * @param city 市区町村
 * @param state 都道府県
 * @param postalCode 郵便番号
 * @param country 国
 */
public record AddressRequest(
    String line1,
    String line2,
    String city,
    String state,
    String postalCode,
    String country
) {}
