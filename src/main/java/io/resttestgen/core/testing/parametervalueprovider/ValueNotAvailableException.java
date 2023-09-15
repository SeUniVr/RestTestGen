package io.resttestgen.core.testing.parametervalueprovider;

import io.resttestgen.core.datatype.parameter.Parameter;

public class ValueNotAvailableException extends Exception {

    public ValueNotAvailableException(ParameterValueProvider provider, Parameter parameter) {
        super(provider.getClass().getSimpleName() + " could not provide a value for parameter " + parameter + ".");
    }
}
