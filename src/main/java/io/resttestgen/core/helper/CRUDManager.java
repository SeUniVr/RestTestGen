package io.resttestgen.core.helper;

import io.resttestgen.core.openapi.OpenAPI;
import io.resttestgen.core.openapi.Operation;

import java.util.*;

/**
 * Helper class to manage batches of CRUD operations. Operations belonging to the same batch are operations that
 * operate on the same target object.
 */
public class CRUDManager {

    private final Set<String> resourceTypes;
    private Map<String, CRUDGroup> groups;

    private final Set<String> inferredResourceTypes;
    private Map<String, CRUDGroup> inferredGroups;

    public CRUDManager(OpenAPI openAPI) {
        this.groups = new HashMap<>();
        this.resourceTypes = collectResourceTypes(openAPI);
        for (String resourceType : resourceTypes) {
            groups.put(resourceType, new CRUDGroup(openAPI, resourceType, false));
        }

        this.inferredGroups = new HashMap<>();
        this.inferredResourceTypes = collectInferredResourceTypes(openAPI);
        for (String inferredResourceType : inferredResourceTypes) {
            inferredGroups.put(inferredResourceType, new CRUDGroup(openAPI, inferredResourceType, true));
        }
    }

    public Set<String> getResourceTypes() {
        return resourceTypes;
    }

    public Collection<CRUDGroup> getGroups() {
        return groups.values();
    }

    public void setGroups(Map<String, CRUDGroup> groups) {
        this.groups = groups;
    }

    public Collection<CRUDGroup> getInferredGroups() {
        return inferredGroups.values();
    }

    public CRUDGroup getGroup(String targetObjectID) {
        return groups.get(targetObjectID);
    }

    // Collect in a set all the target object IDs of operations
    public static Set<String> collectResourceTypes(OpenAPI openAPI) {
        Set<String> resourceTypes = new HashSet<>();
        for (Operation operation : openAPI.getOperations()) {
            if (operation.getCrudResourceType() != null && !operation.getCrudResourceType().equals("")) {
                resourceTypes.add(operation.getCrudResourceType());
            }
        }
        return resourceTypes;
    }

    public static Set<String> collectInferredResourceTypes(OpenAPI openAPI) {
        Set<String> inferredResourceTypes = new HashSet<>();
        for (Operation operation : openAPI.getOperations()) {
            if (operation.getInferredCrudResourceType() != null && !operation.getInferredCrudResourceType().equals("")) {
                inferredResourceTypes.add(operation.getInferredCrudResourceType());
            }
        }
        return inferredResourceTypes;
    }
}
