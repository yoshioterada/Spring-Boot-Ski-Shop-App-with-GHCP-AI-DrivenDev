package com.skishop.inventory.exception;

/**
 * 重複リソース例外（sealed classを使用）
 */
public final class DuplicateResourceException extends InventoryException {
    
    public DuplicateResourceException(String message) {
        super(message);
    }
    
    public DuplicateResourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
