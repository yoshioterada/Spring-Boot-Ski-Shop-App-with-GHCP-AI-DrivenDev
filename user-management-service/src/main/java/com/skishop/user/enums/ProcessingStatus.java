package com.skishop.user.enums;

/**
 * 処理ステータス（ユーザー管理サービスから認証サービスへのフィードバック用）
 */
public enum ProcessingStatus {
    
    /**
     * 処理成功
     */
    SUCCESS("処理成功"),
    
    /**
     * 処理失敗
     */
    FAILED("処理失敗"),
    
    /**
     * 補償処理成功
     */
    COMPENSATION_SUCCESS("補償処理成功"),
    
    /**
     * 補償処理失敗
     */
    COMPENSATION_FAILED("補償処理失敗");

    private final String description;

    ProcessingStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
