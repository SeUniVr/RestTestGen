package io.resttestgen.core.openapi;

public class InvalidOpenApiException extends RuntimeException {

    public InvalidOpenApiException() {
    }

    public InvalidOpenApiException(String message) {
        super(message);
    }
}
