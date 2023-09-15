package io.resttestgen.implementation.parametervalueprovider.single;

import io.resttestgen.core.datatype.parameter.Parameter;
import io.resttestgen.core.datatype.parameter.leaves.LeafParameter;
import io.resttestgen.core.testing.parametervalueprovider.CountableParameterValueProvider;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public class ExamplesParameterValueProvider extends CountableParameterValueProvider {

    public ExamplesParameterValueProvider() {
        setSelfValueSourceClass();
    }

    @Override
    protected Collection<Object> collectValuesFor(LeafParameter leafParameter) {
        Set<Object> values;
        switch (getValueSourceClass()) {
            case SAME_NAME:
                values = collectParametersWithSameName(leafParameter.getName()).stream()
                        .map(Parameter::getExamples)
                        .flatMap(Collection::stream)
                        .collect(Collectors.toSet());
                break;
            case SAME_NORMALIZED_NAME:
                values = collectParametersWithSameNormalizedName(leafParameter.getNormalizedName()).stream()
                        .map(Parameter::getExamples)
                        .flatMap(Collection::stream)
                        .collect(Collectors.toSet());
                break;
            default:
                values = leafParameter.getExamples();
        }
        return strict ? filterNonCompliantValues(values, leafParameter) : values;
    }
}
