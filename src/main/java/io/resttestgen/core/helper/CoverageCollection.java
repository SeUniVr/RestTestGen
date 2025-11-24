package io.resttestgen.core.helper;

import io.resttestgen.core.datatype.HttpMethod;
import io.resttestgen.core.datatype.HttpStatusCode;
import io.resttestgen.core.openapi.Operation;
import io.resttestgen.implementation.coveragemetric.ParameterElementWrapper;

import java.util.HashMap;
import java.util.Set;

public class CoverageCollection {
    // Path coverage
    private final Set<String> documentedPaths;
    private final Set<String> testedPaths;

    // Operation coverage
    private final Set<Operation> documentedOperations;
    private final Set<Operation> testedOperations;

    // Status code coverage
    private final HashMap<Operation, Set<HttpStatusCode>> documentedStatusCodes;
    private final HashMap<Operation, Set<HttpStatusCode>> testedStatusCodes;

    // Parameter coverage
    private final HashMap<Operation, Set<ParameterElementWrapper>> documentedParameters;
    private final HashMap<Operation, Set<ParameterElementWrapper>> testedParameters;

    // Parameter value coverage
    private final HashMap<Operation, HashMap<ParameterElementWrapper, Set<Object>>> documentedValues;
    private final HashMap<Operation, HashMap<ParameterElementWrapper, Set<Object>>> testedValues;

    public CoverageCollection() {
        this.documentedPaths = Set.of();
        this.testedPaths = Set.of();
        this.documentedOperations = Set.of();
        this.testedOperations = Set.of();
        this.documentedStatusCodes = new HashMap<>();
        this.testedStatusCodes = new HashMap<>();
        this.documentedParameters = new HashMap<>();
        this.testedParameters = new HashMap<>();
        this.documentedValues = new HashMap<>();
        this.testedValues = new HashMap<>();
    }

    public CoverageCollection(Set<String> documentedPaths, Set<String> testedPaths,
                              Set<Operation> documentedOperations, Set<Operation> testedOperations,
                              HashMap<Operation, Set<HttpStatusCode>> documentedStatusCodes, HashMap<Operation, Set<HttpStatusCode>> testedStatusCodes,
                              HashMap<Operation, Set<ParameterElementWrapper>> documentedParameters, HashMap<Operation, Set<ParameterElementWrapper>> testedParameters,
                              HashMap<Operation, HashMap<ParameterElementWrapper, Set<Object>>> documentedValues, HashMap<Operation, HashMap<ParameterElementWrapper, Set<Object>>> testedValues) {
        this.documentedPaths = documentedPaths;
        this.testedPaths = testedPaths;
        this.documentedOperations = documentedOperations;
        this.testedOperations = testedOperations;
        this.documentedStatusCodes = documentedStatusCodes;
        this.testedStatusCodes = testedStatusCodes;
        this.documentedParameters = documentedParameters;
        this.testedParameters = testedParameters;
        this.documentedValues = documentedValues;
        this.testedValues = testedValues;
    }

    public boolean isPathTested(String path) {
        return testedPaths.contains(path);
    }

    public boolean isPathDocumented(String path) {
        return documentedPaths.contains(path);
    }

    public boolean isOperationDocumented(Operation operation) {
        if (operation == null) {
            return false;
        }
        return documentedOperations.contains(operation);
    }

    public boolean isOperationDocumented(String endpoint, HttpMethod method) {
        return documentedOperations.stream()
                .anyMatch(op -> op.getEndpoint().equals(endpoint) && op.getMethod().equals(method));
    }

    public boolean isOperationTested(Operation operation) {
        if (operation == null) {
            return false;
        }
        return testedOperations.contains(operation);
    }

    public boolean isOperationTested(String endpoint, HttpMethod method) {
        return testedOperations.stream()
                .anyMatch(op -> op.getEndpoint().equals(endpoint) && op.getMethod().equals(method));
    }

    public boolean isStatusCodeDocumented(Operation operation, HttpStatusCode statusCode) {
        if (operation == null || documentedStatusCodes.get(operation) == null) {
            return false;
        }
        return documentedStatusCodes.get(operation).contains(statusCode);
    }

    public boolean isStatusCodeTested(Operation operation, HttpStatusCode statusCode) {
        if (operation == null || testedStatusCodes.get(operation) == null) {
            return false;
        }
        return testedStatusCodes.get(operation).contains(statusCode);
    }

    public boolean isParameterDocumented(Operation operation, ParameterElementWrapper parameter) {
        if (operation == null || documentedParameters.get(operation) == null) {
            return false;
        }
        return documentedParameters.get(operation).contains(parameter);
    }

    public boolean isParameterTested(Operation operation, ParameterElementWrapper parameter) {
        if (operation == null || testedParameters.get(operation) == null) {
            return false;
        }
        return testedParameters.get(operation).contains(parameter);
    }

    public boolean isParameterValueDocumented(Operation operation, ParameterElementWrapper parameter, Object value) {
        if (operation == null || parameter == null || documentedValues.get(operation) == null || documentedValues.get(operation).get(parameter) == null) {
            return false;
        }
        return documentedValues.get(operation).get(parameter).contains(value);
    }

    public boolean isParameterValueTested(Operation operation, ParameterElementWrapper parameter, Object value) {
        if (operation == null || parameter == null || testedValues.get(operation) == null || testedValues.get(operation).get(parameter) == null) {
            return false;
        }
        return testedValues.get(operation).get(parameter).contains(value);
    }
}
