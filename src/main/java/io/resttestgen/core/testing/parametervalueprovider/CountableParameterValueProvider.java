package io.resttestgen.core.testing.parametervalueprovider;

import io.resttestgen.core.datatype.parameter.ParameterLeaf;

/**
 * Parameter value providers that pick values from a deterministic source with a countable number of values.
 */
public abstract class CountableParameterValueProvider extends ParameterValueProvider {

    public abstract int countAvailableValuesFor(ParameterLeaf parameterLeaf);
}
