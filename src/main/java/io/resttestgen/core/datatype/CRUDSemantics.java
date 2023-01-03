package io.resttestgen.core.datatype;

import io.resttestgen.core.datatype.parameter.ParameterArray;
import io.resttestgen.core.datatype.parameter.ParameterObject;
import io.resttestgen.core.openapi.Operation;

public enum CRUDSemantics {
    CREATE,
    READ,
    READ_MULTI,
    UPDATE,
    DELETE,
    NONE;

    public static CRUDSemantics inferSemantics(Operation operation) {
        CRUDSemantics inferredCrudSemantics = READ;

        switch (operation.getMethod()) {// Infer if read is actually read-multi

            case POST:
                inferredCrudSemantics = CREATE;
                break;

            case PATCH:
            case PUT:
                inferredCrudSemantics = UPDATE;
                break;

            case DELETE:
                inferredCrudSemantics = DELETE;
                break;

            case OPTIONS:
            case TRACE:
            case HEAD:
                inferredCrudSemantics = NONE;
        }

        // Infer if read is actually read-multi
        if (inferredCrudSemantics == READ && operation.getOutputParameters().get("200") != null) {
            if (operation.getOutputParameters().get("200") instanceof ParameterArray) {
                inferredCrudSemantics = READ_MULTI;
            }
            if (operation.getOutputParameters().get("200") instanceof ParameterObject) {
                ParameterObject rootObject = (ParameterObject) operation.getOutputParameters().get("200");
                if (rootObject.getProperties().size() == 1 && rootObject.getProperties().get(0) instanceof ParameterArray) {
                    inferredCrudSemantics = READ_MULTI;
                }
            }
        }

        return inferredCrudSemantics;
    }

    public static CRUDSemantics parseSemantics(String string) {
        switch (string.toLowerCase().trim()) {
            case "create":
                return CREATE;

            case "read":
                return READ;

            case "read-multi":
                return READ_MULTI;

            case "update":
                return UPDATE;

            case "delete":
                return DELETE;

            default:
                return NONE;
        }
    }
}
