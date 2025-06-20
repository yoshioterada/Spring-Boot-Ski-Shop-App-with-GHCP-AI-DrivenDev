package com.skishop.sales.exception;

/**
 * 在庫不足の例外
 */
public final class InsufficientStockException extends SalesException {

    public InsufficientStockException(String message) {
        super(message);
    }

    public InsufficientStockException(String message, Throwable cause) {
        super(message, cause);
    }
}
