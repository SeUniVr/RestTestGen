package io.resttestgen.implementation.parametervalueprovider.single;

import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.parameter.ParameterLeaf;
import io.resttestgen.core.helper.ExtendedRandom;
import io.resttestgen.core.testing.parametervalueprovider.CountableParameterValueProvider;

import java.util.stream.Collectors;

public class EnumParameterValueProvider extends CountableParameterValueProvider {

    @Override
    public int countAvailableValuesFor(ParameterLeaf parameterLeaf) {
        if (!strict) {
            return parameterLeaf.getEnumValues().size();
        } else {
            return (int) parameterLeaf.getEnumValues().stream().filter(parameterLeaf::isValueCompliant).count();
        }
    }

    @Override
    public Object provideValueFor(ParameterLeaf parameterLeaf) {
        ExtendedRandom random = Environment.getInstance().getRandom();
        if (!strict) {
            return random.nextElement(parameterLeaf.getEnumValues()).orElse(null);
        } else {
            return random.nextElement(parameterLeaf.getEnumValues().stream().filter(parameterLeaf::isValueCompliant)
                    .collect(Collectors.toSet())).orElse(null);
        }
    }
}
