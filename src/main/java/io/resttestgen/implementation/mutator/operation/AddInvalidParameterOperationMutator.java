package io.resttestgen.implementation.mutator.operation;

import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.parameter.leaves.LeafParameter;
import io.resttestgen.core.helper.ExtendedRandom;
import io.resttestgen.core.openapi.Operation;
import io.resttestgen.core.testing.mutator.OperationMutator;
import io.resttestgen.core.testing.mutator.ParameterMutator;
import io.resttestgen.core.testing.parametervalueprovider.ParameterValueProviderCachedFactory;
import io.resttestgen.implementation.parametervalueprovider.ParameterValueProviderType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AddInvalidParameterOperationMutator extends OperationMutator {

    private static final Logger logger = LogManager.getLogger(AddInvalidParameterOperationMutator.class);

    private final ParameterMutator parameterMutator;

    public AddInvalidParameterOperationMutator(ParameterMutator parameterMutator) {
        this.parameterMutator = parameterMutator;
    }

    @Override
    public boolean isOperationMutable(Operation operation) {
        return !findAddableAndMutableParameters(operation).isEmpty();
    }

    @Override
    public Operation mutate(Operation operation) {

        ExtendedRandom random = Environment.getInstance().getRandom();

        Operation mutatedOperation = operation.deepClone();

        Optional<LeafParameter> mutatedParameter = random.nextElement(findAddableAndMutableParameters(mutatedOperation));

        if (mutatedParameter.isEmpty()) {
            logger.warn("Could not detect mutable parameters in this operation.");
            return operation;
        }

        mutatedParameter.get().setValueWithProvider(ParameterValueProviderCachedFactory.getParameterValueProvider(ParameterValueProviderType.ENUM_AND_EXAMPLE_PRIORITY));
        parameterMutator.mutate(mutatedParameter.get());
        mutatedParameter.get().addTag("mutated");

        logger.debug("Added invalid parameter {} (mutated with {}) with value {}.", mutatedParameter.get(),
                parameterMutator, mutatedParameter.get().getConcreteValue());

        return mutatedOperation;
    }

    @Override
    public boolean isErrorMutator() {
        return true;
    }

    private List<LeafParameter> findAddableAndMutableParameters(Operation operation) {
        return operation.getLeaves().stream()
                .filter(l -> !l.hasValue()) // Keep parameters without a value
                .filter(parameterMutator::isParameterMutable)
                .collect(Collectors.toList());
    }
}
