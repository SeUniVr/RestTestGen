package io.resttestgen.implementation.parametervalueprovider.single;

import io.resttestgen.core.datatype.parameter.ParameterLeaf;
import io.resttestgen.core.testing.parametervalueprovider.ParameterValueProvider;

/**
 * Generates a random value for the given parameter.
 */
public class RandomParameterValueProvider implements ParameterValueProvider {

    @Override
    public Object provideValueFor(ParameterLeaf parameterLeaf) {
        // FIXME: move here generation of value
        return parameterLeaf.generateCompliantValue();
    }
}
