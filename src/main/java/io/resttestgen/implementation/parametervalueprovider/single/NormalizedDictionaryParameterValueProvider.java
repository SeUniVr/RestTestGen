package io.resttestgen.implementation.parametervalueprovider.single;

import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.parameter.leaves.LeafParameter;
import io.resttestgen.core.dictionary.Dictionary;
import io.resttestgen.core.dictionary.DictionaryEntry;
import io.resttestgen.core.helper.ExtendedRandom;
import io.resttestgen.core.testing.parametervalueprovider.CountableParameterValueProvider;

import java.util.Optional;
import java.util.stream.Collectors;

public class NormalizedDictionaryParameterValueProvider extends CountableParameterValueProvider {

    // Get values from global dictionary by default
    private Dictionary dictionary = Environment.getInstance().getGlobalDictionary();

    @Override
    public int countAvailableValuesFor(LeafParameter leafParameter) {
        if (!strict) {
            return dictionary.getEntriesByNormalizedParameterName(leafParameter.getNormalizedName(), leafParameter.getType()).size();
        } else {
            return (int) dictionary.getEntriesByNormalizedParameterName(leafParameter.getNormalizedName(), leafParameter.getType())
                    .stream().filter(e -> leafParameter.isValueCompliant(e.getValue())).count();
        }
    }

    @Override
    public Object provideValueFor(LeafParameter leafParameter) {
        ExtendedRandom random = Environment.getInstance().getRandom();
        Optional<DictionaryEntry> entry;
        if (!strict) {
            entry = random.nextElement(dictionary.getEntriesByNormalizedParameterName(leafParameter.getNormalizedName(),
                    leafParameter.getType()));
        } else {
            entry = random.nextElement(dictionary.getEntriesByNormalizedParameterName(leafParameter.getNormalizedName(),
                    leafParameter.getType()).stream().filter(e -> leafParameter.isValueCompliant(e.getValue()))
                    .collect(Collectors.toSet()));
        }
        return entry.map(DictionaryEntry::getSource).orElse(null);
    }

    /**
     * Set the dictionary from which the provider picks the value.
     * @param dictionary the dictionary from which the provider picks the value.
     */
    public void setDictionary(Dictionary dictionary) {
        this.dictionary = dictionary;
    }
}
