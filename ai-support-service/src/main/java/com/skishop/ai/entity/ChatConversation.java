package com.skishop.ai.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * チャット会話エンティティ
 * 
 * <p>Java 21のrecord機能を使用した不変データクラス</p>
 * <p>チャットボットとの会話情報を管理する</p>
 * 
 * @param conversationId 会話ID
 * @param userId ユーザーID
 * @param sessionId セッションID
 * @param messages メッセージリスト
 * @param status 会話ステータス
 * @param satisfaction 満足度
 * @param createdAt 作成日時
 * @param updatedAt 更新日時
 * @param metadata メタデータ
 * 
 * @since 1.0.0
 */
@Document(collection = "chat_conversations")
public record ChatConversation(
    @Id String conversationId,
    String userId,
    String sessionId,
    List<ChatMessage> messages,
    ConversationStatus status,
    Double satisfaction,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    Map<String, Object> metadata
) {
    
    /**
     * 新規会話作成用ファクトリメソッド
     * 
     * @param userId ユーザーID
     * @param sessionId セッションID
     * @return 新しいChatConversationインスタンス
     */
    public static ChatConversation create(String userId, String sessionId) {
        var now = LocalDateTime.now();
        return new ChatConversation(
            UUID.randomUUID().toString(),
            userId,
            sessionId,
            List.of(),
            new ConversationStatus.Active(),
            null,
            now,
            now,
            Map.of()
        );
    }
    
    /**
     * メッセージを追加した新しい会話を作成
     * 
     * @param message 追加するメッセージ
     * @return メッセージが追加された新しいChatConversationインスタンス
     * @throws IllegalStateException メッセージ追加ができないステータスの場合
     */
    public ChatConversation addMessage(ChatMessage message) {
        if (!status.canAddMessage()) {
            throw new IllegalStateException("""
                現在のステータス「%s」ではメッセージを追加できません。
                """.formatted(status.asString()));
        }
        
        var newMessages = new java.util.ArrayList<>(messages);
        newMessages.add(message);
            
        return new ChatConversation(
            conversationId,
            userId,
            sessionId,
            List.copyOf(newMessages),
            status,
            satisfaction,
            createdAt,
            LocalDateTime.now(),
            metadata
        );
    }
    
    /**
     * ステータスを更新した新しい会話を作成
     * 
     * @param newStatus 新しいステータス
     * @return ステータスが更新された新しいChatConversationインスタンス
     */
    public ChatConversation updateStatus(ConversationStatus newStatus) {
        return new ChatConversation(
            conversationId,
            userId,
            sessionId,
            messages,
            newStatus,
            satisfaction,
            createdAt,
            LocalDateTime.now(),
            metadata
        );
    }
    
    /**
     * 満足度を設定した新しい会話を作成
     * 
     * @param satisfaction 満足度 (0.0 - 1.0)
     * @return 満足度が設定された新しいChatConversationインスタンス
     */
    public ChatConversation setSatisfaction(Double satisfaction) {
        return new ChatConversation(
            conversationId,
            userId,
            sessionId,
            messages,
            status,
            satisfaction,
            createdAt,
            LocalDateTime.now(),
            metadata
        );
    }
}
