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
 * Refills the value of a previously set leaf parameter. With 50% probability, the new value is picked from the same
 * ParameterValueProvider as it used to be from. Conversely, a new provider is picked.
 */
public class RefillValueOperationMutator extends OperationMutator {

    private static final Logger logger = LogManager.getLogger(RefillValueOperationMutator.class);

    /**
     * Only operations that have parameters with a set value are mutable.
     * @param operation the operation to check.
     * @return true when an operation has parameters with a set value.
     */
    @Override
    public boolean isOperationMutable(Operation operation) {
        return !findRefillableParameters(operation).isEmpty();
    }

    @Override
    public Operation mutate(Operation operation) {

        ExtendedRandom random = Environment.getInstance().getRandom();

        Operation mutatedOperation = operation.deepClone();

        Optional<LeafParameter> refillableParameter = random.nextElement(findRefillableParameters(mutatedOperation));

        if (refillableParameter.isEmpty()) {
            logger.warn("Could not find a parameter to refill with new value.");
            return operation;
        }

        String oldValue = refillableParameter.get().getConcreteValue().toString();
        String newValue = refillableParameter.get().getConcreteValue().toString();
        int attempts = 10;

        // Attempt the retrieval a new value 10 times, as it's likely to get a value identical to the previous one
        while (oldValue.equals(newValue) && attempts > 0) {
            if (random.nextBoolean()) {
                refillableParameter.get().setValueWithProvider(refillableParameter.get().getValueSource());
            } else {
                refillableParameter.get().setValueWithProvider(ParameterValueProviderCachedFactory.getParameterValueProvider(ParameterValueProviderType.ENUM_AND_EXAMPLE_PRIORITY));
            }
            newValue = refillableParameter.get().getConcreteValue().toString();
            attempts--;
        }

        logger.debug("Refilled value for parameter {}. Old value {}. New value {}", refillableParameter.get(), oldValue, refillableParameter.get().getConcreteValue());

        return mutatedOperation;
    }

    private List<LeafParameter> findRefillableParameters(Operation operation) {
        return operation.getLeaves().stream().filter(LeafParameter::hasValue).collect(Collectors.toList());
    }

    @Override
    public boolean isErrorMutator() {
        return false;
    }
}
