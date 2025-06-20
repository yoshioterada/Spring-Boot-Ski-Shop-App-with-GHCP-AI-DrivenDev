package com.skishop.inventory.exception;

/**
 * 在庫不足例外（sealed classを使用）
 */
public final class InsufficientStockException extends InventoryException {
    
    public InsufficientStockException(String message) {
        super(message);
    }
    
    public InsufficientStockException(String message, Throwable cause) {
        super(message, cause);
    }
}
