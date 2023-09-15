package io.resttestgen.core.testing.parametervalueprovider;

import io.resttestgen.core.datatype.parameter.leaves.LeafParameter;
import kotlin.Pair;

/**
 * Given a parameter, the parameter value provider provides a value for that parameter. Example of parameter value
 * providers are random generators, examples, default values, dictionaries
 */
public abstract class ParameterValueProvider {

    protected boolean strict = false;

    public abstract Pair<ParameterValueProvider, Object> provideValueFor(LeafParameter leafParameter) throws ValueNotAvailableException;

    public boolean isStrict() {
        return strict;
    }

    public void setStrict(boolean strict) {
        this.strict = strict;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }
}
