package io.resttestgen.core.testing.parametervalueprovider;

import io.resttestgen.core.datatype.parameter.ParameterLeaf;

/**
 * Given a parameter, the parameter value provider provides a value for that parameter. Example of parameter value
 * providers are random generators, examples, default values, dictionaries
 */
public interface ParameterValueProvider {

    //public int countAvailableValuesFor(ParameterLeaf parameterLeaf);

    Object provideValueFor(ParameterLeaf parameterLeaf);
}
