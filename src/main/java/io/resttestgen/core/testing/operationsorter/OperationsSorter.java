package io.resttestgen.core.testing.operationsorter;

import io.resttestgen.core.openapi.Operation;

import java.util.LinkedList;

/**
 *
 */
public abstract class OperationsSorter {

    protected LinkedList<Operation> queue = new LinkedList<>();

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

    public int getQueueSize() {
        return queue.size();
    }
 }
