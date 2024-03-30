package io.resttestgen.core.testing.mutator;

import io.resttestgen.core.openapi.Operation;

public abstract class OperationMutator extends Mutator {

    /**
     * Check if an operation is mutable.
     * @param operation the operation to check.
     * @return true if the operation is mutable, false otherwise.
     */
    public abstract boolean isOperationMutable(Operation operation);

    /**
     * Applies the mutation to the provided operation.
     * @param operation the operation to mutate.
     * @return the mutated operation.
     */
    public abstract Operation mutate(Operation operation);
}
