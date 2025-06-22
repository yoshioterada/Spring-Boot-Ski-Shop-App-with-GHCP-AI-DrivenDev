package com.skishop.auth.exception;

/**
 * イベント配信時の例外
 */
public class EventPublishingException extends RuntimeException {
    
    public EventPublishingException(String message) {
        super(message);
    }
    
    public EventPublishingException(String message, Throwable cause) {
        super(message, cause);
    }
}
