package io.resttestgen.core.testing.parametervalueprovider;

public class ValueNotAvailableException extends Exception {

    public ValueNotAvailableException() {
        super("The parameter value provider could not provide a value for this parameter.");
    }
}
