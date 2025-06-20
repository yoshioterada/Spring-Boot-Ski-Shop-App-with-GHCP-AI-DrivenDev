package com.skishop.ai.controller;

import com.skishop.ai.dto.ChatMessageRequest;
import com.skishop.ai.dto.ChatMessageResponse;
import com.skishop.ai.exception.AiServiceException;
import com.skishop.ai.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;

/**
 * チャットボットAPI コントローラー
 * 
 * <p>LangChain4j 1.1.0 + Azure OpenAI を使用したAIチャット機能</p>
 * 
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/chat")
@Tag(name = "Chat API", description = "AIチャットボット関連API")
public class ChatController {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);
    
    private final ChatService chatService;
    
    /**
     * コンストラクタ
     * 
     * @param chatService チャットサービス
     */
    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }
    
    /**
     * チャットメッセージの送信
     */
    @PostMapping("/message")
    @Operation(summary = "チャットメッセージ送信", description = "ユーザーからのメッセージを処理してAIレスポンスを返す")
    public ResponseEntity<ChatMessageResponse> sendMessage(
            @Valid @RequestBody ChatMessageRequest request) {
        
        logger.info("Received chat message from user: {}", request.userId());
        
        // ChatService で既にエラーハンドリングされているため、
        // コントローラーではそのまま結果を返す
        // AiServiceException は GlobalExceptionHandler で処理される
        ChatMessageResponse response = chatService.processMessage(request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 商品推奨チャット
     */
    @PostMapping("/recommend")
    @Operation(summary = "商品推奨チャット", description = "商品推奨に特化したAIチャット")
    public ResponseEntity<ChatMessageResponse> recommendProducts(
            @Valid @RequestBody ChatMessageRequest request) {
        
        logger.info("Processing product recommendation for user: {}", request.userId());
        
        // リクエストに推奨フラグを設定 - recordは不変なので新しいインスタンスを作成
        var contextWithIntent = new HashMap<String, Object>();
        if (request.context() != null) {
            contextWithIntent.putAll(request.context());
        }
        contextWithIntent.put("forcedIntent", "PRODUCT_RECOMMENDATION");
        
        var enhancedRequest = new ChatMessageRequest(
            request.userId(),
            request.content(),
            request.conversationId(),
            request.sessionId(),
            contextWithIntent
        );
        
        ChatMessageResponse response = chatService.processMessage(enhancedRequest);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 技術アドバイスチャット
     */
    @PostMapping("/advice")
    @Operation(summary = "技術アドバイスチャット", description = "スキー技術に関するアドバイスに特化したAIチャット")
    public ResponseEntity<ChatMessageResponse> provideTechnicalAdvice(
            @Valid @RequestBody ChatMessageRequest request) {
        
        logger.info("Processing technical advice for user: {}", request.userId());
        
        // リクエストに技術アドバイスフラグを設定 - recordは不変なので新しいインスタンスを作成
        var contextWithIntent = new HashMap<String, Object>();
        if (request.context() != null) {
            contextWithIntent.putAll(request.context());
        }
        contextWithIntent.put("forcedIntent", "TECHNICAL_ADVICE");
        
        var enhancedRequest = new ChatMessageRequest(
            request.userId(),
            request.content(),
            request.conversationId(),
            request.sessionId(),
            contextWithIntent
        );
        
        ChatMessageResponse response = chatService.processMessage(enhancedRequest);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 会話履歴の取得
     */
    @GetMapping("/conversations/{userId}")
    @Operation(summary = "会話履歴取得", description = "指定されたユーザーの会話履歴を取得")
    public ResponseEntity<List<ChatMessageResponse>> getConversationHistory(
            @PathVariable String userId,
            @RequestParam(defaultValue = "10") int limit) {
        
        logger.info("Getting conversation history for user: {}", userId);
        
        // 実装予定: ChatSessionRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, limit))
        // とChatMessage.findBySessionIdOrderByCreatedAtAsc(sessionId)を組み合わせて履歴取得
        return ResponseEntity.ok(List.of()); // 暫定的に空リストを返却
    }
    
    /**
     * 会話の削除
     */
    @DeleteMapping("/conversations/{conversationId}")
    @Operation(summary = "会話削除", description = "指定された会話を削除")
    public ResponseEntity<Void> deleteConversation(@PathVariable String conversationId) {
        
        logger.info("Deleting conversation: {}", conversationId);
        
        // 実装予定: ChatSessionRepository.deleteById(conversationId) と
        // 関連するChatMessageも CASCADE削除または手動削除
        return ResponseEntity.noContent().build();
    }
    
    /**
     * チャット評価の送信
     */
    @PostMapping("/feedback")
    @Operation(summary = "チャット評価", description = "チャットのユーザー評価を送信")
    public ResponseEntity<Void> submitFeedback(
            @RequestParam String conversationId,
            @RequestParam int rating,
            @RequestParam(required = false) String comment) {
        
        logger.info("Received feedback for conversation: {} with rating: {}", conversationId, rating);
        
        // 実装予定: ChatFeedbackエンティティを作成してリポジトリで保存
        // ChatFeedback.builder().conversationId(conversationId).rating(rating).comment(comment).build()
        return ResponseEntity.ok().build();
    }
}
