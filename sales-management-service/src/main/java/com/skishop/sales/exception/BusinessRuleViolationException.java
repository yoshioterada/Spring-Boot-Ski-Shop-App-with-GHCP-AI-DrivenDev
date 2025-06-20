package com.skishop.sales.exception;

/**
 * 業務ルール違反の例外
 */
public final class BusinessRuleViolationException extends SalesException {

    public BusinessRuleViolationException(String message) {
        super(message);
    }

    public BusinessRuleViolationException(String message, Throwable cause) {
        super(message, cause);
    }
}
