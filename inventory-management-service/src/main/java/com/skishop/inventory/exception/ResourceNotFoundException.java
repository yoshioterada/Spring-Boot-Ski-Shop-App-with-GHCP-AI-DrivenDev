package com.skishop.inventory.exception;

/**
 * リソースが見つからない例外（sealed classを使用）
 */
public final class ResourceNotFoundException extends InventoryException {
    
    public ResourceNotFoundException(String message) {
        super(message);
    }
    
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
