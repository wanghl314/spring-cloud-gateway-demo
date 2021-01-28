package com.weaver.emobile.gateway.global;

public class BodyEncryptException extends RuntimeException {
    public BodyEncryptException() {
    }

    public BodyEncryptException(String message) {
        super(message);
    }

    public BodyEncryptException(Throwable cause) {
        super(cause);
    }

    public BodyEncryptException(String message, Throwable cause) {
        super(message, cause);
    }

}
