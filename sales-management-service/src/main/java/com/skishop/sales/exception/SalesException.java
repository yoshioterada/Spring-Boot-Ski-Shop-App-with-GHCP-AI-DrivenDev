package com.skishop.sales.exception;

/**
 * 販売業務例外の基底クラス
 * Java 21のSealed Classを使用して例外階層を明確に定義
 */
public sealed class SalesException extends RuntimeException 
    permits InvalidOrderStateException, BusinessRuleViolationException, InsufficientStockException {
    
    public SalesException(String message) {
        super(message);
    }

    public SalesException(String message, Throwable cause) {
        super(message, cause);
    }
}
