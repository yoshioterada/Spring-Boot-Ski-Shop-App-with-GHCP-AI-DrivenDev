package com.skishop.frontend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Arrays;
import java.util.UUID;

/**
 * AIサポートサービスとの連携を担当するサービスクラス
 */
@Service
public class AiChatService {

    private static final Logger log = LoggerFactory.getLogger(AiChatService.class);

    @Value("${app.ai-support-service.base-url:http://localhost:8084}")
    private String aiServiceBaseUrl;

    private final RestTemplate restTemplate;

    public AiChatService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * AIサポートサービスにメッセージを送信
     */
    public Map<String, Object> sendMessage(String userId, String message, String conversationId) {
        return sendMessage(userId, message, conversationId, null);
    }

    /**
     * AIサポートサービスにメッセージを送信（セッションID付き）
     */
    public Map<String, Object> sendMessage(String userId, String message, String conversationId, String sessionId) {
        try {
            String url = aiServiceBaseUrl + "/api/v1/chat/message";
            
            // ChatMessageRequest形式でリクエストボディ構築
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("userId", userId != null ? userId : generateTempUserId());
            requestBody.put("content", message);  // "message"ではなく"content"を使用
            requestBody.put("conversationId", conversationId);
            requestBody.put("sessionId", sessionId);
            
            // コンテキスト情報を追加
            Map<String, Object> context = new HashMap<>();
            context.put("channel", "web-chat");
            context.put("timestamp", System.currentTimeMillis());
            context.put("userAgent", "frontend-service");
            requestBody.put("context", context);

            // HTTPヘッダー設定
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            // AIサポートサービス呼び出し
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url, 
                HttpMethod.POST, 
                request, 
                (Class<Map<String, Object>>) (Class<?>) Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // ChatMessageResponse形式のレスポンスを処理
                Map<String, Object> responseBody = response.getBody();
                Map<String, Object> frontendResponse = new HashMap<>();
                
                // フロントエンド向けに変換
                frontendResponse.put("message", responseBody.get("content"));
                frontendResponse.put("conversationId", responseBody.get("conversationId"));
                frontendResponse.put("messageId", responseBody.get("messageId"));
                frontendResponse.put("timestamp", responseBody.get("timestamp"));
                frontendResponse.put("intent", responseBody.get("intent"));
                frontendResponse.put("confidence", responseBody.get("confidence"));
                frontendResponse.put("context", responseBody.get("context"));
                
                return frontendResponse;
            } else {
                log.warn("AI service returned non-successful status: {}", response.getStatusCode());
                return createFallbackResponse(message);
            }

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("HTTP error calling AI service: {} - {}", e.getStatusCode(), e.getMessage());
            return createFallbackResponse(message);
        } catch (Exception e) {
            log.error("Unexpected error calling AI service", e);
            return createFallbackResponse(message);
        }
    }

    /**
     * ユーザーの会話履歴を取得
     */
    public Map<String, Object> getConversations(String userId) {
        try {
            String url = aiServiceBaseUrl + "/api/v1/chat/conversations/" + userId;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));

            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url, 
                HttpMethod.GET, 
                request, 
                (Class<Map<String, Object>>) (Class<?>) Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return (Map<String, Object>) response.getBody();
            } else {
                log.warn("AI service returned non-successful status for conversations: {}", response.getStatusCode());
                return createEmptyConversationsResponse();
            }

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("HTTP error getting conversations from AI service: {}", e.getMessage());
            return createEmptyConversationsResponse();
        } catch (Exception e) {
            log.error("Unexpected error getting conversations from AI service", e);
            return createEmptyConversationsResponse();
        }
    }

    /**
     * AIサービスが利用できない場合のフォールバック応答
     */
    private Map<String, Object> createFallbackResponse(String userMessage) {
        Map<String, Object> response = new HashMap<>();
        response.put("conversationId", generateTempConversationId());
        response.put("messageId", generateTempMessageId());
        response.put("timestamp", java.time.LocalDateTime.now().toString());
        response.put("content", generateFallbackText(userMessage)); // "content"フィールドを使用
        response.put("intent", "FALLBACK");
        response.put("confidence", 0.5);
        
        Map<String, Object> context = new HashMap<>();
        context.put("conversationState", "FALLBACK_MODE");
        context.put("serviceFallback", true);
        response.put("context", context);
        
        // フロントエンド互換性のため"message"フィールドも追加
        response.put("message", generateFallbackText(userMessage));

        return response;
    }

    /**
     * フォールバック用のテキスト生成
     */
    private String generateFallbackText(String userMessage) {
        String lowerMessage = userMessage.toLowerCase();
        
        if (lowerMessage.contains("スキー")) {
            return "スキー用品についてご質問ですね！申し訳ございませんが、現在AIアシスタントが一時的に利用できません。スキー商品ページから直接商品をご覧いただけます。";
        } else if (lowerMessage.contains("スノーボード")) {
            return "スノーボード用品についてお答えしたいのですが、現在システムメンテナンス中です。スノーボード商品ページをご覧いただくか、お電話でお問い合わせください。";
        } else if (lowerMessage.contains("サイズ")) {
            return "サイズ選びについては、各商品ページにサイズガイドを掲載しております。詳しくは店舗スタッフにお問い合わせください。";
        } else if (lowerMessage.contains("価格") || lowerMessage.contains("値段")) {
            return "価格については、商品ページで最新の価格をご確認いただけます。セール情報なども随時更新しております。";
        } else {
            return "ご質問ありがとうございます。現在AIアシスタントが一時的にご利用いただけませんが、商品ページやお問い合わせフォームをご利用ください。";
        }
    }

    /**
     * 空の会話履歴レスポンス
     */
    private Map<String, Object> createEmptyConversationsResponse() {
        Map<String, Object> response = new HashMap<>();
        response.put("conversations", Arrays.asList());
        response.put("totalCount", 0);
        response.put("serviceFallback", true);
        return response;
    }

    /**
     * 一時的なユーザーID生成
     */
    private String generateTempUserId() {
        return "guest-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 一時的な会話ID生成
     */
    private String generateTempConversationId() {
        return "conv-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 一時的なメッセージID生成
     */
    private String generateTempMessageId() {
        return "msg-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
