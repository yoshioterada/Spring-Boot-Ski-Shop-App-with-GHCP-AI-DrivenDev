package com.skishop.ai.service;

import com.skishop.ai.dto.ChatMessageRequest;
import com.skishop.ai.dto.ChatMessageResponse;
import com.skishop.ai.entity.ChatConversation;
import com.skishop.ai.entity.ChatMessage;
import com.skishop.ai.exception.AiServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * チャットサービス実装
 * 
 * <p>LangChain4j 1.1.0のAIサービスを使用したチャットボット機能</p>
 * <p>エラーハンドリング・リトライ・フォールバック機能を統合</p>
 * <p>Java 21の最新機能を活用</p>
 * 
 * @since 1.0.0
 */
@Service
public class ChatService {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);
    
    private final CustomerSupportAssistant customerSupportAssistant;
    private final ProductRecommendationAssistant recommendationAssistant;
    private final EnhancedAiServiceExecutor aiServiceExecutor;
    // private final ChatConversationRepository conversationRepository;
    
    /**
     * コンストラクタ
     * 
     * @param customerSupportAssistant カスタマーサポートアシスタント
     * @param recommendationAssistant 商品推奨アシスタント
     * @param aiServiceExecutor AIサービス実行エンジン
     */
    public ChatService(
            CustomerSupportAssistant customerSupportAssistant,
            ProductRecommendationAssistant recommendationAssistant,
            EnhancedAiServiceExecutor aiServiceExecutor) {
        this.customerSupportAssistant = customerSupportAssistant;
        this.recommendationAssistant = recommendationAssistant;
        this.aiServiceExecutor = aiServiceExecutor;
    }
    
    /**
     * Java 21のText Blocks機能を活用したモック商品カタログ（定数として定義）
     */
    private static final String MOCK_PRODUCT_CATALOG = """
            [
              {
                "productId": "ski-001",
                "name": "Rossignol Experience 88 Ti",
                "category": "オールマウンテンスキー",
                "price": 89000,
                "skillLevel": "中級者-上級者",
                "features": ["チタン強化", "安定性", "カービング性能"]
              },
              {
                "productId": "ski-002", 
                "name": "Salomon QST 92",
                "category": "フリーライドスキー",
                "price": 76000,
                "skillLevel": "中級者",
                "features": ["軽量", "パウダー対応", "オールラウンド"]
              }
            ]
            """;
    
    /**
     * Java 21の密封インターフェースを使用した意図分類
     */
    public sealed interface Intent 
        permits Intent.ProductRecommendation, Intent.TechnicalAdvice, 
                Intent.OrderSupport, Intent.GeneralInquiry {
        
        record ProductRecommendation() implements Intent {}
        record TechnicalAdvice() implements Intent {}
        record OrderSupport() implements Intent {}
        record GeneralInquiry() implements Intent {}
        
        /**
         * 文字列から意図を検出
         */
        static Intent detectFromMessage(String message) {
            var lowerMessage = message.toLowerCase();
            
            if (lowerMessage.contains("推奨") || lowerMessage.contains("おすすめ") || 
                lowerMessage.contains("選び方") || lowerMessage.contains("どれがいい")) {
                return new ProductRecommendation();
            } else if (lowerMessage.contains("技術") || lowerMessage.contains("滑り方") || 
                      lowerMessage.contains("コツ") || lowerMessage.contains("上達")) {
                return new TechnicalAdvice();
            } else if (lowerMessage.contains("注文") || lowerMessage.contains("配送") || 
                      lowerMessage.contains("返品") || lowerMessage.contains("交換")) {
                return new OrderSupport();
            } else {
                return new GeneralInquiry();
            }
        }
        
        /**
         * 意図を文字列として取得
         */
        default String asString() {
            return switch (this) {
                case ProductRecommendation() -> "PRODUCT_RECOMMENDATION";
                case TechnicalAdvice() -> "TECHNICAL_ADVICE";
                case OrderSupport() -> "ORDER_SUPPORT";
                case GeneralInquiry() -> "GENERAL_INQUIRY";
            };
        }
        
        /**
         * アクションが必要かどうかを判定
         */
        default boolean requiresAction() {
            return switch (this) {
                case ProductRecommendation(), OrderSupport() -> true;
                case TechnicalAdvice(), GeneralInquiry() -> false;
            };
        }
        
        /**
         * アクションタイプを取得
         */
        default String getActionType() {
            return switch (this) {
                case ProductRecommendation() -> "SHOW_PRODUCTS";
                case OrderSupport() -> "REDIRECT_TO_SUPPORT";
                case TechnicalAdvice(), GeneralInquiry() -> "NONE";
            };
        }
    }
    
    /**
     * チャットメッセージの処理
     */
    public ChatMessageResponse processMessage(ChatMessageRequest request) {
        logger.info("Processing chat message for user: {}", request.userId());
        
        try {
            // Java 21のvar型推論とパターンマッチングを活用
            var userMessage = request.content();
            var intent = Intent.detectFromMessage(userMessage);
            
            // 意図に基づいて適切なAIサービスを呼び出し（エラーハンドリング・リトライ付き）
            var aiResponse = generateResponseWithRetry(userMessage, intent, request);
            
            // レスポンスを構築して返却 - Recordのファクトリメソッドを使用
            return ChatMessageResponse.withConfidence(
                UUID.randomUUID().toString(),
                request.conversationId(),
                aiResponse,
                intent.asString(),
                0.9 // 実際は分析結果に基づく
            );
            
        } catch (AiServiceException e) {
            // AI サービス特有のエラーはそのまま再スロー
            logger.error("AI service error processing chat message: {}", e.getMessage());
            throw e;
            
        } catch (Exception e) {
            logger.error("Unexpected error processing chat message: ", e);
            throw AiServiceException.internalError("チャットメッセージの処理中にエラーが発生しました", e);
        }
    }
    
    /**
     * 意図に基づいてAIレスポンスを生成（Java 21のSwitch Expressionsを活用）
     */
    private String generateResponseWithRetry(String userMessage, Intent intent, ChatMessageRequest request) {
        return switch (intent) {
            case Intent.ProductRecommendation() -> 
                generateProductRecommendationWithRetry(userMessage, request);
                
            case Intent.TechnicalAdvice() -> 
                aiServiceExecutor.executeWithRetry(
                    (text, context) -> customerSupportAssistant.provideTechnicalAdvice(text),
                    userMessage,
                    intent.asString()
                );
                
            case Intent.OrderSupport(), Intent.GeneralInquiry() -> 
                aiServiceExecutor.executeWithRetry(
                    (text, context) -> customerSupportAssistant.chat(text),
                    userMessage,
                    intent.asString()
                );
        };
    }
    
    /**
     * 商品推奨の生成（エラーハンドリング・リトライ付き）
     */
    private String generateProductRecommendationWithRetry(String userMessage, ChatMessageRequest request) {
        // ユーザープロファイルの構築
        var userProfile = buildUserProfile(request);
        
        // AIを使用して推奨を生成（エラーハンドリング・リトライ付き）
        return aiServiceExecutor.executeWithRetry(
            (text, context) -> recommendationAssistant.generateRecommendations(text, userProfile, MOCK_PRODUCT_CATALOG),
            userMessage,
            "PRODUCT_RECOMMENDATION"
        );
    }
    
    /**
     * ユーザープロファイルの構築（Java 21のText Blocks with String formatting）
     */
    private String buildUserProfile(ChatMessageRequest request) {
        // 実際の実装ではユーザー管理サービスから取得
        return """
            {
              "userId": "%s",
              "skillLevel": "中級者",
              "preferences": {
                "budget": "50000-100000",
                "brands": ["Rossignol", "Salomon"],
                "usage": "レジャー"
              },
              "physicalAttributes": {
                "height": "170cm",
                "weight": "65kg"
              }
            }
            """.formatted(request.userId());
    }
    
    /**
     * コンテキスト情報の構築（Java 21のvar型推論を活用）
     */
    private Map<String, Object> buildContext(ChatMessageRequest request) {
        var context = new HashMap<String, Object>();
        context.put("sessionId", request.sessionId());
        context.put("timestamp", LocalDateTime.now());
        context.put("channel", "web-chat");
        return context;
    }
}
