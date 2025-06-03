package io.resttestgen.core.helper;

import io.resttestgen.core.datatype.OperationSemantics;
import io.resttestgen.core.datatype.parameter.Parameter;
import io.resttestgen.core.openapi.OpenApi;
import io.resttestgen.core.openapi.Operation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

public class CrudGroup {

    private final boolean isInferred;
    private final String resourceType;
    private final Set<Operation> operations;

    private static final Logger logger = LogManager.getLogger(CrudGroup.class);

    public CrudGroup(OpenApi openAPI, String resourceType, boolean isInferred) {
        this.isInferred = isInferred;
        this.resourceType = resourceType;
        this.operations = new HashSet<>();
        if (isInferred) {
            operations.addAll(openAPI.getOperations().stream()
                    .filter(o -> o.getInferredCrudResourceType().equals(resourceType)).collect(Collectors.toSet()));
        } else {
            if (resourceType != null && !resourceType.trim().isEmpty()) {
                operations.addAll(openAPI.getOperations().stream()
                        .filter(o -> o.getCrudResourceType().equals(resourceType)).collect(Collectors.toSet()));
            }
        }
    }

    public boolean hasOperationsWithCRUDSemantics(OperationSemantics operationSemantics) {
        return operations.stream().anyMatch(o -> o.getCrudSemantics() == operationSemantics);
    }

    public boolean hasCRUDOperations(Collection<OperationSemantics> operationSemanticsCollection) {
        Set<OperationSemantics> operationSemanticsSet = new HashSet<>(operationSemanticsCollection);
        for (OperationSemantics operationSemantics : operationSemanticsSet) {
            return false;
            /*if (hasCRUDOperation(crudSemantics) < 1) {
                return false;
            }*/
        }
        return true;
    }

    public int countOperations(OperationSemantics operationSemantics) {
        return (int) operations.stream().filter(o -> o.getCrudSemantics() == operationSemantics).count();
    }

    // Returns a random operation among those with a given CRUD semantics
    public Operation pickRandomOperation(OperationSemantics operationSemantics) {
        Random random = new Random(); // FIXME: use global random
        List<Operation> matchingOperations = operations.stream()
                .filter(o -> o.getCrudSemantics() == operationSemantics).collect(Collectors.toList());
        return matchingOperations.get(random.nextInt(matchingOperations.size()));
    }

    // Returns the operation with index i among those with a given CRUD semantics
    public Operation getOperation(OperationSemantics operationSemantics, int index) {
        List<Operation> matchingOperations;
        if (isInferred) {
            matchingOperations = operations.stream()
                    .filter(o -> o.getInferredCrudSemantics() == operationSemantics).collect(Collectors.toList());
        } else {
            matchingOperations = operations.stream()
                    .filter(o -> o.getCrudSemantics() == operationSemantics).collect(Collectors.toList());
        }
        if (index >= 0 && index < matchingOperations.size()) {
            return matchingOperations.get(index);
        }
        return null;
    }

    /**
     * Get all the operations in the batch with a given CRUD semantics.
     * @param operationSemantics the CRUD semantics of the wanted operations.
     * @return a set of operation with the corresponding CRUD semantics.
     */
    public Set<Operation> getOperations(OperationSemantics operationSemantics) {
        if (isInferred) {
            return operations.stream().filter(o -> o.getInferredCrudSemantics().equals(operationSemantics)).collect(Collectors.toSet());
        }
        return operations.stream().filter(o -> o.getCrudSemantics().equals(operationSemantics)).collect(Collectors.toSet());
    }

    public Set<Operation> getOperations() {
        return this.operations;
    }

    public List<Parameter> getReadOnlyParameters() {
        return null;
    }

    public String getResourceType() {
        return resourceType;
    }

    public boolean isInferred() {
        return isInferred;
    }
}
