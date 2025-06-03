package io.resttestgen.core.testing.parametervalueprovider;

import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.NormalizedParameterName;
import io.resttestgen.core.datatype.ParameterName;
import io.resttestgen.core.datatype.parameter.Parameter;
import io.resttestgen.core.datatype.parameter.leaves.LeafParameter;
import io.resttestgen.core.openapi.Operation;
import kotlin.Pair;

import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Parameter value providers that pick values from a deterministic source with a countable number of values.
 */
public abstract class CountableParameterValueProvider extends ParameterValueProvider {

    /** Defines the source class of values:
     * SELF: values are only taken from the single instance of the parameter.
     * SAME_NAME: values can be taken from all parameters in the API with the same name.
     * SAME_NORMALIZED_NAME: values can be taken from all parameters in the API with the same normalized name.
     */
    protected enum ValueSourceClass {SELF, SAME_NAME, SAME_NORMALIZED_NAME}

    private ValueSourceClass valueSourceClass = ValueSourceClass.SELF;

    protected abstract Collection<Object> collectValuesFor(LeafParameter leafParameter);

    public int countAvailableValuesFor(LeafParameter leafParameter) {
        return collectValuesFor(leafParameter).size();
    }

    public Pair<ParameterValueProvider, Object> provideValueFor(LeafParameter leafParameter) throws ValueNotAvailableException {
        Collection<Object> values = collectValuesFor(leafParameter);
        if (values.isEmpty()) {
            throw new ValueNotAvailableException(this, leafParameter);
        } else if (values.size() == 1) {
            return new Pair<>(this, values.stream().findFirst().get());
        }
        return new Pair<>(this, Environment.getInstance().getRandom().nextElement(values).get());
    }

    public void setSelfValueSourceClass() {
        valueSourceClass = ValueSourceClass.SELF;
    }

    public void setSameNameValueSourceClass() {
        valueSourceClass = ValueSourceClass.SAME_NAME;
    }

    public void setSameNormalizedNameValueSourceClass() {
        valueSourceClass = ValueSourceClass.SAME_NORMALIZED_NAME;
    }

    protected ValueSourceClass getValueSourceClass() {
        return valueSourceClass;
    }

    protected Set<LeafParameter> collectParametersWithSameName(ParameterName parameterName) {
        return Environment.getInstance().getOpenAPI().getOperations().stream()
                .map(Operation::getReferenceLeaves).flatMap(Collection::stream)
                .filter(l -> l.getName().equals(parameterName)).collect(Collectors.toSet());
    }

    protected Set<Parameter> collectParametersWithSameNormalizedName(NormalizedParameterName normalizedParameterName) {
        return Environment.getInstance().getOpenAPI().getOperations().stream()
                .map(Operation::getReferenceLeaves).flatMap(Collection::stream)
                .filter(l -> l.getNormalizedName().equals(normalizedParameterName)).collect(Collectors.toSet());
    }

    /**
     * Removes duplicate values from a collection of values. Values are considered duplicate when their string value
     * matches. Also, null values are removed.
     * @param values the input collection of values.
     * @return the filtered set of values.
     */
    protected Collection<Object> filterDuplicateValues(Collection<Object> values) {

        // TODO: implement way to consider referenced parameters

        HashMap<String, Object> valuesMap = new HashMap<>();

        // Values with the same string value will be overwritten
        for (Object value : values) {
            if (value != null) {
                valuesMap.put(value.toString(), value);
            }
        }

        return valuesMap.values();
    }

    /**
     * Filters out values that are not compliant with the provided leaf.
     * @param values a collection of values to filter.
     * @param leafParameter the leaf against which the check should be performed.
     * @return the filtered collection of values.
     */
    protected Collection<Object> filterNonCompliantValues(Collection<Object> values, LeafParameter leafParameter) {
        return values.stream().filter(leafParameter::isValueCompliant).collect(Collectors.toSet());
    }
}
