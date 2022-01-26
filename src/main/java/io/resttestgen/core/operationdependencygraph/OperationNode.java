package io.resttestgen.core.operationdependencygraph;

import io.resttestgen.core.openapi.Operation;

public class OperationNode {

    private final Operation operation;
    private int testingAttempts;
    private boolean tested;

    public OperationNode(Operation operation) {
        this.operation = operation;
        this.testingAttempts = 0;
        this.tested = false;
    }

    public void increaseTestingAttempts() {
        testingAttempts++;
    }

    public void setAsTested() {
        tested = true;
    }

    public Operation getOperation() {
        return operation;
    }

    public int getTestingAttempts() {
        return testingAttempts;
    }

    public boolean isTested() {
        return tested;
    }

    @Override
    public String toString() {
        return operation.toString();
    }
}
