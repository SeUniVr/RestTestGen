package io.resttestgen.implementation.parametervalueprovider.multi;

import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.parameter.leaves.LeafParameter;
import io.resttestgen.core.helper.ExtendedRandom;
import io.resttestgen.core.testing.parametervalueprovider.CountableParameterValueProvider;
import io.resttestgen.core.testing.parametervalueprovider.ParameterValueProvider;
import io.resttestgen.implementation.parametervalueprovider.single.*;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class RandomProviderParameterValueProvider extends ParameterValueProvider {

    @Override
    public Object provideValueFor(LeafParameter leafParameter) {

        ExtendedRandom random = Environment.getInstance().getRandom();

        Set<ParameterValueProvider> providers = new HashSet<>();

        // The random generator provider is always available
        providers.add(new RandomParameterValueProvider());

        // List of candidate providers, that will be used only if they have values available
        Set<CountableParameterValueProvider> candidateProviders = new HashSet<>();
        candidateProviders.add(new DefaultParameterValueProvider());
        candidateProviders.add(new EnumParameterValueProvider());
        candidateProviders.add(new ExamplesParameterValueProvider());
        candidateProviders.add(new NormalizedDictionaryParameterValueProvider());
        candidateProviders.add(new DictionaryParameterValueProvider());

        candidateProviders.forEach(p -> p.setStrict(this.strict));

        providers.addAll(candidateProviders.stream().filter(p -> p.countAvailableValuesFor(leafParameter) > 0)
                .collect(Collectors.toSet()));

        Optional<ParameterValueProvider> chosenProvider = random.nextElement(providers);
        return chosenProvider.map(parameterValueProvider ->
                parameterValueProvider.provideValueFor(leafParameter)).orElse(null);

    }
}
