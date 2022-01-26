package io.resttestgen.core.openapi;

public class CannotParseOpenAPIException extends Exception {

    public CannotParseOpenAPIException() {
    }

    public CannotParseOpenAPIException(String message) {
        super(message);
    }
}
