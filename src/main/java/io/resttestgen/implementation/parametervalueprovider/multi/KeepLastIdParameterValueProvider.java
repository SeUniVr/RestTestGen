package io.resttestgen.implementation.parametervalueprovider.multi;

import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.OperationSemantics;
import io.resttestgen.core.datatype.parameter.leaves.LeafParameter;
import io.resttestgen.core.openapi.Operation;
import io.resttestgen.core.testing.parametervalueprovider.ParameterValueProvider;
import io.resttestgen.core.testing.parametervalueprovider.ParameterValueProviderCachedFactory;
import io.resttestgen.core.testing.parametervalueprovider.ValueNotAvailableException;
import io.resttestgen.implementation.parametervalueprovider.ParameterValueProviderType;
import io.resttestgen.implementation.parametervalueprovider.single.*;
import kotlin.Pair;

public class KeepLastIdParameterValueProvider extends ParameterValueProvider {

    Object currentIdValue;
    boolean useInferredCrudSemantics = false;

    @Override
    public Pair<ParameterValueProvider, Object> provideValueFor(LeafParameter leafParameter) throws ValueNotAvailableException {

        if (!leafParameter.getTags().isEmpty() && leafParameter.getTags().contains("injected") &&
                leafParameter.getConcreteValue() != null) {
            return new Pair<>(this, leafParameter.getConcreteValue());
        }

        if (isCrudResourceIdentifier(leafParameter) &&
                (getCRUDSemantics(leafParameter.getOperation()).equals(OperationSemantics.UPDATE) ||
                getCRUDSemantics(leafParameter.getOperation()).equals(OperationSemantics.READ) ||
                getCRUDSemantics(leafParameter.getOperation()).equals(OperationSemantics.DELETE))) {

            if (currentIdValue != null) {
                return new Pair<>(this, currentIdValue);
            }

            // Try to get value from dictionary
            LastResponseDictionaryParameterValueProvider localNormalizedDictionaryProvider =
                    new LastResponseDictionaryParameterValueProvider();
            if (localNormalizedDictionaryProvider.countAvailableValuesFor(leafParameter) > 0) {
                return localNormalizedDictionaryProvider.provideValueFor(leafParameter);
            }

            // If dictionary is not available, try other strategies (e.g., enum, example, default)
            EnumParameterValueProvider enumProvider = (EnumParameterValueProvider) ParameterValueProviderCachedFactory.getParameterValueProvider(ParameterValueProviderType.ENUM);
            if (enumProvider.countAvailableValuesFor(leafParameter) > 0) {
                return enumProvider.provideValueFor(leafParameter);
            }
            ExamplesParameterValueProvider examplesProvider = (ExamplesParameterValueProvider) ParameterValueProviderCachedFactory.getParameterValueProvider(ParameterValueProviderType.EXAMPLES);
            if (examplesProvider.countAvailableValuesFor(leafParameter) > 0) {
                return examplesProvider.provideValueFor(leafParameter);
            }
            DefaultParameterValueProvider defaultProvider = (DefaultParameterValueProvider) ParameterValueProviderCachedFactory.getParameterValueProvider(ParameterValueProviderType.DEFAULT);
            if (defaultProvider.countAvailableValuesFor(leafParameter) > 0) {
                return defaultProvider.provideValueFor(leafParameter);
            }

            // If no other value is available, randomly generate it
            ParameterValueProvider randomProvider = ParameterValueProviderCachedFactory.getParameterValueProvider(ParameterValueProviderType.RANDOM);
            return randomProvider.provideValueFor(leafParameter);

        } else {

            // If dictionary is not available, try other strategies (e.g., enum, example, default)
            EnumParameterValueProvider enumProvider = (EnumParameterValueProvider) ParameterValueProviderCachedFactory.getParameterValueProvider(ParameterValueProviderType.ENUM);
            if (enumProvider.countAvailableValuesFor(leafParameter) > 0) {
                return enumProvider.provideValueFor(leafParameter);
            }
            ExamplesParameterValueProvider examplesProvider = (ExamplesParameterValueProvider) ParameterValueProviderCachedFactory.getParameterValueProvider(ParameterValueProviderType.EXAMPLES);
            if (examplesProvider.countAvailableValuesFor(leafParameter) > 0) {
                return examplesProvider.provideValueFor(leafParameter);
            }
            DefaultParameterValueProvider defaultProvider = (DefaultParameterValueProvider) ParameterValueProviderCachedFactory.getParameterValueProvider(ParameterValueProviderType.DEFAULT);
            if (defaultProvider.countAvailableValuesFor(leafParameter) > 0) {
                return defaultProvider.provideValueFor(leafParameter);
            }

            // If no other value is available, randomly generate it
            ParameterValueProvider randomProvider = Environment.getInstance().getRandom().nextBoolean() ?
                    ParameterValueProviderCachedFactory.getParameterValueProvider(ParameterValueProviderType.RANDOM) :
                    ParameterValueProviderCachedFactory.getParameterValueProvider(ParameterValueProviderType.NARROW_RANDOM);
            return randomProvider.provideValueFor(leafParameter);
        }
    }

    private OperationSemantics getCRUDSemantics(Operation operation) {
        if (useInferredCrudSemantics) {
            return operation.getInferredCrudSemantics();
        }
        return operation.getCrudSemantics();
    }

    private boolean isCrudResourceIdentifier(LeafParameter leaf) {
        if (useInferredCrudSemantics) {
            return leaf.isInferredResourceIdentifier();
        }
        return leaf.isResourceIdentifier();
    }

    public Object getCurrentIdValue() {
        return currentIdValue;
    }

    public void setCurrentIdValue(Object currentIdValue) {
        this.currentIdValue = currentIdValue;
    }

    public boolean isUseInferredCrudSemantics() {
        return useInferredCrudSemantics;
    }

    public void setUseInferredCrudSemantics(boolean useInferredCrudSemantics) {
        this.useInferredCrudSemantics = useInferredCrudSemantics;
    }
}
