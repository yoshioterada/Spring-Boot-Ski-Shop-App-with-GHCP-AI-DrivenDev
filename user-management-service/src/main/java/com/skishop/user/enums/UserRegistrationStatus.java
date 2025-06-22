package com.skishop.user.enums;

/**
 * ユーザー登録ステータス（ユーザー管理サービス側）
 * 設計仕様書に従ったステータス定義
 */
public enum UserRegistrationStatus {
    EVENT_RECEIVED("イベント受信"),
    VALIDATION_IN_PROGRESS("検証中"),
    VALIDATION_PASSED("検証成功"),
    VALIDATION_FAILED("検証失敗"),
    PROFILE_CREATION_IN_PROGRESS("プロファイル作成中"),
    PROFILE_CREATED("プロファイル作成成功"),
    PROFILE_CREATION_FAILED("プロファイル作成失敗"),
    DUPLICATE_USER_DETECTED("重複ユーザー検出"),
    PROCESSING_TIMEOUT("処理タイムアウト");
    
    private final String description;
    
    UserRegistrationStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
