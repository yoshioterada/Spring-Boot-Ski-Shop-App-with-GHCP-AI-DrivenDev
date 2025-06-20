package com.skishop.ai.service;

import com.skishop.ai.dto.ChatMessageRequest;
import com.skishop.ai.dto.ChatMessageResponse;
import com.skishop.ai.exception.AiServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * AI サービス実行基盤
 * 
 * <p>Azure OpenAI + LangChain4j のエラーハンドリング・リトライ・フォールバック機能を提供</p>
 * <p>Java 21の最新機能を活用</p>
 * 
 * @since 1.0.0
 */
@Service
public class EnhancedAiServiceExecutor {
    
    private static final Logger logger = LoggerFactory.getLogger(EnhancedAiServiceExecutor.class);
    private static final int MAX_INPUT_LENGTH = 8000;
    private static final Duration AI_TIMEOUT = Duration.ofSeconds(30);
    
    /**
     * Java 21のText Blocks機能を活用したフォールバックメッセージ
     */
    private static final String FALLBACK_MESSAGE = """
        申し訳ございません。現在AIサービスが一時的に利用できません。
        しばらく時間をおいて再度お試しください。
        
        お急ぎの場合は、以下の方法でお問い合わせください：
        - 電話: 0120-XXX-XXX (平日 9:00-18:00)
        - メール: support@skishop.com
        """;
    
    /**
     * Java 21の密封インターフェースを使用したエラータイプ分類
     */
    public sealed interface ErrorType 
        permits ErrorType.ConnectionError, ErrorType.TimeoutError, 
                ErrorType.ValidationError, ErrorType.ServiceError {
        
        record ConnectionError(String message, Throwable cause) implements ErrorType {}
        record TimeoutError(Duration timeout) implements ErrorType {}
        record ValidationError(String field, String issue) implements ErrorType {}
        record ServiceError(String service, String errorCode) implements ErrorType {}
        
        /**
         * エラータイプに基づいてAiServiceExceptionを作成
         */
        default AiServiceException toAiServiceException() {
            if (this instanceof ConnectionError ce) {
                return AiServiceException.connectionFailed(ce.message(), ce.cause());
            } else if (this instanceof TimeoutError te) {
                return AiServiceException.connectionFailed(
                    "Request timeout after " + te.timeout().toSeconds() + " seconds", null);
            } else if (this instanceof ValidationError ve) {
                return AiServiceException.validationFailed("Validation failed for " + ve.field() + ": " + ve.issue());
            } else if (this instanceof ServiceError se) {
                return AiServiceException.serviceUnavailable(
                    "Service " + se.service() + " error: " + se.errorCode());
            }
            throw new IllegalStateException("Unknown error type: " + this.getClass());
        }
    }
    
    /**
     * リトライ付きAIサービス実行
     * 
     * @param aiServiceCall AI サービス呼び出し関数
     * @param inputText 入力テキスト
     * @param context コンテキスト情報
     * @return AI レスポンス
     */
    @Retryable(
        retryFor = {AiServiceException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 5000)
    )
    public String executeWithRetry(
            AiServiceCall aiServiceCall,
            String inputText,
            String context) {
        
        logger.debug("Executing AI service call with input length: {}", inputText.length());
        
        try {
            // 入力検証（Java 21のパターンマッチング活用）
            validateInput(inputText);
            
            // タイムアウト付き実行（Java 21のvar型推論活用）
            var future = CompletableFuture.supplyAsync(() -> {
                try {
                    return aiServiceCall.execute(inputText, context);
                } catch (Exception e) {
                    throw mapToAiServiceException(e);
                }
            });
            
            var result = future.get(AI_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
            
            // レスポンス検証
            validateResponse(result);
            
            logger.debug("AI service call completed successfully, response length: {}", result.length());
            return result;
            
        } catch (java.util.concurrent.TimeoutException e) {
            logger.error("AI service call timed out after {} seconds", AI_TIMEOUT.toSeconds());
            var timeoutError = new ErrorType.TimeoutError(AI_TIMEOUT);
            throw timeoutError.toAiServiceException();
            
        } catch (Exception e) {
            // Java 21のinstanceof パターンマッチング
            if (e instanceof AiServiceException aiEx) {
                throw aiEx;
            }
            throw mapToAiServiceException(e);
        }
    }
    
    /**
     * フォールバック処理
     */
    @Recover
    public String fallback(AiServiceException ex, AiServiceCall aiServiceCall, String inputText, String context) {
        logger.error("AI service call failed after retries, falling back. Error: {}", ex.getMessage());
        
        // メトリクス記録
        recordFailureMetrics(ex);
        
        // 基本的なフォールバックメッセージを返す
        return FALLBACK_MESSAGE;
    }
    
    /**
     * 入力検証（Java 21の機能を活用）
     */
    private void validateInput(String inputText) {
        if (inputText == null || inputText.isBlank()) {
            var error = new ErrorType.ValidationError("inputText", "cannot be null or blank");
            throw error.toAiServiceException();
        }
        
        if (inputText.length() > MAX_INPUT_LENGTH) {
            var error = new ErrorType.ValidationError("inputText", 
                "exceeds maximum length of " + MAX_INPUT_LENGTH + " characters");
            throw error.toAiServiceException();
        }
    }
    
    /**
     * レスポンス検証
     */
    private void validateResponse(String response) {
        if (response == null || response.isBlank()) {
            var error = new ErrorType.ServiceError("AI Service", "EMPTY_RESPONSE");
            throw error.toAiServiceException();
        }
    }
    
    /**
     * 例外マッピング（Java 21のSwitch Expressionsを活用）
     */
    private AiServiceException mapToAiServiceException(Exception e) {
        if (e instanceof IllegalArgumentException ex) {
            return AiServiceException.validationFailed(ex.getMessage());
        } else if (e instanceof java.net.ConnectException ex) {
            return AiServiceException.connectionFailed("Connection failed", ex);
        } else if (e instanceof java.net.SocketTimeoutException ex) {
            return AiServiceException.connectionFailed("Socket timeout", ex);
        } else if (e instanceof RuntimeException ex && ex.getMessage() != null && 
                   ex.getMessage().contains("rate limit")) {
            return AiServiceException.rateLimitExceeded("API rate limit exceeded");
        } else if (e instanceof RuntimeException ex && ex.getMessage() != null && 
                   ex.getMessage().contains("quota")) {
            return AiServiceException.quotaExceeded("API quota exceeded");
        } else {
            return AiServiceException.internalError("Unexpected error: " + e.getMessage(), e);
        }
    }
    
    /**
     * 失敗メトリクス記録
     */
    private void recordFailureMetrics(AiServiceException ex) {
        // 実際の実装では Micrometer などのメトリクスライブラリを使用
        logger.warn("AI service failure metrics - Error Type: {}, Error Code: {}, Retryable: {}", 
                ex.getClass().getSimpleName(),
                ex.getErrorCode(),
                ex.isRetryable());
    }
    
    /**
     * AI サービス呼び出し関数型インターフェース
     */
    @FunctionalInterface
    public interface AiServiceCall {
        String execute(String inputText, String context) throws Exception;
    }
}
