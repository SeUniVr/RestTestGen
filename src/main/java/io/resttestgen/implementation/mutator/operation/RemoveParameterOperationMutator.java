package io.resttestgen.implementation.mutator.operation;

import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.parameter.leaves.LeafParameter;
import io.resttestgen.core.openapi.Operation;
import io.resttestgen.core.testing.mutator.OperationMutator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.stream.Collectors;

public class RemoveParameterOperationMutator extends OperationMutator {

    private static final Logger logger = LogManager.getLogger(RemoveParameterOperationMutator.class);
    
    @Override
    public boolean isOperationMutable(Operation operation) {
        return !findRemovableParameters(operation).isEmpty();
    }

    @Override
    public Operation mutate(Operation operation) {

        Operation mutatedOperation = operation.deepClone();
        List<LeafParameter> removableParameter = findRemovableParameters(mutatedOperation);

        if (removableParameter.isEmpty()) {
            logger.warn("Could not find removable parameters in this operation.");
            return operation;
        }

        logger.debug("Removed parameter {}.", removableParameter);

        Environment.getInstance().getRandom().nextElement(removableParameter).get().removeValue();
        return mutatedOperation;
    }

    /**
     * Removable parameters are parameters that have been used in the request (they have a value set), but they are not
     * mandatory (not required).
     * @param operation the operation on which to perform the search.
     * @return the list of removable parameters.
     */
    private List<LeafParameter> findRemovableParameters(Operation operation) {
        return operation.getLeaves().stream().filter(l -> l.hasValue() && !l.isRequired()).collect(Collectors.toList());
    }

    @Override
    public boolean isErrorMutator() {
        return false;
    }
}
