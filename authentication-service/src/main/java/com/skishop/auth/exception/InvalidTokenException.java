package com.skishop.auth.exception;

/**
 * Invalid Token Exception
 * 
 * 無効なトークンの例外
 */
public class InvalidTokenException extends RuntimeException {
    
    public InvalidTokenException(String message) {
        super(message);
    }
    
    public InvalidTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
