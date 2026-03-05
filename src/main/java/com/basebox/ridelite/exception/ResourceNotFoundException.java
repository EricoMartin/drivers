package com.basebox.ridelite.exception;

/**
 * Thrown when a requested resource is not found.
 */
public class ResourceNotFoundException extends BusinessException {
    public ResourceNotFoundException(String resource, String identifier) {
        super(String.format("%s not found with identifier: %s", resource, identifier));
    }
}
