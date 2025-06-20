package com.skishop.ai.entity;

/**
 * 会話ステータスを表すsealed interface
 * 
 * <p>Java 21のsealed interfaceを使用して会話のステータスを型安全に定義</p>
 * <p>パターンマッチングとswitch文での完全性チェックを提供</p>
 * 
 * @since 1.0.0
 */
public sealed interface ConversationStatus 
    permits ConversationStatus.Active, ConversationStatus.Completed, 
            ConversationStatus.Abandoned, ConversationStatus.Escalated {
    
    /**
     * アクティブな会話
     */
    record Active() implements ConversationStatus {}
    
    /**
     * 完了した会話
     */
    record Completed() implements ConversationStatus {}
    
    /**
     * 放棄された会話
     */
    record Abandoned() implements ConversationStatus {}
    
    /**
     * エスカレーションされた会話
     */
    record Escalated() implements ConversationStatus {}
    
    /**
     * 文字列からConversationStatusを作成するファクトリメソッド
     * 
     * @param statusString ステータスを表す文字列
     * @return 対応するConversationStatusインスタンス
     * @throws IllegalArgumentException 不明なステータスの場合
     */
    static ConversationStatus fromString(String statusString) {
        return switch (statusString.toUpperCase()) {
            case "ACTIVE" -> new Active();
            case "COMPLETED" -> new Completed();
            case "ABANDONED" -> new Abandoned();
            case "ESCALATED" -> new Escalated();
            default -> throw new IllegalArgumentException("Unknown status: " + statusString);
        };
    }
    
    /**
     * ConversationStatusを文字列に変換
     * 
     * @return ステータスを表す文字列
     */
    default String asString() {
        return switch (this) {
            case Active() -> "ACTIVE";
            case Completed() -> "COMPLETED";
            case Abandoned() -> "ABANDONED";
            case Escalated() -> "ESCALATED";
        };
    }
    
    /**
     * ステータスが終了状態かどうかを判定
     * 
     * @return 終了状態の場合true
     */
    default boolean isFinished() {
        return switch (this) {
            case Completed(), Abandoned(), Escalated() -> true;
            case Active() -> false;
        };
    }
    
    /**
     * メッセージ追加可能かどうかを判定
     * 
     * @return メッセージ追加可能な場合true
     */
    default boolean canAddMessage() {
        return switch (this) {
            case Active() -> true;
            case Completed(), Abandoned(), Escalated() -> false;
        };
    }
    
    /**
     * 表示用の文字列を取得
     * 
     * @return 表示用文字列
     */
    default String getDisplayName() {
        return switch (this) {
            case Active() -> "アクティブ";
            case Completed() -> "完了";
            case Abandoned() -> "放棄";
            case Escalated() -> "エスカレーション";
        };
    }
}
