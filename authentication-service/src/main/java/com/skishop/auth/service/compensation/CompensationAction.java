package com.skishop.auth.service.compensation;

/**
 * 補償アクションを定義するインターフェース
 */
public interface CompensationAction {
    
    /**
     * 補償処理を実行
     * @param sagaId Saga ID
     * @param context 補償処理に必要なコンテキスト情報
     * @return 補償処理が成功した場合true、失敗した場合false
     */
    boolean compensate(String sagaId, CompensationContext context);
    
    /**
     * この補償アクションが適用可能かどうかを判定
     * @param sagaType Saga種類
     * @param status 現在のステータス
     * @return 適用可能な場合true
     */
    boolean isApplicable(String sagaType, String status);
    
    /**
     * 補償アクションの優先度（低い数値ほど高優先度）
     * @return 優先度
     */
    int getPriority();
    
    /**
     * 補償アクションの名前
     * @return アクション名
     */
    String getActionName();
}
