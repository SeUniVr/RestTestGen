package io.resttestgen.core.datatype;

import io.resttestgen.core.datatype.parameter.structured.ArrayParameter;
import io.resttestgen.core.datatype.parameter.structured.ObjectParameter;
import io.resttestgen.core.openapi.Operation;

public enum OperationSemantics {
    CREATE,
    READ,
    READ_MULTI,
    UPDATE,
    DELETE,
    SIGN_UP,
    LOG_IN,
    LOG_OUT,
    OTHER,
    UNKNOWN;

    public static OperationSemantics inferSemantics(Operation operation) {
        OperationSemantics inferredOperationSemantics = READ;

        switch (operation.getMethod()) {

            case POST:
                inferredOperationSemantics = CREATE;
                break;

            case PATCH:
            case PUT:
                inferredOperationSemantics = UPDATE;
                break;

            case DELETE:
                inferredOperationSemantics = DELETE;
                break;

            case OPTIONS:
            case TRACE:
            case HEAD:
                inferredOperationSemantics = OTHER;
        }

        // Infer if read is actually read-multi
        if (inferredOperationSemantics == READ && operation.getSuccessfulOutputParameters() != null) {
            if (operation.getSuccessfulOutputParameters() instanceof ArrayParameter) {
                inferredOperationSemantics = READ_MULTI;
            }
            if (operation.getSuccessfulOutputParameters() instanceof ObjectParameter) {
                ObjectParameter rootObject = (ObjectParameter) operation.getSuccessfulOutputParameters();
                if (rootObject.getProperties().size() == 1 && rootObject.getProperties().get(0) instanceof ArrayParameter) {
                    inferredOperationSemantics = READ_MULTI;
                }
            }
        }

        return inferredOperationSemantics;
    }

    public static OperationSemantics parseSemantics(String string) {
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

            case "signup":
                return SIGN_UP;

            case "login":
                return LOG_IN;

            case "logout":
                return LOG_OUT;

            default:
                return UNKNOWN;
        }
    }
}
