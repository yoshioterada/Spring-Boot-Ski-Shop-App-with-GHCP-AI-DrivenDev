package com.skishop.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * エラーレスポンス DTO
 * REST API のエラー情報を統一形式で返却
 * 
 * Java 21のRecord機能を使用した不変データクラス
 * @param timestamp エラー発生時刻
 * @param status HTTPステータスコード
 * @param error HTTPエラー名
 * @param message ユーザー向けエラーメッセージ
 * @param errorCode システム内部エラーコード
 * @param retryable リトライ可能フラグ
 * @param path リクエストパス
 * @param details 詳細情報（バリデーションエラーなど）
 * @param traceId トレースID（分散トレーシング用）
 */
@JsonInclude(Include.NON_NULL)
public record ErrorResponse(
    LocalDateTime timestamp,
    Integer status,
    String error,
    String message,
    String errorCode,
    Boolean retryable,
    String path,
    Map<String, Object> details,
    String traceId
) {
    
    /**
     * ビルダーパターンの代替として、基本的なエラー情報でインスタンスを作成
     */
    public static ErrorResponse of(Integer status, String error, String message) {
        return new ErrorResponse(
            LocalDateTime.now(),
            status,
            error,
            message,
            null,
            false,
            null,
            null,
            null
        );
    }
    
    /**
     * リトライ可能なエラーを作成
     */
    public static ErrorResponse retryable(Integer status, String error, String message) {
        return new ErrorResponse(
            LocalDateTime.now(),
            status,
            error,
            message,
            null,
            true,
            null,
            null,
            null
        );
    }
    
    /**
     * 詳細情報付きエラーを作成
     */
    public static ErrorResponse withDetails(Integer status, String error, String message, 
                                           Map<String, Object> details) {
        return new ErrorResponse(
            LocalDateTime.now(),
            status,
            error,
            message,
            null,
            false,
            null,
            details,
            null
        );
    }
}
