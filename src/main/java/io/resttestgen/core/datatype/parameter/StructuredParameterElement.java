package io.resttestgen.core.datatype.parameter;

import io.resttestgen.core.openapi.EditReadOnlyOperationException;
import io.resttestgen.core.openapi.Operation;

import java.util.Map;

public abstract class StructuredParameterElement extends ParameterElement {

    // By default, remove a structured parameter instead keeping it empty when representing it
    private boolean keepIfEmpty = false;

    public StructuredParameterElement(ParameterElement parent, Map<String, Object> schema, Operation operation, String name) {
        super(parent, schema, operation, name);
    }

    public StructuredParameterElement(ParameterElement parent, Map<String, Object> schema, Operation operation) {
        this(parent, schema, operation, null);
    }

    protected StructuredParameterElement(StructuredParameterElement other) {
        super(other);
    }

    protected StructuredParameterElement(StructuredParameterElement other, Operation operation, ParameterElement parent) {
        super(other, operation, parent);
    }

    protected StructuredParameterElement(ParameterElement other) {
        super(other);
    }

    @Override
    public abstract StructuredParameterElement deepClone();

    @Override
    public abstract StructuredParameterElement deepClone(Operation operation, ParameterElement parent);

    /**
     * Function that returns whether the structured parameter is empty or not.
     * @return Boolean value representing whether the structured parameter is empty
     */
    public abstract boolean isEmpty();

    /**
     * Function to retrieve the boolean value that is used to keep track if the structured parameter should be kept
     * if empty or not.
     * @return Boolean value representing whether the structured parameter should be represented when empty
     */
    public final boolean isKeepIfEmpty() {
        return keepIfEmpty;
    }

    public final void setKeepIfEmpty(boolean keepIfEmpty) {
        if (getOperation().isReadOnly()) {
            throw new EditReadOnlyOperationException(getOperation());
        }
        this.keepIfEmpty = keepIfEmpty;
    }

    /**
     * Helper function used to remove null-valued (for Leaves) and empty (for structured) children Parameters
     * in order to have a valid representation of the structured parameter.
     */
    public abstract void removeUninitializedParameters();
}
