package com.weaver.emobile.gateway.global;

public class KeyDecryptException extends RuntimeException {
    public KeyDecryptException() {
    }

    public KeyDecryptException(String message) {
        super(message);
    }

    public KeyDecryptException(Throwable cause) {
        super(cause);
    }

    public KeyDecryptException(String message, Throwable cause) {
        super(message, cause);
    }

}
