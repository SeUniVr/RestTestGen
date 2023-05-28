package io.resttestgen.implementation.parametervalueprovider.single;

import io.resttestgen.core.datatype.parameter.leaves.LeafParameter;
import io.resttestgen.core.testing.parametervalueprovider.CountableParameterValueProvider;

public class DefaultParameterValueProvider extends CountableParameterValueProvider {

    @Override
    public int countAvailableValuesFor(LeafParameter leafParameter) {
        if (leafParameter.getDefaultValue() != null) {
            if (!strict || leafParameter.isValueCompliant(leafParameter.getDefaultValue())) {
                return 1;
            }
        }
        return 0;
    }

    @Override
    public Object provideValueFor(LeafParameter leafParameter) {
        return leafParameter.getDefaultValue();
    }
}
