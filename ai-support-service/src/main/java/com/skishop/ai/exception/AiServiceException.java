package com.skishop.ai.exception;

import lombok.Getter;

/**
 * AI サービス関連の例外クラス
 * LangChain4j + Azure OpenAI での統合エラーハンドリング
 */
@Getter
public class AiServiceException extends RuntimeException {
    
    private final String errorCode;
    private final String userFriendlyMessage;
    private final boolean retryable;
    
    /**
     * コンストラクタ
     */
    public AiServiceException(String errorCode, String message, String userFriendlyMessage, boolean retryable) {
        super(message);
        this.errorCode = errorCode;
        this.userFriendlyMessage = userFriendlyMessage;
        this.retryable = retryable;
    }
    
    public AiServiceException(String errorCode, String message, String userFriendlyMessage, Throwable cause, boolean retryable) {
        super(message, cause);
        this.errorCode = errorCode;
        this.userFriendlyMessage = userFriendlyMessage;
        this.retryable = retryable;
    }
    
    /**
     * Azure OpenAI API制限エラー
     */
    public static AiServiceException rateLimitExceeded(String details) {
        return new AiServiceException(
            "RATE_LIMIT_EXCEEDED",
            "Azure OpenAI API rate limit exceeded: " + details,
            "現在アクセスが集中しています。しばらく時間をおいて再度お試しください。",
            true
        );
    }
    
    /**
     * Azure OpenAI接続エラー
     */
    public static AiServiceException connectionFailed(String details, Throwable cause) {
        return new AiServiceException(
            "CONNECTION_FAILED",
            "Failed to connect to Azure OpenAI: " + details,
            "AIサービスに接続できません。ネットワーク接続をご確認ください。",
            cause,
            true
        );
    }
    
    /**
     * 認証エラー
     */
    public static AiServiceException authenticationFailed(String details) {
        return new AiServiceException(
            "AUTHENTICATION_FAILED",
            "Azure OpenAI authentication failed: " + details,
            "認証に失敗しました。システム管理者にお問い合わせください。",
            false
        );
    }
    
    /**
     * 入力検証エラー
     */
    public static AiServiceException invalidInput(String details) {
        return new AiServiceException(
            "INVALID_INPUT",
            "Invalid input provided: " + details,
            "入力内容に問題があります。内容をご確認の上、再度お試しください。",
            false
        );
    }
    
    /**
     * コンテンツフィルターエラー
     */
    public static AiServiceException contentFiltered(String details) {
        return new AiServiceException(
            "CONTENT_FILTERED",
            "Content was filtered by Azure OpenAI: " + details,
            "申し訳ございません。入力内容が適切でない可能性があります。別の表現でお試しください。",
            false
        );
    }
    
    /**
     * 内部サービスエラー
     */
    public static AiServiceException internalError(String details, Throwable cause) {
        return new AiServiceException(
            "INTERNAL_ERROR",
            "Internal AI service error: " + details,
            "システム内部でエラーが発生しました。しばらく時間をおいて再度お試しください。",
            cause,
            true
        );
    }
    
    /**
     * バリデーション失敗エラー
     */
    public static AiServiceException validationFailed(String details) {
        return new AiServiceException(
            "VALIDATION_FAILED",
            "Validation failed: " + details,
            "入力内容に問題があります。内容をご確認の上、再度お試しください。",
            false
        );
    }
    
    /**
     * サービス利用不可エラー
     */
    public static AiServiceException serviceUnavailable(String details) {
        return new AiServiceException(
            "SERVICE_UNAVAILABLE",
            "AI service is unavailable: " + details,
            "AIサービスが一時的に利用できません。しばらく時間をおいて再度お試しください。",
            true
        );
    }
    
    /**
     * クォータ超過エラー
     */
    public static AiServiceException quotaExceeded(String details) {
        return new AiServiceException(
            "QUOTA_EXCEEDED",
            "API quota exceeded: " + details,
            "APIの利用上限に達しました。時間をおいて再度お試しください。",
            true
        );
    }
}
