package io.resttestgen.implementation.mutator.operation;

import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.HttpMethod;
import io.resttestgen.core.helper.ExtendedRandom;
import io.resttestgen.core.openapi.Operation;
import io.resttestgen.core.testing.mutator.OperationMutator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.HashSet;

/**
 * Mutates an operation by changing its HTTP method (e.g., GET becomes POST, PUT becomes GET, etc.)
 */
public class HttpMethodOperationMutator extends OperationMutator {

    private static final Logger logger = LogManager.getLogger(HttpMethodOperationMutator.class);

    /**
     * All operations are mutable, so just return true.
     */
    @Override
    public boolean isOperationMutable(Operation operation) {
        return true;
    }

    /**
     * Mutates the operation by choosing a random HTTP method between all method except for delete and current method.
     * @param operation the operation to mutate.
     * @return the mutated operation.
     */
    @Override
    public Operation mutate(Operation operation) {

        ExtendedRandom random = Environment.getInstance().getRandom();

        HashSet<HttpMethod> methods = new HashSet<>(Arrays.asList(HttpMethod.values()));

        // Do not mutate to delete method (to preserve API state)
        methods.remove(HttpMethod.DELETE);

        // With 50% probability remove uncommon HTTP methods
        if (random.nextBoolean()) {
            methods.remove(HttpMethod.TRACE);
            methods.remove(HttpMethod.HEAD);
            methods.remove(HttpMethod.OPTIONS);
        }

        // Remove current operation http method
        methods.remove(operation.getMethod());

        Operation mutateOperation = operation.deepClone();
        mutateOperation.setMethod(random.nextElement(methods).get());

        logger.debug("Mutated operation {} with new method {}", operation, mutateOperation.getMethod());

        return mutateOperation;
    }

    @Override
    public boolean isErrorMutator() {
        return true;
    }
}
