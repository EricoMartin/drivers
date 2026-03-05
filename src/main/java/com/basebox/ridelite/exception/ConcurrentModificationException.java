package com.basebox.ridelite.exception;

public class ConcurrentModificationException extends BusinessException {
    public ConcurrentModificationException(String message) {
        super(message);
    }
}
