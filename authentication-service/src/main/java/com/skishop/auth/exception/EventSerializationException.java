package com.skishop.auth.exception;

/**
 * イベントシリアライゼーション時の例外
 */
public class EventSerializationException extends RuntimeException {
    
    public EventSerializationException(String message) {
        super(message);
    }
    
    public EventSerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
