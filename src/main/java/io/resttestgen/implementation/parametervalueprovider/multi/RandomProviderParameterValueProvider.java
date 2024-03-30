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

public class RandomProviderParameterValueProvider extends ParameterValueProvider {

    @Override
    public Pair<ParameterValueProvider, Object> provideValueFor(LeafParameter leafParameter) throws ValueNotAvailableException {

        ExtendedRandom random = Environment.getInstance().getRandom();

        Set<ParameterValueProvider> providers = new HashSet<>();

        // The random generator provider is always available
        providers.add(ParameterValueProviderCachedFactory.getParameterValueProvider(ParameterValueProviderType.RANDOM));
        providers.add(ParameterValueProviderCachedFactory.getParameterValueProvider(ParameterValueProviderType.NARROW_RANDOM));

        // List of candidate providers, that will be used only if they have values available
        Set<CountableParameterValueProvider> candidateProviders = new HashSet<>();
        candidateProviders.add((DefaultParameterValueProvider) ParameterValueProviderCachedFactory.getParameterValueProvider(ParameterValueProviderType.DEFAULT));
        candidateProviders.add((EnumParameterValueProvider) ParameterValueProviderCachedFactory.getParameterValueProvider(ParameterValueProviderType.ENUM));
        candidateProviders.add((ExamplesParameterValueProvider) ParameterValueProviderCachedFactory.getParameterValueProvider(ParameterValueProviderType.EXAMPLES));
        candidateProviders.add((ResponseDictionaryParameterValueProvider) ParameterValueProviderCachedFactory.getParameterValueProvider(ParameterValueProviderType.RESPONSE_DICTIONARY));
        candidateProviders.add((RequestDictionaryParameterValueProvider) ParameterValueProviderCachedFactory.getParameterValueProvider(ParameterValueProviderType.REQUEST_DICTIONARY));
        candidateProviders.add((LastResponseDictionaryParameterValueProvider) ParameterValueProviderCachedFactory.getParameterValueProvider(ParameterValueProviderType.LAST_RESPONSE_DICTIONARY));
        candidateProviders.add((LastRequestDictionaryParameterValueProvider) ParameterValueProviderCachedFactory.getParameterValueProvider(ParameterValueProviderType.LAST_REQUEST_DICTIONARY));

        candidateProviders.forEach(p -> p.setStrict(this.strict));

        providers.addAll(candidateProviders.stream().filter(p -> p.countAvailableValuesFor(leafParameter) > 0)
                .collect(Collectors.toSet()));

        ParameterValueProvider chosenProvider = random.nextElement(providers).get();
        return chosenProvider.provideValueFor(leafParameter);

    }
}
