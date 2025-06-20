package com.skishop.ai.integration;

import com.skishop.ai.dto.ChatMessageRequest;
import com.skishop.ai.dto.ChatMessageResponse;
import com.skishop.ai.exception.AiServiceException;
import com.skishop.ai.service.ChatService;
import com.skishop.ai.service.CustomerSupportAssistant;
import com.skishop.ai.service.EnhancedAiServiceExecutor;
import com.skishop.ai.service.ProductRecommendationAssistant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 拡張エラーハンドリング統合テスト
 * AIサービスのリトライ・フォールバック・例外処理を検証
 */
@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("test")
class EnhancedErrorHandlingIntegrationTest {
    
    @Autowired
    private ChatService chatService;
    
    @MockBean
    private CustomerSupportAssistant customerSupportAssistant;
    
    @MockBean
    private ProductRecommendationAssistant recommendationAssistant;
    
    @Autowired
    private EnhancedAiServiceExecutor aiServiceExecutor;
    
    @Test
    void testSuccessfulChatProcessing() {
        // Given
        when(customerSupportAssistant.chat(anyString()))
                .thenReturn("こんにちは！お手伝いさせていただきます。");
        
        ChatMessageRequest request = createTestRequest("こんにちは");
        
        // When
        ChatMessageResponse response = chatService.processMessage(request);
        
        // Then
        assertNotNull(response);
        assertEquals("こんにちは！お手伝いさせていただきます。", response.getContent());
        assertEquals("GENERAL_INQUIRY", response.getIntent());
        assertNotNull(response.getMessageId());
        assertNotNull(response.getTimestamp());
    }
    
    @Test
    void testProductRecommendationProcessing() {
        // Given
        when(recommendationAssistant.generateRecommendations(anyString(), anyString(), anyString()))
                .thenReturn("おすすめのスキー板をご提案いたします。初心者の方にはSalomon QST 92がおすすめです。");
        
        ChatMessageRequest request = createTestRequest("スキー板のおすすめを教えて");
        
        // When
        ChatMessageResponse response = chatService.processMessage(request);
        
        // Then
        assertNotNull(response);
        assertTrue(response.getContent().contains("おすすめ"));
        assertEquals("PRODUCT_RECOMMENDATION", response.getIntent());
        assertTrue(response.isRequiresAction());
        assertEquals("SHOW_PRODUCTS", response.getActionType());
    }
    
    @Test
    void testTechnicalAdviceProcessing() {
        // Given
        when(customerSupportAssistant.provideTechnicalAdvice(anyString()))
                .thenReturn("スキーの技術向上のためには、まず基本姿勢の確認から始めましょう。");
        
        ChatMessageRequest request = createTestRequest("スキーの滑り方のコツを教えて");
        
        // When
        ChatMessageResponse response = chatService.processMessage(request);
        
        // Then
        assertNotNull(response);
        assertTrue(response.getContent().contains("技術"));
        assertEquals("TECHNICAL_ADVICE", response.getIntent());
    }
    
    @Test
    void testErrorHandlingWithRetry() {
        // Given - 最初の2回は失敗、3回目で成功
        when(customerSupportAssistant.chat(anyString()))
                .thenThrow(new RuntimeException("Connection timeout"))
                .thenThrow(new RuntimeException("Rate limit exceeded"))
                .thenReturn("リトライ後の正常なレスポンスです。");
        
        ChatMessageRequest request = createTestRequest("テストメッセージ");
        
        // When
        ChatMessageResponse response = chatService.processMessage(request);
        
        // Then
        assertNotNull(response);
        assertEquals("リトライ後の正常なレスポンスです。", response.getContent());
        
        // リトライが3回実行されたことを確認
        verify(customerSupportAssistant, times(3)).chat(anyString());
    }
    
    @Test
    void testRateLimitExceptionHandling() {
        // Given
        when(customerSupportAssistant.chat(anyString()))
                .thenThrow(new RuntimeException("Rate limit exceeded"));
        
        ChatMessageRequest request = createTestRequest("テストメッセージ");
        
        // When & Then
        AiServiceException exception = assertThrows(AiServiceException.class, () -> {
            chatService.processMessage(request);
        });
        
        assertEquals("RATE_LIMIT_EXCEEDED", exception.getErrorCode());
        assertTrue(exception.isRetryable());
        assertTrue(exception.getUserFriendlyMessage().contains("アクセスが集中"));
    }
    
    @Test
    void testInvalidInputHandling() {
        // Given
        ChatMessageRequest request = createTestRequest("");
        
        // When & Then
        AiServiceException exception = assertThrows(AiServiceException.class, () -> {
            chatService.processMessage(request);
        });
        
        assertEquals("INVALID_INPUT", exception.getErrorCode());
        assertFalse(exception.isRetryable());
    }
    
    @Test
    void testInputLengthValidation() {
        // Given - 長すぎる入力
        String longInput = "a".repeat(10000);
        ChatMessageRequest request = createTestRequest(longInput);
        
        // When & Then
        AiServiceException exception = assertThrows(AiServiceException.class, () -> {
            chatService.processMessage(request);
        });
        
        assertEquals("INVALID_INPUT", exception.getErrorCode());
        assertTrue(exception.getMessage().contains("Input text too long"));
    }
    
    @Test
    void testFallbackAfterMaxRetries() {
        // Given - すべてのリトライが失敗
        when(customerSupportAssistant.chat(anyString()))
                .thenThrow(new RuntimeException("Persistent connection error"));
        
        ChatMessageRequest request = createTestRequest("テストメッセージ");
        
        // When & Then
        AiServiceException exception = assertThrows(AiServiceException.class, () -> {
            chatService.processMessage(request);
        });
        
        // 最大リトライ回数が実行されたことを確認
        verify(customerSupportAssistant, times(3)).chat(anyString());
        assertTrue(exception.getMessage().contains("connection error"));
    }
    
    /**
     * テスト用のリクエストを作成
     */
    private ChatMessageRequest createTestRequest(String content) {
        ChatMessageRequest request = new ChatMessageRequest();
        request.setUserId("test-user-001");
        request.setConversationId("test-conversation-001");
        request.setSessionId("test-session-001");
        request.setContent(content);
        
        Map<String, Object> context = new HashMap<>();
        context.put("channel", "web");
        context.put("userAgent", "test-agent");
        request.setContext(context);
        
        return request;
    }
}
