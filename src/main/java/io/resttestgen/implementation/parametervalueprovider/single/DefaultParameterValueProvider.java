package io.resttestgen.implementation.parametervalueprovider.single;

import io.resttestgen.core.datatype.parameter.ParameterLeaf;
import io.resttestgen.core.testing.parametervalueprovider.CountableParameterValueProvider;

public class DefaultParameterValueProvider extends CountableParameterValueProvider {

    @Override
    public int countAvailableValuesFor(ParameterLeaf parameterLeaf) {
        if (parameterLeaf.getDefaultValue() != null) {
            if (!strict || parameterLeaf.isValueCompliant(parameterLeaf.getDefaultValue())) {
                return 1;
            }
        }
        return 0;
    }

    @Override
    public Object provideValueFor(ParameterLeaf parameterLeaf) {
        return parameterLeaf.getDefaultValue();
    }
}
