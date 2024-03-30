package io.resttestgen.implementation.parametervalueprovider;

import io.resttestgen.core.testing.parametervalueprovider.ParameterValueProvider;
import io.resttestgen.implementation.parametervalueprovider.multi.*;
import io.resttestgen.implementation.parametervalueprovider.single.*;

/**
 * Enum listing the types of single parameter value providers
 */
public enum ParameterValueProviderType {

    // SINGLE
    RANDOM,
    NARROW_RANDOM,
    ENUM,
    EXAMPLES,
    DEFAULT,
    REQUEST_DICTIONARY,
    RESPONSE_DICTIONARY,
    LAST_REQUEST_DICTIONARY,
    LAST_RESPONSE_DICTIONARY,

    //MULTI
    RANDOM_PROVIDER,
    GLOBAL_DICTIONARY_PRIORITY,
    LOCAL_DICTIONARY_PRIORITY,
    KEEP_LAST_ID,
    ENUM_AND_EXAMPLE_PRIORITY;

    public static ParameterValueProviderType getTypeFromProvider(ParameterValueProvider provider) {
        if (provider instanceof RandomParameterValueProvider) {
            return RANDOM;
        } else if (provider instanceof NarrowRandomParameterValueProvider) {
            return NARROW_RANDOM;
        } else if (provider instanceof EnumParameterValueProvider) {
            return ENUM;
        } else if (provider instanceof ExamplesParameterValueProvider) {
            return EXAMPLES;
        } else if (provider instanceof DefaultParameterValueProvider) {
            return DEFAULT;
        } else if (provider instanceof RequestDictionaryParameterValueProvider) {
            return REQUEST_DICTIONARY;
        } else if (provider instanceof ResponseDictionaryParameterValueProvider) {
            return RESPONSE_DICTIONARY;
        } else if (provider instanceof LastRequestDictionaryParameterValueProvider) {
            return LAST_REQUEST_DICTIONARY;
        } else if (provider instanceof LastResponseDictionaryParameterValueProvider) {
            return LAST_RESPONSE_DICTIONARY;
        } else if (provider instanceof RandomProviderParameterValueProvider) {
            return RANDOM_PROVIDER;
        } else if (provider instanceof GlobalDictionaryPriorityParameterValueProvider) {
            return GLOBAL_DICTIONARY_PRIORITY;
        } else if (provider instanceof LocalDictionaryPriorityParameterValueProvider) {
            return LOCAL_DICTIONARY_PRIORITY;
        } else if (provider instanceof KeepLastIdParameterValueProvider) {
            return KEEP_LAST_ID;
        } else {
            return ENUM_AND_EXAMPLE_PRIORITY;
        }
    }
}
