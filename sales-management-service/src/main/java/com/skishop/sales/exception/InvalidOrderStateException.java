package com.skishop.sales.exception;

/**
 * 注文状態が無効な場合の例外
 * SalesExceptionのSealed Class階層の一部として定義
 */
public final class InvalidOrderStateException extends SalesException {

    public InvalidOrderStateException(String message) {
        super(message);
    }

    public InvalidOrderStateException(String message, Throwable cause) {
        super(message, cause);
    }
}
