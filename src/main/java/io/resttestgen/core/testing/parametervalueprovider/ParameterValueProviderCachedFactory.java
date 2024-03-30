package io.resttestgen.core.testing.parametervalueprovider;

import io.resttestgen.implementation.parametervalueprovider.ParameterValueProviderType;
import io.resttestgen.implementation.parametervalueprovider.multi.*;
import io.resttestgen.implementation.parametervalueprovider.single.*;

import java.util.HashMap;

public class ParameterValueProviderCachedFactory {

    private static final HashMap<ParameterValueProviderType, ParameterValueProvider> cache = new HashMap<>();

    public static ParameterValueProvider getParameterValueProvider(ParameterValueProviderType type) {
        if (cache.get(type) == null) {
            ParameterValueProvider provider;
            switch (type) {
                case ENUM:
                    provider = new EnumParameterValueProvider();
                    break;
                case EXAMPLES:
                    provider = new ExamplesParameterValueProvider();
                    break;
                case DEFAULT:
                    provider = new DefaultParameterValueProvider();
                    break;
                case REQUEST_DICTIONARY:
                    provider = new RequestDictionaryParameterValueProvider();
                    break;
                case RESPONSE_DICTIONARY:
                    provider = new ResponseDictionaryParameterValueProvider();
                    break;
                case LAST_REQUEST_DICTIONARY:
                    provider = new LastRequestDictionaryParameterValueProvider();
                    break;
                case LAST_RESPONSE_DICTIONARY:
                    provider = new LastResponseDictionaryParameterValueProvider();
                    break;
                case NARROW_RANDOM:
                    provider = new NarrowRandomParameterValueProvider();
                    break;
                case RANDOM_PROVIDER:
                    provider = new RandomProviderParameterValueProvider();
                    break;
                case GLOBAL_DICTIONARY_PRIORITY:
                    provider = new GlobalDictionaryPriorityParameterValueProvider();
                    break;
                case LOCAL_DICTIONARY_PRIORITY:
                    provider = new LocalDictionaryPriorityParameterValueProvider();
                    break;
                case KEEP_LAST_ID:
                    provider = new KeepLastIdParameterValueProvider();
                    break;
                case ENUM_AND_EXAMPLE_PRIORITY:
                    provider = new EnumAndExamplePriorityParameterValueProvider();
                    break;
                default:
                    provider = new RandomParameterValueProvider();
            }
            cache.put(type, provider);
        }
        return cache.get(type);
    }
}
