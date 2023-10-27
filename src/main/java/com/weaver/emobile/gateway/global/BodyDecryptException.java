package com.weaver.emobile.gateway.global;

import java.io.Serial;

public class BodyDecryptException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = -5881205170669650956L;

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
