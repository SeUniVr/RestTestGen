package io.resttestgen.core.datatype.parameter;

import io.resttestgen.core.openapi.EditReadOnlyOperationException;
import io.resttestgen.core.openapi.Operation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

public abstract class StructuredParameterElement extends ParameterElement {
    private static final Logger logger = LogManager.getLogger(StructuredParameterElement.class);
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
        this.keepIfEmpty = other.keepIfEmpty;
    }

    protected StructuredParameterElement(StructuredParameterElement other, Operation operation, ParameterElement parent) {
        super(other, operation, parent);
    }

    protected StructuredParameterElement(ParameterElement other) {
        super(other);
    }

    public StructuredParameterElement(Operation operation, ParameterElement parent) {
        super(operation, parent);
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

    @Override
    public String getJsonPath() {
        if (getParent() == null) {
            return "$";
        }

        else if (getParent() instanceof ParameterArray) {

            // If this is the referenceElement of the array, return index = -1
            if (this == ((ParameterArray) getParent()).getReferenceElement()) {
                return getParent().getJsonPath() + "[-1]";
            }

            // If this is an element of the array, return its index
            else if (((ParameterArray) getParent()).getElements().contains(this)) {
                return getParent().getJsonPath() + "[" + ((ParameterArray) getParent()).getElements().indexOf(this) + "]";
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

    // TODO: remove
    //public abstract ParameterElement getElementByJsonPath(String jsonPath);

    @Override
    public boolean remove() {

        // If the leaf has no parent (it is a root), then remove it from the operation
        if (getParent() == null) {
            switch (getLocation()) {
                case REQUEST_BODY:
                    if (this == getOperation().getRequestBody()) {
                        getOperation().setRequestBody(null);
                        return true;
                    }
                    break;
                case RESPONSE_BODY:
                    if (this == getOperation().getResponseBody()) {
                        getOperation().setResponseBody(null);
                        return true;
                    }
                    break;
                case QUERY:
                    return getOperation().getQueryParameters().remove(this);
                case PATH:
                    return getOperation().getPathParameters().remove(this);
                case HEADER:
                    return getOperation().getHeaderParameters().remove(this);
                case COOKIE:
                    return getOperation().getCookieParameters().remove(this);
            }
        }

        // If the leaf is contained in a parent element (array or object), remove it from the parent
        else {
            if (getParent() instanceof ParameterArray) {
                return ((ParameterArray) getParent()).getElements().remove(this);
            } else if (getParent() instanceof ParameterObject) {
                return ((ParameterObject) getParent()).getProperties().remove(this);
            }
        }

        return false;
    }

    /**
     * Helper function used to remove null-valued (for Leaves) and empty (for structured) children Parameters
     * in order to have a valid representation of the structured parameter.
     */
    public abstract void removeUninitializedParameters();

}
