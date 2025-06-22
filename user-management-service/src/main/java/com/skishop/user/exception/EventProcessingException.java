package com.skishop.user.exception;

/**
 * イベント処理専用の例外クラス
 */
public class EventProcessingException extends RuntimeException {

    public EventProcessingException(String message) {
        super(message);
    }

    public EventProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
