package io.resttestgen.core.testing;

import io.resttestgen.core.Environment;
import io.resttestgen.core.openapi.Operation;

import java.util.LinkedList;

/**
 * Class providing a static order for the operations to test. The order is decided a priori (no changes during the test
 * execution), so the complete queue of operations can be returned anytime.
 */
public abstract class StaticOperationsSorter extends OperationsSorter {


    public StaticOperationsSorter(Environment environment) {
        super(environment);
    }

    public LinkedList<Operation> getOperationsQueue() {
        return queue;
    }
}
