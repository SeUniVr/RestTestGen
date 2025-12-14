package io.resttestgen.implementation.mutator.operation;

import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.parameter.leaves.LeafParameter;
import io.resttestgen.core.helper.ExtendedRandom;
import io.resttestgen.core.openapi.Operation;
import io.resttestgen.core.testing.mutator.OperationMutator;
import io.resttestgen.core.testing.parametervalueprovider.ParameterValueProviderCachedFactory;
import io.resttestgen.implementation.parametervalueprovider.ParameterValueProviderType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Mutates an operation by adding a leaf parameter.
 */
public class AddParameterOperationMutator extends OperationMutator {

    private static final Logger logger = LogManager.getLogger(AddParameterOperationMutator.class);

    /**
     * Operation is mutable if there is at least a parameter for which the value was not set previously.
     * @param operation the operation to check.
     * @return true if mutable, else otherwise.
     */
    @Override
    public boolean isOperationMutable(Operation operation) {
        return !findAddableParameters(operation).isEmpty();
    }

    @Override
    public Operation mutate(Operation operation) {

        ExtendedRandom random = Environment.getInstance().getRandom();

        Operation mutatedOperation = operation.deepClone();

        Optional<LeafParameter> mutatedParameter = random.nextElement(findAddableParameters(mutatedOperation));

        if (mutatedParameter.isEmpty()) {
            logger.warn("Could not detect mutable parameters in this operation.");
            return operation;
        }

        mutatedParameter.get().setValueWithProvider(ParameterValueProviderCachedFactory.getParameterValueProvider(ParameterValueProviderType.ENUM_AND_EXAMPLE_PRIORITY));

        logger.info("Added parameter {} with value {}.", mutatedParameter.get(), mutatedParameter.get().getConcreteValue());

        return mutatedOperation;
    }

    private List<LeafParameter> findAddableParameters(Operation operation) {
        return operation.getLeaves().stream().filter(l -> !l.hasValue()).collect(Collectors.toList());
    }

    @Override
    public boolean isErrorMutator() {
        return false;
    }
}
