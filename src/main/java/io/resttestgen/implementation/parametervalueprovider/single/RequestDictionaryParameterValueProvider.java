package io.resttestgen.implementation.parametervalueprovider.single;

import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.parameter.leaves.LeafParameter;
import io.resttestgen.core.dictionary.Dictionary;
import io.resttestgen.core.dictionary.DictionaryEntry;
import io.resttestgen.core.testing.parametervalueprovider.CountableParameterValueProvider;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.stream.Collectors;

public class RequestDictionaryParameterValueProvider extends CountableParameterValueProvider {

    // Get values from global dictionary by default
    private Dictionary requestDictionary = Environment.getInstance().getGlobalRequestDictionary();

    // Remove duplicates by default
    private boolean removeDuplicates = true;

    public RequestDictionaryParameterValueProvider() {
        setSameNormalizedNameValueSourceClass();
    }

    @Override
    protected Collection<Object> collectValuesFor(LeafParameter leafParameter) {
        Set<DictionaryEntry> entries = new HashSet<>(
                requestDictionary.getEntriesByParameterName(leafParameter.getName(), leafParameter.getType()));
        entries.addAll(requestDictionary.getEntriesByNormalizedParameterName(leafParameter.getNormalizedName(), leafParameter.getType()));
        Set<Object> concreteValuesCache = new HashSet<>();
        LinkedList<Object> values = new LinkedList<>();
        for (DictionaryEntry entry : entries) {
            Object concreteValue = entry.getSource().getConcreteValue();
            if (!concreteValuesCache.contains(concreteValue)) {
                concreteValuesCache.add(concreteValue);
                values.add(entry.getSource());
            }
        }
        return strict ? filterNonCompliantValues(values, leafParameter) : values;
    }

    /**
     * Set the dictionary from which the provider picks the value.
     * @param requestDictionary the dictionary from which the provider picks the value.
     */
    public void setRequestDictionary(Dictionary requestDictionary) {
        this.requestDictionary = requestDictionary;
    }

    public boolean isRemoveDuplicates() {
        return removeDuplicates;
    }

    public void setRemoveDuplicates(boolean removeDuplicates) {
        this.removeDuplicates = removeDuplicates;
    }
}
