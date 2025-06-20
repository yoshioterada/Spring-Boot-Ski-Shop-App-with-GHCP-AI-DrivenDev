package com.skishop.payment.dto;

/**
 * 請求先詳細リクエスト
 * 
 * @param name 名前
 * @param email メールアドレス
 * @param phone 電話番号
 * @param address 住所
 */
public record BillingDetailsRequest(
    String name,
    String email,
    String phone,
    AddressRequest address
) {}
