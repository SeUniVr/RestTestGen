package io.resttestgen.core.datatype.parameter;

import io.resttestgen.core.helper.ObjectHelper;
import io.resttestgen.core.openapi.EditReadOnlyOperationException;
import io.resttestgen.core.openapi.OpenAPIParser;
import io.resttestgen.core.openapi.Operation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public abstract class ParameterLeaf extends ParameterElement {

    private static final Logger logger = LogManager.getLogger(ParameterLeaf.class);

    protected Object value;
    protected boolean resourceIdentifier = false;
    protected boolean inferredResourceIdentifier = false;

    public ParameterLeaf(ParameterElement parent, Map<String, Object> parameterMap, Operation operation, String name) {
        super(parent, parameterMap, operation, name);
        if (parameterMap.containsKey("x-crudResourceIdentifier")) {
            this.resourceIdentifier = OpenAPIParser.safeGet(parameterMap, "x-crudResourceIdentifier", Boolean.class);
        }
    }

    public ParameterLeaf(ParameterElement parent, Map<String, Object> parameterMap, Operation operation) {
        this(parent, parameterMap, operation,null);
        if (parameterMap.containsKey("x-crudResourceIdentifier")) {
            this.resourceIdentifier = OpenAPIParser.safeGet(parameterMap, "x-crudResourceIdentifier", Boolean.class);
        }
    }

    protected ParameterLeaf(ParameterLeaf other) {
        super(other);
        value = ObjectHelper.deepCloneObject(other.value);
        resourceIdentifier = other.resourceIdentifier;
        inferredResourceIdentifier = other.inferredResourceIdentifier;
    }

    protected ParameterLeaf(ParameterLeaf other, Operation operation, ParameterElement parent) {
        super(other, operation, parent);
        value = ObjectHelper.deepCloneObject(other.value);
        resourceIdentifier = other.resourceIdentifier;
        inferredResourceIdentifier = other.inferredResourceIdentifier;
    }

    protected ParameterLeaf(ParameterElement other) {
        super(other);
    }

    public ParameterLeaf(Operation operation, ParameterElement parent) {
        super(operation, parent);
    }

    public abstract boolean isValueCompliant(Object value);

    private boolean inferResourceIdentifier() {
        return normalizedName.toString().toLowerCase().endsWith("id") ||
                normalizedName.toString().toLowerCase().endsWith("usernam") ||
                normalizedName.toString().toLowerCase().endsWith("username");
    }

    public String getValueAsFormattedString(ParameterStyle style, boolean explode) {
        if (value == null) {
            logger.warn("Called 'getValueAsFormattedString' function on null-valued parameter.");
            return "";
        }

        String encodedValue = getConcreteValue().toString();

        // Encode body parameters in x-www-form-urlencoded
        if (this.getLocation() == ParameterLocation.REQUEST_BODY &&
                this.getOperation().getRequestContentType().contains("application/x-www-form-urlencoded")) {
            encodedValue = URLEncoder.encode(encodedValue, StandardCharsets.UTF_8);
        }

        // Remove slashes in path parameters
        if (this.getLocation() == ParameterLocation.PATH) {
            encodedValue = encodedValue.replaceAll("/", "").replaceAll("\\\\", "");
        }

        // If numeric value (double) is integer (not decimal), convert it to long to prevent the printing of .0
        if (getConcreteValue() instanceof Double && ((Double) getConcreteValue()) % 1 == 0) {
            encodedValue = Long.toString(((Double) getConcreteValue()).longValue());
        }

        switch (style) {
            case MATRIX:
                return ";" + getName().toString() + "=" + encodedValue;
            case LABEL:
                return "." + encodedValue;
            case FORM:
                return getName().toString() + "=" + encodedValue;
            case SIMPLE:
                return encodedValue;
            case SPACE_DELIMITED:
            case PIPE_DELIMITED:
            case DEEP_OBJECT:
            default:
                ParameterStyle parameterStyle = getStyle();
                if (parameterStyle == ParameterStyle.SPACE_DELIMITED ||
                        parameterStyle == ParameterStyle.PIPE_DELIMITED ||
                        parameterStyle == ParameterStyle.DEEP_OBJECT) {
                    parameterStyle = ParameterStyle.SIMPLE;
                }
                logger.warn(getName() +
                        ": Style not consistent with parameter type. Returning '" + parameterStyle + "' style.");
                return getValueAsFormattedString(parameterStyle);
        }
    }

    @Override
    public Object getValue() {
        return value;
    }

    /**
     * @return the concrete value of the parameter, i.e., if the value is a reference to another leaf, the concrete
     * value of that leaf is returned.
     */
    public Object getConcreteValue() {
        if (value instanceof ParameterLeaf) {
            return ((ParameterLeaf) value).getConcreteValue();
        }
        return value;
    }

    public void setValue(Object value) {
        if (getOperation().isReadOnly()) {
            throw new EditReadOnlyOperationException(getOperation());
        }
        if (this.isObjectTypeCompliant(value)) {
            this.value = value;
        } else {
            logger.warn("Setting value '" + value + "' to parameter '" +
                    this.getName() + "' is not possible due to type mismatch.");
        }
    }

    public void removeValue() {
        this.value = null;
    }

    @Override
    public boolean isSet() {
        return getConcreteValue() != null || this instanceof NullParameter;
    }

    public String getJsonPath() {

        String thisJsonPath = "['" + this.getName() + "']";

        if (getParent() == null) {
            return "$" + thisJsonPath;
        } else if (getParent() instanceof ParameterArray) {

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
        } else {
            return getParent().getJsonPath() + thisJsonPath;
        }
    }


    public boolean isResourceIdentifier() {
        return resourceIdentifier;
    }

    public boolean isInferredResourceIdentifier() {
        return inferredResourceIdentifier;
    }

    public void setInferredResourceIdentifier(boolean inferredResourceIdentifier) {
        this.inferredResourceIdentifier = inferredResourceIdentifier;
    }

    @Override
    public boolean hasValue() {
        if (value == null) {
            logger.warn("Parameter " + getName() + " has an invalid value.");
        }
        return value != null;
    }

    @Override
    public Set<ParameterElement> getAllParameters() {
        HashSet<ParameterElement> parameters = new HashSet<>();
        parameters.add(this);
        return parameters;
    }

    @Override
    public Collection<ParameterLeaf> getLeaves() {
        Collection<ParameterLeaf> leaves = new LinkedList<>();
        leaves.add(this);
        return leaves;
    }

    @Override
    public Collection<ParameterLeaf> getReferenceLeaves() {
        return getLeaves();
    }

    @Override
    public Collection<ParameterObject> getObjects() {
        return new LinkedList<>();
    }

    @Override
    public Collection<ParameterObject> getReferenceObjects() {
        return new LinkedList<>();
    }

    @Override
    public Collection<CombinedSchemaParameter> getCombinedSchemas() {
        return new LinkedList<>();
    }

    @Override
    public boolean remove() {

        // If the leaf has no parent (it is a root), then remove it from the operation
        if (getParent() == null) {
            switch (getLocation()) {
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

    /* TODO: remove
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if(o == null || getClass() != o.getClass()) {
            return false;
        }
        return false;
    }
*/

    /**
     * Return a parameter element according to its JSON path.
     * @param jsonPath the JSON path of the parameter we want to get.
     * @return the parameter matching the JSON path.
     */
    @Override
    public ParameterElement getParameterFromJsonPath(String jsonPath) {

        // If the JSON path starts with $, then start the search from the root element
        ParameterElement rootElement = getRoot();
        if (this != rootElement && jsonPath.startsWith("$")) {
            return rootElement.getParameterFromJsonPath(jsonPath);
        }

        // If the JSON path starts with $, remove it
        if (jsonPath.startsWith("$")) {
            jsonPath = jsonPath.substring(1);
        }

        int start = jsonPath.indexOf("[");
        int end = jsonPath.indexOf("]");

        if (start >= 0 && end >= 0) {
            if ((jsonPath.charAt(start + 1) == '\'' || jsonPath.charAt(start + 1) == '"') &&
                    (jsonPath.charAt(end - 1) == '\'' || jsonPath.charAt(end - 1) == '"')) {
                String leafName = jsonPath.substring(start + 2, end - 1);
                if (getName().toString().equals(leafName)) {
                    return this;
                }
                return null;
            } else {
                // Missing quotes or double quotes
                return null;
            }
        } else {
            // Missing parenthesis
            return null;
        }
    }
}
