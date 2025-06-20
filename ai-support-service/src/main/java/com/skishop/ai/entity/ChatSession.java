package com.skishop.ai.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * チャットセッション エンティティ
 * 
 * <p>Java 21のrecord機能を使用した不変データクラス</p>
 * <p>チャットボットとのセッション情報を管理する</p>
 * 
 * @param sessionId セッションID（MongoDB _id）
 * @param userId ユーザーID
 * @param conversationId 会話ID
 * @param messages チャットメッセージ一覧
 * @param sessionType セッションタイプ
 * @param status セッションステータス
 * @param context セッションコンテキスト
 * @param startedAt 開始日時
 * @param endedAt 終了日時
 * @param updatedAt 更新日時
 * 
 * @since 1.0.0
 */
@Document(collection = "chat_sessions")
public record ChatSession(
    @Id
    String sessionId,
    String userId,
    String conversationId,
    List<ChatMessage> messages,
    SessionType sessionType,
    SessionStatus status,
    Map<String, Object> context,
    LocalDateTime startedAt,
    LocalDateTime endedAt,
    LocalDateTime updatedAt
) {
    
    /**
     * 新規セッション作成用ファクトリメソッド
     */
    public static ChatSession startNew(String userId, String conversationId, SessionType type) {
        var now = LocalDateTime.now();
        return new ChatSession(
            null, // MongoDB generates ID
            userId,
            conversationId,
            List.of(),
            type,
            new SessionStatus.Active(now),
            Map.of(),
            now,
            null,
            now
        );
    }
    
    /**
     * メッセージ追加
     */
    public ChatSession withMessage(ChatMessage message) {
        var updatedMessages = new java.util.ArrayList<>(messages);
        updatedMessages.add(message);
        
        return new ChatSession(
            sessionId,
            userId,
            conversationId,
            List.copyOf(updatedMessages),
            sessionType,
            new SessionStatus.Active(LocalDateTime.now()),
            context,
            startedAt,
            endedAt,
            LocalDateTime.now()
        );
    }
    
    /**
     * セッション終了
     */
    public ChatSession close(String reason) {
        var now = LocalDateTime.now();
        return new ChatSession(
            sessionId,
            userId,
            conversationId,
            messages,
            sessionType,
            new SessionStatus.Closed(reason, now),
            context,
            startedAt,
            now,
            now
        );
    }
}
