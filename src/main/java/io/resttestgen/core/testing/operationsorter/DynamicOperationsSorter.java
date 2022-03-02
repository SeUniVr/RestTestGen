package io.resttestgen.core.testing.operationsorter;

import io.resttestgen.core.openapi.Operation;

public abstract class DynamicOperationsSorter extends OperationsSorter {

    @Override
    public boolean isEmpty() {
        refresh();
        return super.isEmpty();
    }

    @Override
    public Operation getFirst() {
        return super.getFirst();
    }

    /**
     * This method is implemented in subclasses, and it is intended to refresh the queue in the operations sorter
     */
    public abstract void refresh();
}
