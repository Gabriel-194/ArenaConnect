package com.example.Exceptions;

public class AsaasIntegrationException extends RuntimeException {
    public AsaasIntegrationException(String message) {
        super(message);
    }
    public AsaasIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
