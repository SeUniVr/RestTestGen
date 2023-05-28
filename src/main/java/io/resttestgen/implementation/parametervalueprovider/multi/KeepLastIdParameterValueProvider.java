package io.resttestgen.implementation.parametervalueprovider.multi;

import io.resttestgen.core.datatype.OperationSemantics;
import io.resttestgen.core.datatype.parameter.leaves.LeafParameter;
import io.resttestgen.core.openapi.Operation;
import io.resttestgen.core.testing.parametervalueprovider.ParameterValueProvider;
import io.resttestgen.implementation.parametervalueprovider.single.*;

public class KeepLastIdParameterValueProvider extends ParameterValueProvider {

    Object currentIdValue;
    boolean useInferredCrudSemantics = false;

    @Override
    public Object provideValueFor(LeafParameter leafParameter) {

        if (leafParameter.getTags().size() > 0 && leafParameter.getTags().contains("injected") &&
                leafParameter.getConcreteValue() != null) {
            return leafParameter.getConcreteValue();
        }

        if (isCrudResourceIdentifier(leafParameter) &&
                (getCRUDSemantics(leafParameter.getOperation()).equals(OperationSemantics.UPDATE) ||
                getCRUDSemantics(leafParameter.getOperation()).equals(OperationSemantics.READ) ||
                getCRUDSemantics(leafParameter.getOperation()).equals(OperationSemantics.DELETE))) {

            if (currentIdValue != null) {
                return currentIdValue;
            }

            // Try to get value from normalized dictionary
            /*LastNormalizedDictionaryParameterValueProvider localNormalizedDictionaryProvider =
                    new LastNormalizedDictionaryParameterValueProvider();
            if (localNormalizedDictionaryProvider.countAvailableValuesFor(parameterLeaf) > 0) {
                return localNormalizedDictionaryProvider.provideValueFor(parameterLeaf);
            }

            // Otherwise, try to get value from non-normalized dictionary
            LastDictionaryParameterValueProvider localDictionaryProvider = new LastDictionaryParameterValueProvider();
            if (localDictionaryProvider.countAvailableValuesFor(parameterLeaf) > 0) {
                return localDictionaryProvider.provideValueFor(parameterLeaf);
            }*/

            // If dictionary is not available, try other strategies (e.g., enum, example, default)
            EnumParameterValueProvider enumProvider = new EnumParameterValueProvider();
            if (enumProvider.countAvailableValuesFor(leafParameter) > 0) {
                return enumProvider.provideValueFor(leafParameter);
            }
            ExamplesParameterValueProvider examplesProvider = new ExamplesParameterValueProvider();
            if (examplesProvider.countAvailableValuesFor(leafParameter) > 0) {
                return examplesProvider.provideValueFor(leafParameter);
            }
            DefaultParameterValueProvider defaultProvider = new DefaultParameterValueProvider();
            if (defaultProvider.countAvailableValuesFor(leafParameter) > 0) {
                return defaultProvider.provideValueFor(leafParameter);
            }

            // If no other value is available, randomly generate it
            RandomParameterValueProvider randomProvider = new RandomParameterValueProvider();
            return randomProvider.provideValueFor(leafParameter);

        } else {

            // If dictionary is not available, try other strategies (e.g., enum, example, default)
            EnumParameterValueProvider enumProvider = new EnumParameterValueProvider();
            if (enumProvider.countAvailableValuesFor(leafParameter) > 0) {
                return enumProvider.provideValueFor(leafParameter);
            }
            ExamplesParameterValueProvider examplesProvider = new ExamplesParameterValueProvider();
            if (examplesProvider.countAvailableValuesFor(leafParameter) > 0) {
                return examplesProvider.provideValueFor(leafParameter);
            }
            DefaultParameterValueProvider defaultProvider = new DefaultParameterValueProvider();
            if (defaultProvider.countAvailableValuesFor(leafParameter) > 0) {
                return defaultProvider.provideValueFor(leafParameter);
            }

            // If no other value is available, randomly generate it
            RandomParameterValueProvider randomProvider = new RandomParameterValueProvider();
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
