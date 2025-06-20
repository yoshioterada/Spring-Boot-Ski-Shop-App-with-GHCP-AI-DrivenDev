package com.skishop.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;

/**
 * チャットメッセージリクエスト DTO
 * Java 21のRecord機能を使用した不変データクラス
 * 
 * @param userId ユーザーID（必須）
 * @param content メッセージ内容（必須、最大1000文字）
 * @param conversationId 会話ID（オプション）
 * @param sessionId セッションID（オプション）
 * @param context コンテキスト情報（オプション）
 */
public record ChatMessageRequest(
    @NotBlank(message = "User ID is required")
    String userId,
    
    @NotBlank(message = "Message content is required")
    @Size(max = 1000, message = "Message content must not exceed 1000 characters")
    String content,
    
    String conversationId,
    String sessionId,
    Map<String, Object> context
) {
    
    /**
     * 基本的なチャットメッセージを作成するファクトリメソッド
     */
    public static ChatMessageRequest of(String userId, String content) {
        return new ChatMessageRequest(userId, content, null, null, null);
    }
    
    /**
     * コンテキスト付きチャットメッセージを作成するファクトリメソッド
     */
    public static ChatMessageRequest withContext(String userId, String content, 
                                               Map<String, Object> context) {
        return new ChatMessageRequest(userId, content, null, null, context);
    }
    
    /**
     * セッション継続用のチャットメッセージを作成するファクトリメソッド
     */
    public static ChatMessageRequest forSession(String userId, String content, 
                                              String sessionId, String conversationId) {
        return new ChatMessageRequest(userId, content, conversationId, sessionId, null);
    }
}
