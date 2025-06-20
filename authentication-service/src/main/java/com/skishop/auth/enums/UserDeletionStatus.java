package com.skishop.auth.enums;

/**
 * 設計文書に準拠したユーザー削除ステータス定義
 */
public enum UserDeletionStatus {
    // 認証サービス側ステータス
    PENDING_DELETION("ユーザー削除要求を受信、処理開始"),
    DELETION_AUTHORIZED("削除権限の確認完了"),
    ACCOUNT_SOFT_DELETED("認証サービスでアカウント論理削除完了"),
    DELETION_EVENT_PUBLISHED("ユーザー削除イベントの発行成功"),
    DELETION_EVENT_PUBLISH_FAILED("ユーザー削除イベントの発行失敗"),
    PENDING_USER_MANAGEMENT_DELETION("ユーザー管理サービスでの削除処理待機中"),
    DELETION_COMPLETED("ユーザー削除プロセス全体の完了"),
    DELETION_FAILED("ユーザー削除プロセスの失敗"),
    DELETION_ROLLBACK_REQUIRED("削除の巻き戻しが必要な状態"),
    DELETION_ROLLED_BACK("削除巻き戻し完了"),
    
    // ユーザー管理サービス側ステータス
    DELETION_EVENT_RECEIVED("ユーザー削除イベントを受信"),
    DELETION_VALIDATION_IN_PROGRESS("削除イベントデータの検証中"),
    DELETION_VALIDATION_PASSED("削除イベントデータ検証成功"),
    DELETION_VALIDATION_FAILED("削除イベントデータ検証失敗"),
    PROFILE_DELETION_IN_PROGRESS("ユーザープロファイル削除中"),
    RELATED_DATA_CLEANUP_IN_PROGRESS("関連データのクリーンアップ中"),
    PROFILE_DELETED("ユーザープロファイル削除成功"),
    PROFILE_DELETION_FAILED("ユーザープロファイル削除失敗"),
    USER_NOT_FOUND("削除対象ユーザーが見つからない"),
    DELETION_TIMEOUT("削除処理タイムアウト");
    
    private final String description;
    
    UserDeletionStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
