package io.resttestgen.implementation.mutator.operation;

import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.parameter.leaves.LeafParameter;
import io.resttestgen.core.helper.ExtendedRandom;
import io.resttestgen.core.openapi.Operation;
import io.resttestgen.core.testing.mutator.OperationMutator;
import io.resttestgen.core.testing.mutator.ParameterMutator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.stream.Collectors;

public class MutateRandomParameterWithParameterMutatorOperationMutator extends OperationMutator {

    private static final Logger logger = LogManager.getLogger(MutateRandomParameterWithParameterMutatorOperationMutator.class);
    private ParameterMutator parameterMutator;

    public MutateRandomParameterWithParameterMutatorOperationMutator(@NotNull ParameterMutator parameterMutator) {
        this.parameterMutator = parameterMutator;
    }

    /**
     * The operation is mutable if there is at least one mutable parameter.
     * @param operation the operation to check.
     * @return true if operation is mutable.
     */
    @Override
    public boolean isOperationMutable(Operation operation) {
        return operation.getLeaves().stream().anyMatch(parameterMutator::isParameterMutable);
    }

    @Override
    public Operation mutate(Operation operation) {
        ExtendedRandom random = Environment.getInstance().getRandom();
        Operation mutatedOperation = operation.deepClone();
        Optional<LeafParameter> mutatedParameter = random.nextElement(mutatedOperation.getLeaves().stream()
                .filter(parameterMutator::isParameterMutable).collect(Collectors.toList()));

        if (mutatedParameter.isEmpty()) {
            logger.warn("Could not find mutable parameter in this operation.");
            return operation;
        }

        mutatedParameter = Optional.of((LeafParameter) parameterMutator.mutate(mutatedParameter.get()));
        mutatedParameter.get().addTag("mutated");

        logger.debug("Mutated parameter {} with {}. New value: {}", mutatedParameter.get(), parameterMutator, mutatedParameter.get().getConcreteValue());

        return mutatedOperation;
    }

    public ParameterMutator getParameterMutator() {
        return parameterMutator;
    }

    public void setParameterMutator(ParameterMutator parameterMutator) {
        this.parameterMutator = parameterMutator;
    }

    /**
     * Delegates the choice to the used parameter mutator.
     * @return the results of the same method of the parameter mutator.
     */
    @Override
    public boolean isErrorMutator() {
        return parameterMutator.isErrorMutator();
    }
}
