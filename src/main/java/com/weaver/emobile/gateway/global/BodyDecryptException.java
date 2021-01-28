package com.weaver.emobile.gateway.global;

public class BodyDecryptException extends RuntimeException {

    public BodyDecryptException() {
    }

    public BodyDecryptException(String message) {
        super(message);
    }

    public BodyDecryptException(Throwable cause) {
        super(cause);
    }

    public BodyDecryptException(String message, Throwable cause) {
        super(message, cause);
    }

}
