package com.skishop.ai.entity;

/**
 * メッセージ役割を表すsealed interface
 * 
 * <p>Java 21のsealed interfaceを使用してメッセージの役割を型安全に定義</p>
 * <p>パターンマッチングとswitch文での完全性チェックを提供</p>
 * 
 * @since 1.0.0
 */
public sealed interface MessageRole 
    permits MessageRole.User, MessageRole.Assistant, MessageRole.System {
    
    /**
     * ユーザーからのメッセージ
     */
    record User() implements MessageRole {}
    
    /**
     * AIアシスタントからのメッセージ
     */
    record Assistant() implements MessageRole {}
    
    /**
     * システムメッセージ
     */
    record System() implements MessageRole {}
    
    /**
     * 文字列からMessageRoleを作成するファクトリメソッド
     * 
     * @param roleString 役割を表す文字列
     * @return 対応するMessageRoleインスタンス
     * @throws IllegalArgumentException 不明な役割の場合
     */
    static MessageRole fromString(String roleString) {
        return switch (roleString.toUpperCase()) {
            case "USER" -> new User();
            case "ASSISTANT" -> new Assistant();
            case "SYSTEM" -> new System();
            default -> throw new IllegalArgumentException("Unknown role: " + roleString);
        };
    }
    
    /**
     * MessageRoleを文字列に変換
     * 
     * @return 役割を表す文字列
     */
    default String asString() {
        return switch (this) {
            case User() -> "USER";
            case Assistant() -> "ASSISTANT";
            case System() -> "SYSTEM";
        };
    }
    
    /**
     * 権限チェック用メソッド
     * 
     * @return メッセージ作成が可能かどうか
     */
    default boolean canCreateMessage() {
        return switch (this) {
            case User(), System() -> true;
            case Assistant() -> false; // アシスタントは通常、システムによって作成される
        };
    }
    
    /**
     * 表示用の文字列を取得
     * 
     * @return 表示用文字列
     */
    default String getDisplayName() {
        return switch (this) {
            case User() -> "ユーザー";
            case Assistant() -> "AIアシスタント";
            case System() -> "システム";
        };
    }
}
