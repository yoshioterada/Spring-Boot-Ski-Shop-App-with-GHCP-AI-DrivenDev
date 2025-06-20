package com.skishop.auth.exception;

/**
 * Authentication Exception
 * 
 * 認証関連の例外
 */
public class AuthenticationException extends RuntimeException {
    
    public AuthenticationException(String message) {
        super(message);
    }
    
    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
