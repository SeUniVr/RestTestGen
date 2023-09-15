package io.resttestgen.implementation.parametervalueprovider.single;

import io.resttestgen.core.datatype.parameter.Parameter;
import io.resttestgen.core.datatype.parameter.leaves.LeafParameter;
import io.resttestgen.core.testing.parametervalueprovider.CountableParameterValueProvider;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public class DefaultParameterValueProvider extends CountableParameterValueProvider {

    public DefaultParameterValueProvider() {
        setSelfValueSourceClass();
    }

    @Override
    protected Collection<Object> collectValuesFor(LeafParameter leafParameter) {
        Set<Object> values;
        switch (getValueSourceClass()) {
            case SAME_NAME:
                values = Set.of(filterDuplicateValues(collectParametersWithSameName(leafParameter.getName()).stream()
                        .map(Parameter::getDefaultValue)
                        .collect(Collectors.toSet())));
                break;
            case SAME_NORMALIZED_NAME:
                values = Set.of(filterDuplicateValues(collectParametersWithSameNormalizedName(leafParameter.getNormalizedName()).stream()
                        .map(Parameter::getDefaultValue)
                        .collect(Collectors.toSet())));
                break;
            default:
                values = leafParameter.getDefaultValue() == null ? Set.of() : Set.of(leafParameter.getDefaultValue());
        }
        return strict ? filterNonCompliantValues(values, leafParameter) : values;
    }
}
