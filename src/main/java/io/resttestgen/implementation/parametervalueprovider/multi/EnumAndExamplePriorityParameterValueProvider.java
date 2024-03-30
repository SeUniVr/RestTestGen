package io.resttestgen.implementation.parametervalueprovider.multi;

import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.parameter.leaves.LeafParameter;
import io.resttestgen.core.helper.ExtendedRandom;
import io.resttestgen.core.testing.parametervalueprovider.CountableParameterValueProvider;
import io.resttestgen.core.testing.parametervalueprovider.ParameterValueProvider;
import io.resttestgen.core.testing.parametervalueprovider.ParameterValueProviderCachedFactory;
import io.resttestgen.core.testing.parametervalueprovider.ValueNotAvailableException;
import io.resttestgen.implementation.parametervalueprovider.ParameterValueProviderType;
import io.resttestgen.implementation.parametervalueprovider.single.*;
import kotlin.Pair;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class EnumAndExamplePriorityParameterValueProvider extends ParameterValueProvider {

    final ExtendedRandom random = Environment.getInstance().getRandom();

    @Override
    public Pair<ParameterValueProvider, Object> provideValueFor(LeafParameter leafParameter) throws ValueNotAvailableException {

        // If the leaf is an enum, return a random enum value
        EnumParameterValueProvider enumParameterValueProvider = (EnumParameterValueProvider) ParameterValueProviderCachedFactory.getParameterValueProvider(ParameterValueProviderType.ENUM);
        enumParameterValueProvider.setStrict(this.strict);
        ExamplesParameterValueProvider examplesParameterValueProvider = (ExamplesParameterValueProvider) ParameterValueProviderCachedFactory.getParameterValueProvider(ParameterValueProviderType.EXAMPLES);
        examplesParameterValueProvider.setStrict(this.strict);

        int numEnums = enumParameterValueProvider.countAvailableValuesFor(leafParameter);
        int numExamples = examplesParameterValueProvider.countAvailableValuesFor(leafParameter);

        if (numEnums + numExamples > 0 && random.nextInt(1, 10) <= 8) {
            if (random.nextInt(numEnums + numExamples) < numEnums) {
                return enumParameterValueProvider.provideValueFor(leafParameter);
            } else {
                return examplesParameterValueProvider.provideValueFor(leafParameter);
            }
        }

        ExtendedRandom random = Environment.getInstance().getRandom();

        Set<ParameterValueProvider> providers = new HashSet<>();

        // Random providers are always available
        providers.add(ParameterValueProviderCachedFactory.getParameterValueProvider(ParameterValueProviderType.RANDOM));
        providers.add(ParameterValueProviderCachedFactory.getParameterValueProvider(ParameterValueProviderType.NARROW_RANDOM));

        // List of candidate providers, that will be used only if they have values available
        Set<CountableParameterValueProvider> candidateProviders = new HashSet<>();
        candidateProviders.add((DefaultParameterValueProvider) ParameterValueProviderCachedFactory.getParameterValueProvider(ParameterValueProviderType.DEFAULT));
        ResponseDictionaryParameterValueProvider responseDictionaryParameterValueProvider = (ResponseDictionaryParameterValueProvider) ParameterValueProviderCachedFactory.getParameterValueProvider(ParameterValueProviderType.RESPONSE_DICTIONARY);
        candidateProviders.add(responseDictionaryParameterValueProvider);
        LastResponseDictionaryParameterValueProvider lastResponseDictionaryParameterValueProvider = (LastResponseDictionaryParameterValueProvider) ParameterValueProviderCachedFactory.getParameterValueProvider(ParameterValueProviderType.LAST_RESPONSE_DICTIONARY);
        candidateProviders.add(lastResponseDictionaryParameterValueProvider);

        candidateProviders.forEach(p -> p.setStrict(this.strict));

        providers.addAll(candidateProviders.stream().filter(p -> p.countAvailableValuesFor(leafParameter) > 0)
                .collect(Collectors.toSet()));

        ParameterValueProvider chosenProvider = random.nextElement(providers).get();
        return chosenProvider.provideValueFor(leafParameter);
    }
}
