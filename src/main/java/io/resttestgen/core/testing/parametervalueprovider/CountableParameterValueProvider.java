package io.resttestgen.core.testing.parametervalueprovider;

import io.resttestgen.core.datatype.parameter.ParameterLeaf;

/**
 * Parameter value providers that pick values from a deterministic source with a countable number of values.
 */
public interface CountableParameterValueProvider extends ParameterValueProvider {

    int countAvailableValuesFor(ParameterLeaf parameterLeaf);
}
