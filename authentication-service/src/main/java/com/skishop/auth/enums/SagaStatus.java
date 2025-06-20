package com.skishop.auth.enums;

/**
 * Sagaパターンにおけるオーケストレーションステータス
 */
public enum SagaStatus {
    SAGA_STARTED("Sagaトランザクション開始"),
    SAGA_IN_PROGRESS("Sagaトランザクション実行中"),
    SAGA_STEP_COMPLETED("Sagaステップ完了"),
    SAGA_STEP_FAILED("Sagaステップ失敗"),
    SAGA_COMPENSATING("補償処理実行中"),
    SAGA_COMPENSATED("補償処理完了"),
    SAGA_COMPENSATION_FAILED("補償処理失敗"),
    SAGA_COMPLETED("Sagaトランザクション成功完了"),
    SAGA_FAILED("Sagaトランザクション失敗"),
    SAGA_TIMEOUT("Sagaトランザクションタイムアウト");
    
    private final String description;
    
    SagaStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
