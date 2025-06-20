package com.skishop.ai.entity;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * チャットメッセージ エンティティ
 * 
 * <p>Java 21のrecord機能を使用した不変データクラス</p>
 * <p>チャットボットとのメッセージ情報を管理する</p>
 * 
 * @param messageId メッセージID
 * @param timestamp タイムスタンプ
 * @param role メッセージ役割
 * @param content メッセージ内容
 * @param intent インテント
 * @param entities エンティティ情報
 * @param confidence 信頼度
 * @param context コンテキスト情報
 * 
 * @since 1.0.0
 */
public record ChatMessage(
    String messageId,
    LocalDateTime timestamp,
    MessageRole role,
    String content,
    String intent,
    Map<String, Object> entities,
    Double confidence,
    Map<String, Object> context
) {
    
    /**
     * 新規メッセージ作成用ファクトリメソッド
     */
    public static ChatMessage create(MessageRole role, String content) {
        return new ChatMessage(
            UUID.randomUUID().toString(),
            LocalDateTime.now(),
            role,
            content,
            null,
            Map.of(),
            null,
            Map.of()
        );
    }
    
    /**
     * インテント付きメッセージ作成
     */
    public static ChatMessage withIntent(MessageRole role, String content, String intent, Double confidence) {
        return new ChatMessage(
            UUID.randomUUID().toString(),
            LocalDateTime.now(),
            role,
            content,
            intent,
            Map.of(),
            confidence,
            Map.of()
        );
    }
    
    /**
     * エンティティ情報を追加したメッセージを作成
     */
    public ChatMessage withEntities(Map<String, Object> entities) {
        return new ChatMessage(
            messageId,
            timestamp,
            role,
            content,
            intent,
            entities,
            confidence,
            context
        );
    }
}