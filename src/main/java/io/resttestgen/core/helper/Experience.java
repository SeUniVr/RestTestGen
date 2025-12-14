package io.resttestgen.core.helper;

import io.resttestgen.core.datatype.NormalizedParameterName;
import io.resttestgen.core.datatype.parameter.leaves.LeafParameter;
import io.resttestgen.core.datatype.parameter.structured.ArrayParameter;
import io.resttestgen.core.testing.parametervalueprovider.ParameterValueProvider;
import io.resttestgen.implementation.parametervalueprovider.ParameterValueProviderType;
import kotlin.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Experience {

    private final static Logger logger = LogManager.getLogger(Experience.class);

    HashMap<NormalizedParameterName, HashMap<ParameterValueProviderType, AtomicInteger>> parameterValueExperienceMap = new HashMap<>();
    HashMap<NormalizedParameterName, Pair<AtomicInteger, AtomicInteger>> simpleParameterPresenceExperienceMap = new HashMap<>();
    HashMap<NormalizedParameterName, HashMap<Integer, Integer>> arraySizeExperienceMap = new HashMap<>();

    public void addParameterValueObservation(LeafParameter leafParameter, ParameterValueProvider provider) {

        // If no provider is identified, abort
        if (provider == null) {
            return;
        }

        ParameterValueProviderType providerType = ParameterValueProviderType.getTypeFromProvider(provider);

        // If the entry for this parameter name is not already in the map, create instance
        parameterValueExperienceMap.computeIfAbsent(leafParameter.getNormalizedName(), k -> new HashMap<>());

        // Get the experience map for the current parameter name
        HashMap<ParameterValueProviderType, AtomicInteger> map = parameterValueExperienceMap.get(leafParameter.getNormalizedName());

        // If no previous observations for the provider are stored, store first observation
        if (map.get(providerType) == null) {
            map.put(providerType, new AtomicInteger(0));
            logger.info("Found new provider: " + provider.getClass().getSimpleName() + "@" + provider.hashCode());
        }

        map.get(providerType).incrementAndGet();
    }

    public void addParameterPresenceObservation(LeafParameter leafParameter, boolean present) {

        // If there are no observations for the parameter create a new Pair with the current observation
        if (simpleParameterPresenceExperienceMap.get(leafParameter.getNormalizedName()) == null) {
            simpleParameterPresenceExperienceMap.put(leafParameter.getNormalizedName(), new Pair<>(new AtomicInteger(1), new AtomicInteger(present ? 1 : 0)));
        }

        // Conversely, update the observation
        else {
            Pair<AtomicInteger, AtomicInteger> currentObservations = simpleParameterPresenceExperienceMap.get(leafParameter.getNormalizedName());
            currentObservations.getFirst().incrementAndGet();
            if (present) {
                currentObservations.getSecond().incrementAndGet();
            }
        }
    }

    /**
     * Adds an observation for the size of an array parameter.
     * @param arrayParameter the array parameter.
     * @param size the observed size.
     */
    public void addArraySizeObservation(ArrayParameter arrayParameter, int size) {
        int classSize = 2; // All values > 1 are stored to class n. 2
        if (size >= 0 && size <= 1) {
            classSize = size;
        }

        // If there are no observations for the array, create new entry
        if (arraySizeExperienceMap.get(arrayParameter.getNormalizedName()) == null) {
            HashMap<Integer, Integer> map = new HashMap<>();
            map.put(0, 0);
            map.put(1, 0);
            map.put(2, 0);
            arraySizeExperienceMap.put(arrayParameter.getNormalizedName(), map);
        }

        // Add the current observation to the map
        HashMap<Integer, Integer> map = arraySizeExperienceMap.get(arrayParameter.getNormalizedName());
        map.put(classSize, map.get(classSize) + 1);
    }

    public HashMap<ParameterValueProviderType, AtomicInteger> getParameterValueExperienceForNormalizedName(NormalizedParameterName normalizedParameterName) {
        return parameterValueExperienceMap.get(normalizedParameterName);
    }

    public Pair<AtomicInteger, AtomicInteger> getSimpleParameterPresenceExperienceForNormalizedName(NormalizedParameterName normalizedParameterName) {
        return simpleParameterPresenceExperienceMap.get(normalizedParameterName);
    }

    public HashMap<Integer, Integer> getArraySizeExperienceForNormalizedName(NormalizedParameterName normalizedParameterName) {
        return arraySizeExperienceMap.get(normalizedParameterName);
    }

    /*
    public ParameterValueProvider getProvider(LeafParameter leafParameter) {
        refreshExperienceForParameter(leafParameter);

        HashMap<ParameterValueProvider, AtomicInteger> subMap = parameterValueExperienceMap.get(leafParameter.getNormalizedName());
        int count = 0;
        for (Integer singleCount : subMap.values()) {
            count += singleCount;
        }

        int choice = Environment.getInstance().getRandom().nextInt(count);
        for (ParameterValueProvider provider : subMap.keySet()) {
            choice -= subMap.get(provider);
            if (choice < 0) {
                return provider;
            }
        }
        return null;
    }

    private void refreshExperienceForParameter(LeafParameter leafParameter) {

        // Init subMap for parameter
        HashMap<ParameterValueProvider, Integer> subMap =
                parameterValueExperienceMap.computeIfAbsent(leafParameter.getNormalizedName(), k -> new LinkedHashMap<>());

        for (ParameterValueProvider provider : providers) {
            if (subMap.get(provider) == null || subMap.get(provider) == 0) {
                if (provider instanceof CountableParameterValueProvider) {
                    int count = ((CountableParameterValueProvider) provider).countAvailableValuesFor(leafParameter);
                    subMap.put(provider, count > 0 ? 1 : 0);
                } else if (provider instanceof RemoveParameterValueProvider) {
                    subMap.put(provider, leafParameter.isRequired() ? 0 : 8);
                } else {
                    subMap.put(provider, 1);
                }
            }
        }
    }
    */

    public void printStats() {
        System.out.println("Values: " + parameterValueExperienceMap);
        System.out.println("Presence: " + simpleParameterPresenceExperienceMap);
        System.out.println("Array size: " + arraySizeExperienceMap);
    }
}