package com.weaver.emobile.gateway.global;

import java.io.Serial;

public class BodyEncryptException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 548143009856171479L;

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
