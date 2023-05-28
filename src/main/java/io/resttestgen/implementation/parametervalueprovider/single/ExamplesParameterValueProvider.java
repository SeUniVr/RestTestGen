package io.resttestgen.implementation.parametervalueprovider.single;

import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.parameter.leaves.LeafParameter;
import io.resttestgen.core.helper.ExtendedRandom;
import io.resttestgen.core.testing.parametervalueprovider.CountableParameterValueProvider;

import java.util.stream.Collectors;

public class ExamplesParameterValueProvider extends CountableParameterValueProvider {

    @Override
    public int countAvailableValuesFor(LeafParameter leafParameter) {
        if (!strict) {
            return leafParameter.getExamples().size();
        } else {
            return (int) leafParameter.getExamples().stream().filter(leafParameter::isValueCompliant).count();
        }
    }

    @Override
    public Object provideValueFor(LeafParameter leafParameter) {
        ExtendedRandom random = Environment.getInstance().getRandom();
        if (!strict) {
            return random.nextElement(leafParameter.getExamples()).orElse(null);
        } else {
            return random.nextElement(leafParameter.getExamples().stream().filter(leafParameter::isValueCompliant)
                    .collect(Collectors.toSet())).orElse(null);
        }
    }
}
