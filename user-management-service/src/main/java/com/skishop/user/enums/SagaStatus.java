package com.skishop.user.enums;

/**
 * Sagaパターンにおけるオーケストレーションステータス
 * 設計文書の要件に基づく包括的なSagaライフサイクル管理
 */
public enum SagaStatus {
    
    // Saga開始と進行
    /**
     * Sagaトランザクション開始
     */
    SAGA_STARTED("Sagaトランザクション開始"),
    
    /**
     * Sagaトランザクション実行中
     */
    SAGA_IN_PROGRESS("Sagaトランザクション実行中"),
    
    /**
     * Sagaステップ完了
     */
    SAGA_STEP_COMPLETED("Sagaステップ完了"),
    
    /**
     * Sagaステップ失敗
     */
    SAGA_STEP_FAILED("Sagaステップ失敗"),
    
    // 補償処理
    /**
     * 補償処理実行中
     */
    SAGA_COMPENSATING("補償処理実行中"),
    
    /**
     * 補償処理完了
     */
    SAGA_COMPENSATED("補償処理完了"),
    
    /**
     * 補償処理失敗
     */
    SAGA_COMPENSATION_FAILED("補償処理失敗"),
    
    // 最終状態
    /**
     * Sagaトランザクション成功完了
     */
    SAGA_COMPLETED("Sagaトランザクション成功完了"),
    
    /**
     * Sagaトランザクション失敗
     */
    SAGA_FAILED("Sagaトランザクション失敗"),
    
    /**
     * Sagaトランザクションタイムアウト
     */
    SAGA_TIMEOUT("Sagaトランザクションタイムアウト");

    private final String description;

    SagaStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 終了状態かどうかを判定
     */
    public boolean isTerminal() {
        return this == SAGA_COMPLETED || 
               this == SAGA_FAILED || 
               this == SAGA_COMPENSATION_FAILED ||
               this == SAGA_TIMEOUT;
    }

    /**
     * 成功状態かどうかを判定
     */
    public boolean isSuccess() {
        return this == SAGA_COMPLETED;
    }

    /**
     * 失敗状態かどうかを判定
     */
    public boolean isFailure() {
        return this == SAGA_FAILED || 
               this == SAGA_COMPENSATION_FAILED ||
               this == SAGA_TIMEOUT;
    }

    /**
     * 補償処理が必要な状態かどうかを判定
     */
    public boolean needsCompensation() {
        return this == SAGA_STEP_FAILED || this == SAGA_COMPENSATING;
    }

    /**
     * リトライ可能な状態かどうかを判定
     */
    public boolean isRetryable() {
        return this == SAGA_STEP_FAILED && !isTerminal();
    }
}
