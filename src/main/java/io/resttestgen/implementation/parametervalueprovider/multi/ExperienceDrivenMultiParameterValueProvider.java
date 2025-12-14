package io.resttestgen.implementation.parametervalueprovider.multi;

import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.parameter.leaves.LeafParameter;
import io.resttestgen.core.helper.Experience;
import io.resttestgen.core.helper.ExtendedRandom;
import io.resttestgen.core.testing.parametervalueprovider.CountableParameterValueProvider;
import io.resttestgen.core.testing.parametervalueprovider.ParameterValueProvider;
import io.resttestgen.core.testing.parametervalueprovider.ParameterValueProviderCachedFactory;
import io.resttestgen.core.testing.parametervalueprovider.ValueNotAvailableException;
import io.resttestgen.implementation.parametervalueprovider.ParameterValueProviderType;
import kotlin.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Chooses the best parameter value provider for a parameter given the experience gained in previous executions. To
 * allow exploration of choices, sometimes it chooses the parameter provider randomly according to the defined epsilon.
 */
public class ExperienceDrivenMultiParameterValueProvider extends ParameterValueProvider {

    private static final Logger logger = LogManager.getLogger(ExperienceDrivenMultiParameterValueProvider.class);

    private double epsilon = 0.1; // Determine the probability of choosing a random provider
    private final Experience experience = Environment.getInstance().getExperience();
    private final ExtendedRandom random = Environment.getInstance().getRandom();

    @Override
    public Pair<ParameterValueProvider, Object> provideValueFor(LeafParameter leafParameter) throws ValueNotAvailableException {

        ParameterValueProvider chosenProvider = ParameterValueProviderCachedFactory.getParameterValueProvider(ParameterValueProviderType.ENUM_AND_EXAMPLE_PRIORITY);

        HashMap<ParameterValueProviderType, AtomicInteger> providerExperience = experience.getParameterValueExperienceForNormalizedName(leafParameter.getNormalizedName());

        // Ignore experience if no experience is available for parameter, or if epsilon matches
        if (random.nextDouble() <= epsilon || providerExperience == null) {

            // 40% do nothing (keep the enum and example priority provider), 60% use random provider
            if (random.nextDouble() <= 0.6) {
                chosenProvider = ParameterValueProviderCachedFactory.getParameterValueProvider(ParameterValueProviderType.RANDOM_PROVIDER);
            }
        }

        // Otherwise, sample provider from the cumulative experience
        else {

            // Keep only valueProviders that can return at least one value
            HashMap<ParameterValueProviderType, AtomicInteger> filteredExperience = new  HashMap<>(providerExperience);
            filter(filteredExperience,leafParameter);

            ParameterValueProviderType result = random.nextInt(10) < 7 ? ParameterValueProviderType.NARROW_RANDOM : ParameterValueProviderType.RANDOM;

            if (!filteredExperience.keySet().isEmpty()) {
                int count = filteredExperience.values().stream().mapToInt(AtomicInteger::get).sum();

                int choice = Environment.getInstance().getRandom().nextInt(count);
                for (ParameterValueProviderType providerType : filteredExperience.keySet()) {
                    choice -= filteredExperience.get(providerType).get();
                    if (choice < 0) {
                        result = providerType;
                    }
                }
            }

            chosenProvider = ParameterValueProviderCachedFactory.getParameterValueProvider(result);
        }
        return chosenProvider.provideValueFor(leafParameter);
    }

    private void filter(HashMap<ParameterValueProviderType, AtomicInteger> filteredExperience, LeafParameter leafParameter) {
        List<ParameterValueProviderType> toRemove = new ArrayList<>();
        for (ParameterValueProviderType parameterValueProviderType : filteredExperience.keySet()) {
            ParameterValueProvider provider = ParameterValueProviderCachedFactory.getParameterValueProvider(parameterValueProviderType);
            if (provider instanceof CountableParameterValueProvider) {
                if (((CountableParameterValueProvider) provider).countAvailableValuesFor(leafParameter) < 1) {
                    toRemove.add(parameterValueProviderType);
                }
            }
        }
        for (ParameterValueProviderType type: toRemove){
            filteredExperience.remove(type);
        }
    }

    public void setEpsilon(double epsilon) {
        this.epsilon = epsilon;
    }
}
