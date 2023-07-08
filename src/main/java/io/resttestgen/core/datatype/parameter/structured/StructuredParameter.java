package io.resttestgen.core.datatype.parameter.structured;

import io.resttestgen.core.datatype.parameter.Parameter;
import io.resttestgen.core.datatype.parameter.visitor.Visitor;
import io.resttestgen.core.openapi.EditReadOnlyOperationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

public abstract class StructuredParameter extends Parameter {
    private static final Logger logger = LogManager.getLogger(StructuredParameter.class);
    // By default, remove a structured parameter instead keeping it empty when representing it
    private boolean keepIfEmpty = false;

    public StructuredParameter(Map<String, Object> schema, String name) {
        super(schema, name);
    }

    public StructuredParameter(Map<String, Object> schema) {
        this(schema, null);
    }

    protected StructuredParameter(StructuredParameter other) {
        super(other);
        this.keepIfEmpty = other.keepIfEmpty;
    }

    protected StructuredParameter(Parameter other) {
        super(other);
    }

    protected StructuredParameter() {}

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public abstract StructuredParameter deepClone();

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
        if (getOperation() != null && getOperation().isReadOnly()) {
            throw new EditReadOnlyOperationException(getOperation());
        }
        this.keepIfEmpty = keepIfEmpty;
    }

    @Override
    public String getJsonPath() {
        if (getParent() == null) {
            return "$";
        }

        else if (getParent() instanceof ArrayParameter) {

            // If this is the referenceElement of the array, return index = -1
            if (this == ((ArrayParameter) getParent()).getReferenceElement()) {
                return getParent().getJsonPath() + "[-1]";
            }

            // If this is an element of the array, return its index
            else if (((ArrayParameter) getParent()).getElements().contains(this)) {
                return getParent().getJsonPath() + "[" + ((ArrayParameter) getParent()).getElements().indexOf(this) + "]";
            }

            // If this is not contained in the array, return null
            else {
                return null;
            }
        }

        else {
            return getParent().getJsonPath() + "['" + this.getName() + "']";
        }
    }

    @Override
    public boolean remove() {
        if (getOperation() != null && getOperation().isReadOnly()) {
            throw new EditReadOnlyOperationException(getOperation());
        }

        // If the leaf has no parent (it is a root), then remove it from the operation
        if (getParent() == null) {
            switch (getLocation()) {
                case REQUEST_BODY:
                    if (this == getOperation().getRequestBody()) {
                        getOperation().setRequestBody(null);
                        return true;
                    }
                    return false;
                case RESPONSE_BODY:
                    if (this == getOperation().getResponseBody()) {
                        getOperation().setResponseBody(null);
                        return true;
                    }
                    return false;
                default:
                    return getOperation().removeParameter(getLocation(), this);
            }
        }

        // If the leaf is contained in a parent element (array or object), remove it from the parent
        return getParent().removeChild(this);
    }

    /**
     * Helper function used to remove null-valued (for Leaves) and empty (for structured) children Parameters
     * in order to have a valid representation of the structured parameter.
     */
    public abstract void removeUninitializedParameters();

}
