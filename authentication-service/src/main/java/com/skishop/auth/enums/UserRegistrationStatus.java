package com.skishop.auth.enums;

/**
 * 設計文書に準拠したユーザー登録ステータス定義
 */
public enum UserRegistrationStatus {
    // 認証サービス側ステータス
    PENDING_REGISTRATION("ユーザー登録要求を受信、認証サービスでの処理開始"),
    ACCOUNT_CREATED("認証サービスでユーザーアカウント作成完了"),
    EVENT_PUBLISHED("ユーザー登録イベントの発行成功"),
    EVENT_PUBLISH_FAILED("ユーザー登録イベントの発行失敗"),
    PENDING_USER_MANAGEMENT("ユーザー管理サービスでの処理待機中"),
    REGISTRATION_COMPLETED("ユーザー登録プロセス全体の完了"),
    REGISTRATION_FAILED("ユーザー登録プロセスの失敗"),
    COMPENSATION_REQUIRED("補償処理が必要な状態"),
    COMPENSATED("補償処理完了"),
    
    // ユーザー管理サービス側ステータス
    EVENT_RECEIVED("ユーザー登録イベントを受信"),
    VALIDATION_IN_PROGRESS("イベントデータの検証中"),
    VALIDATION_PASSED("イベントデータ検証成功"),
    VALIDATION_FAILED("イベントデータ検証失敗"),
    PROFILE_CREATION_IN_PROGRESS("ユーザープロファイル作成中"),
    PROFILE_CREATED("ユーザープロファイル作成成功"),
    PROFILE_CREATION_FAILED("ユーザープロファイル作成失敗"),
    DUPLICATE_USER_DETECTED("重複ユーザーの検出"),
    PROCESSING_TIMEOUT("処理タイムアウト");
    
    private final String description;
    
    UserRegistrationStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
