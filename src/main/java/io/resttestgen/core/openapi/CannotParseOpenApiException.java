package io.resttestgen.core.openapi;

public class CannotParseOpenApiException extends Exception {

    public CannotParseOpenApiException() {
    }

    public CannotParseOpenApiException(String message) {
        super(message);
    }
}
