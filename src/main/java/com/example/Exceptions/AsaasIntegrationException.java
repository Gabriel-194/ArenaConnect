package com.example.Exceptions;

public class AsaasIntegrationExeception extends RuntimeException {
    public AsaasIntegrationExeception(String message) {
        super(message);
    }
    public AsaasIntegrationExeception(String message, Throwable cause) {
        super(message, cause);
    }
}
