package com.skishop.ai.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

/**
 * リトライ機能の設定
 * 
 * <p>Spring Retry を有効化してAIサービスのエラーハンドリングを強化</p>
 * 
 * <h3>対象となるエラー:</h3>
 * <ul>
 *   <li>Azure OpenAI API の一時的な接続エラー</li>
 *   <li>レート制限による429エラー</li>
 *   <li>タイムアウトエラー</li>
 *   <li>サービス利用不可（503）エラー</li>
 * </ul>
 * 
 * <p>リトライ設定は各サービスクラスで {@code @Retryable} アノテーションを使用して定義</p>
 * 
 * @since 1.0.0
 * @see org.springframework.retry.annotation.Retryable
 * @see com.skishop.ai.service.EnhancedAiServiceExecutor
 */
@Configuration
@EnableRetry
public class RetryConfig {
    // Spring Retry の基本設定はアノテーションで十分
    // より高度な設定が必要な場合はここでカスタマイズ
    
    /*
     * 将来の拡張例:
     * - カスタムRetryTemplate Bean定義
     * - 指数バックオフ戦略設定
     * - リトライ統計の収集設定
     * - リトライ対象例外の細かい制御
     */
}
