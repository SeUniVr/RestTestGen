package io.resttestgen.core.testing;

import io.resttestgen.core.Environment;
import io.resttestgen.core.openapi.Operation;

import java.util.LinkedList;

/**
 *
 */
public abstract class OperationsSorter {

    protected LinkedList<Operation> queue;

    public OperationsSorter(Environment environment) {
        queue = new LinkedList<>();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public Operation getFirst() {
        return queue.getFirst();
    }

    public Operation removeFirst() {
        return queue.remove(0);
    }

    public void remove(Operation operationToRemove) {
        queue.remove(operationToRemove);
    }
}
