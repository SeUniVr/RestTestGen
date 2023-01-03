package io.resttestgen.core.helper;

import io.resttestgen.core.datatype.CRUDSemantics;
import io.resttestgen.core.datatype.parameter.ParameterElement;
import io.resttestgen.core.openapi.OpenAPI;
import io.resttestgen.core.openapi.Operation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

public class CRUDGroup {

    private final boolean isInferred;
    private final String resourceType;
    private final Set<Operation> operations;

    private static final Logger logger = LogManager.getLogger(CRUDGroup.class);

    public CRUDGroup(OpenAPI openAPI, String resourceType, boolean isInferred) {
        this.isInferred = isInferred;
        this.resourceType = resourceType;
        this.operations = new HashSet<>();
        if (isInferred) {
            operations.addAll(openAPI.getOperations().stream()
                    .filter(o -> o.getInferredCrudResourceType().equals(resourceType)).collect(Collectors.toSet()));
        } else {
            if (resourceType != null && !resourceType.trim().equals("")) {
                operations.addAll(openAPI.getOperations().stream()
                        .filter(o -> o.getCrudResourceType().equals(resourceType)).collect(Collectors.toSet()));
            }
        }
    }

    public boolean hasOperationsWithCRUDSemantics(CRUDSemantics crudSemantics) {
        return operations.stream().anyMatch(o -> o.getCrudSemantics() == crudSemantics);
    }

    public boolean hasCRUDOperations(Collection<CRUDSemantics> crudSemanticsCollection) {
        Set<CRUDSemantics> crudSemanticsSet = new HashSet<>(crudSemanticsCollection);
        for (CRUDSemantics crudSemantics : crudSemanticsSet) {
            return false;
            /*if (hasCRUDOperation(crudSemantics) < 1) {
                return false;
            }*/
        }
        return true;
    }

    public int countOperations(CRUDSemantics crudSemantics) {
        return (int) operations.stream().filter(o -> o.getCrudSemantics() == crudSemantics).count();
    }

    // Returns a random operation among those with a given CRUD semantics
    public Operation pickRandomOperation(CRUDSemantics crudSemantics) {
        Random random = new Random(); // FIXME: use global random
        List<Operation> matchingOperations = operations.stream()
                .filter(o -> o.getCrudSemantics() == crudSemantics).collect(Collectors.toList());
        return matchingOperations.get(random.nextInt(matchingOperations.size()));
    }

    // Returns the operation with index i among those with a given CRUD semantics
    public Operation getOperation(CRUDSemantics crudSemantics, int index) {
        List<Operation> matchingOperations;
        if (isInferred) {
            matchingOperations = operations.stream()
                    .filter(o -> o.getInferredCrudSemantics() == crudSemantics).collect(Collectors.toList());
        } else {
            matchingOperations = operations.stream()
                    .filter(o -> o.getCrudSemantics() == crudSemantics).collect(Collectors.toList());
        }
        if (index >= 0 && index < matchingOperations.size()) {
            return matchingOperations.get(index);
        }
        return null;
    }

    /**
     * Get all the operations in the batch with a given CRUD semantics.
     * @param crudSemantics the CRUD semantics of the wanted operations.
     * @return a set of operation with the corresponding CRUD semantics.
     */
    public Set<Operation> getOperations(CRUDSemantics crudSemantics) {
        if (isInferred) {
            return operations.stream().filter(o -> o.getInferredCrudSemantics().equals(crudSemantics)).collect(Collectors.toSet());
        }
        return operations.stream().filter(o -> o.getCrudSemantics().equals(crudSemantics)).collect(Collectors.toSet());
    }

    public Set<Operation> getOperations() {
        return this.operations;
    }

    public List<ParameterElement> getReadOnlyParameters() {
        return null;
    }

    public String getResourceType() {
        return resourceType;
    }

    public boolean isInferred() {
        return isInferred;
    }
}
