package io.resttestgen.implementation.parametervalueprovider.single;

import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.parameter.leaves.LeafParameter;
import io.resttestgen.core.dictionary.Dictionary;
import io.resttestgen.core.dictionary.DictionaryEntry;
import io.resttestgen.core.testing.parametervalueprovider.CountableParameterValueProvider;

import java.util.*;
import java.util.stream.Collectors;

// TODO: add support for strict mode

public class LastRequestDictionaryParameterValueProvider extends CountableParameterValueProvider {

    // Get values from global dictionary by default
    private Dictionary dictionary = Environment.getInstance().getGlobalRequestDictionary();

    public LastRequestDictionaryParameterValueProvider() {
        setSameNormalizedNameValueSourceClass();
    }

    @Override
    protected Collection<Object> collectValuesFor(LeafParameter leafParameter) {
        Set<DictionaryEntry> entries = new HashSet<>(dictionary.getEntriesByParameterName(leafParameter.getName(), leafParameter.getType()));
        entries.addAll(dictionary.getEntriesByNormalizedParameterName(leafParameter.getNormalizedName(), leafParameter.getType()));

        // Filter dictionary entries by removing those with non-compliant values
        if (isStrict()) {
            entries = entries.stream().filter(e -> leafParameter.isValueCompliant(e.getSource().getConcreteValue())).collect(Collectors.toSet());
        }

        if (entries.isEmpty()) {
            return new LinkedList<>();
        }

        List<DictionaryEntry> orderedEntries = new LinkedList<>(entries);

        orderedEntries.sort(Comparator.comparing(DictionaryEntry::getDiscoveryTime));

        return Set.of(orderedEntries.get(entries.size() - 1).getSource());
    }

    /**
     * Set the dictionary from which the provider picks the value.
     * @param dictionary the dictionary from which the provider picks the value.
     */
    public void setDictionary(Dictionary dictionary) {
        this.dictionary = dictionary;
    }
}
