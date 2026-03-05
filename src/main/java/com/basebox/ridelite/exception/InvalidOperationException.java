package com.basebox.ridelite.exception;

/**
 * Thrown when business rules are violated.
 */
public class InvalidOperationException extends BusinessException {
    public InvalidOperationException(String message) {
        super(message);
    }
}
