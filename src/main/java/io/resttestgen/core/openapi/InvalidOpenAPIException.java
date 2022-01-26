package io.resttestgen.core.openapi;

public class InvalidOpenAPIException extends RuntimeException {

    public InvalidOpenAPIException() {
    }

    public InvalidOpenAPIException(String message) {
        super(message);
    }
}
