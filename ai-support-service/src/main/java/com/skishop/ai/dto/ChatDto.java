package com.skishop.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * チャット機能関連のDTO定義
 * Java 21のRecord機能を使用した不変データクラス群
 */
public final class ChatDto {

    // プライベートコンストラクタを追加してユーティリティクラスにする
    private ChatDto() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * チャットリクエストDTO
     * 
     * @param message メッセージ内容（必須）
     * @param sessionId セッションID（オプション）
     * @param conversationType 会話タイプ（SUPPORT, RECOMMENDATION, SEARCH）
     * @param context コンテキスト情報
     */
    public record ChatRequest(
        @NotBlank(message = "Message is required")
        String message,
        String sessionId,
        String conversationType,
        Map<String, Object> context
    ) {
        
        public ChatRequest {
            if (context == null) {
                context = Map.of();
            }
        }
        
        /**
         * 基本的なチャットリクエストを作成
         */
        public static ChatRequest of(String message) {
            return new ChatRequest(message, null, null, Map.of());
        }
        
        /**
         * セッション継続用のチャットリクエストを作成
         */
        public static ChatRequest forSession(String message, String sessionId) {
            return new ChatRequest(message, sessionId, null, Map.of());
        }
        
        /**
         * 特定タイプの会話を開始するリクエストを作成
         */
        public static ChatRequest forType(String message, ConversationType type) {
            return new ChatRequest(message, null, type.asString(), Map.of());
        }
        
        /**
         * コンテキスト付きのチャットリクエストを作成
         */
        public static ChatRequest withContext(String message, Map<String, Object> context) {
            return new ChatRequest(message, null, null, context);
        }
    }
    
    /**
     * 会話タイプを表す密封インターフェース
     */
    public sealed interface ConversationType 
        permits ConversationType.Support, ConversationType.Recommendation, ConversationType.Search {
        
        record Support() implements ConversationType {}
        record Recommendation() implements ConversationType {}
        record Search() implements ConversationType {}
        
        static ConversationType fromString(String typeString) {
            return switch (typeString.toUpperCase()) {
                case "SUPPORT" -> new Support();
                case "RECOMMENDATION" -> new Recommendation();
                case "SEARCH" -> new Search();
                default -> throw new IllegalArgumentException("Unknown conversation type: " + typeString);
            };
        }
        
        default String asString() {
            return switch (this) {
                case Support() -> "SUPPORT";
                case Recommendation() -> "RECOMMENDATION";
                case Search() -> "SEARCH";
            };
        }
    }

    /**
     * チャットレスポンスDTO
     * 
     * @param sessionId セッションID
     * @param response レスポンス内容
     * @param responseType レスポンスタイプ（TEXT, PRODUCT_LIST, RECOMMENDATION）
     * @param data 関連データ
     * @param metadata メタデータ
     * @param timestamp タイムスタンプ
     */
    public record ChatResponse(
        String sessionId,
        String response,
        String responseType,
        List<Object> data,
        Map<String, Object> metadata,
        LocalDateTime timestamp
    ) {
        
        public ChatResponse {
            if (data == null) {
                data = List.of();
            }
            if (metadata == null) {
                metadata = Map.of();
            }
            if (timestamp == null) {
                timestamp = LocalDateTime.now();
            }
        }
        
        /**
         * テキストレスポンスを作成
         */
        public static ChatResponse textResponse(String sessionId, String response) {
            return new ChatResponse(sessionId, response, "TEXT", List.of(), Map.of(), LocalDateTime.now());
        }
        
        /**
         * 商品リスト付きレスポンスを作成
         */
        public static ChatResponse withProductList(String sessionId, String response, List<Object> products) {
            return new ChatResponse(sessionId, response, "PRODUCT_LIST", products, Map.of(), LocalDateTime.now());
        }
        
        /**
         * 推薦付きレスポンスを作成
         */
        public static ChatResponse withRecommendation(String sessionId, String response, 
                                                    List<Object> recommendations, Map<String, Object> metadata) {
            return new ChatResponse(sessionId, response, "RECOMMENDATION", recommendations, metadata, LocalDateTime.now());
        }
        
        /**
         * データが含まれているかチェック
         */
        public boolean hasData() {
            return !data.isEmpty();
        }
        
        /**
         * レスポンスタイプを取得（密封インターフェース）
         */
        public ResponseType getResponseTypeEnum() {
            return ResponseType.fromString(responseType);
        }
    }
    
    /**
     * レスポンスタイプを表す密封インターフェース
     */
    public sealed interface ResponseType 
        permits ResponseType.Text, ResponseType.ProductList, ResponseType.Recommendation {
        
        record Text() implements ResponseType {}
        record ProductList() implements ResponseType {}
        record Recommendation() implements ResponseType {}
        
        static ResponseType fromString(String typeString) {
            return switch (typeString.toUpperCase()) {
                case "TEXT" -> new Text();
                case "PRODUCT_LIST" -> new ProductList();
                case "RECOMMENDATION" -> new Recommendation();
                default -> new Text(); // デフォルトはテキスト
            };
        }
        
        default String asString() {
            return switch (this) {
                case Text() -> "TEXT";
                case ProductList() -> "PRODUCT_LIST";
                case Recommendation() -> "RECOMMENDATION";
            };
        }
    }

    /**
     * チャット履歴レスポンスDTO
     * 
     * @param sessionId セッションID
     * @param messages メッセージリスト
     * @param status ステータス
     * @param startedAt 開始時刻
     * @param updatedAt 更新時刻
     */
    public record ChatHistoryResponse(
        String sessionId,
        List<ChatMessageDto> messages,
        String status,
        LocalDateTime startedAt,
        LocalDateTime updatedAt
    ) {
        
        public ChatHistoryResponse {
            if (messages == null) {
                messages = List.of();
            }
        }
        
        /**
         * メッセージ数を取得
         */
        public int getMessageCount() {
            return messages.size();
        }
        
        /**
         * 最新メッセージを取得
         */
        public ChatMessageDto getLatestMessage() {
            return messages.isEmpty() ? null : messages.get(messages.size() - 1);
        }
        
        /**
         * セッションが有効かチェック
         */
        public boolean isActive() {
            return "ACTIVE".equalsIgnoreCase(status);
        }
    }

    /**
     * チャットメッセージDTO
     * 
     * @param messageId メッセージID
     * @param role ロール（USER, ASSISTANT, SYSTEM）
     * @param content メッセージ内容
     * @param metadata メタデータ
     * @param timestamp タイムスタンプ
     */
    public record ChatMessageDto(
        String messageId,
        String role,
        String content,
        Map<String, Object> metadata,
        LocalDateTime timestamp
    ) {
        
        public ChatMessageDto {
            if (metadata == null) {
                metadata = Map.of();
            }
            if (timestamp == null) {
                timestamp = LocalDateTime.now();
            }
        }
        
        /**
         * ユーザーメッセージを作成
         */
        public static ChatMessageDto userMessage(String messageId, String content) {
            return new ChatMessageDto(messageId, "USER", content, Map.of(), LocalDateTime.now());
        }
        
        /**
         * アシスタントメッセージを作成
         */
        public static ChatMessageDto assistantMessage(String messageId, String content) {
            return new ChatMessageDto(messageId, "ASSISTANT", content, Map.of(), LocalDateTime.now());
        }
        
        /**
         * システムメッセージを作成
         */
        public static ChatMessageDto systemMessage(String messageId, String content) {
            return new ChatMessageDto(messageId, "SYSTEM", content, Map.of(), LocalDateTime.now());
        }
        
        /**
         * ロールをチェック
         */
        public boolean isUserMessage() {
            return "USER".equalsIgnoreCase(role);
        }
        
        public boolean isAssistantMessage() {
            return "ASSISTANT".equalsIgnoreCase(role);
        }
        
        public boolean isSystemMessage() {
            return "SYSTEM".equalsIgnoreCase(role);
        }
    }
}
