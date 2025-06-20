package com.skishop.inventory.exception;

/**
 * 在庫管理例外基底クラス（Java 21 sealed class使用）
 */
public sealed class InventoryException extends RuntimeException
    permits ResourceNotFoundException, InsufficientStockException, DuplicateResourceException {
    
    public InventoryException(String message) {
        super(message);
    }
    
    public InventoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
