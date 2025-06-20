package com.skishop.ai.dto;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * チャットメッセージレスポンス DTO
 * Java 21のRecord機能を使用した不変データクラス
 * 
 * @param messageId メッセージID
 * @param conversationId 会話ID
 * @param content メッセージ内容
 * @param intent 意図
 * @param confidence 信頼度
 * @param timestamp タイムスタンプ
 * @param context コンテキスト情報
 * @param requiresAction アクション要求フラグ
 * @param actionType アクションタイプ
 */
public record ChatMessageResponse(
    String messageId,
    String conversationId,
    String content,
    String intent,
    Double confidence,
    LocalDateTime timestamp,
    Map<String, Object> context,
    boolean requiresAction,
    String actionType
) {
    
    /**
     * 基本的なレスポンスを作成するファクトリメソッド
     */
    public static ChatMessageResponse of(String messageId, String conversationId, String content) {
        return new ChatMessageResponse(
            messageId,
            conversationId,
            content,
            null,
            null,
            LocalDateTime.now(),
            null,
            false,
            null
        );
    }
    
    /**
     * 信頼度付きレスポンスを作成するファクトリメソッド
     */
    public static ChatMessageResponse withConfidence(String messageId, String conversationId, 
                                                   String content, String intent, Double confidence) {
        return new ChatMessageResponse(
            messageId,
            conversationId,
            content,
            intent,
            confidence,
            LocalDateTime.now(),
            null,
            false,
            null
        );
    }
    
    /**
     * アクション要求付きレスポンスを作成するファクトリメソッド
     */
    public static ChatMessageResponse withAction(String messageId, String conversationId, 
                                               String content, String actionType) {
        return new ChatMessageResponse(
            messageId,
            conversationId,
            content,
            null,
            null,
            LocalDateTime.now(),
            null,
            true,
            actionType
        );
    }
}
