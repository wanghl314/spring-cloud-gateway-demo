package com.weaver.emobile.gateway.global;

import java.io.Serial;

public class KeyDecryptException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = -5009935888823392258L;

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
