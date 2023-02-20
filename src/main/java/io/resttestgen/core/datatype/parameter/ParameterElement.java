package io.resttestgen.core.datatype.parameter;

import com.google.gson.internal.LinkedTreeMap;
import io.resttestgen.core.datatype.NormalizedParameterName;
import io.resttestgen.core.datatype.ParameterName;
import io.resttestgen.core.helper.ObjectHelper;
import io.resttestgen.core.helper.Taggable;
import io.resttestgen.core.openapi.EditReadOnlyOperationException;
import io.resttestgen.core.openapi.OpenAPIParser;
import io.resttestgen.core.openapi.Operation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/*
 * In this class every function that sets a value after the Parameter creation checks the value of 'isReadOnly' field
 * in the associated operation: if true, it won't allow any modification and will throw an exception.
 * This behavior has been implemented to prevent accidental modifications to the template structure of the reference
 * OpenAPI specification. In fact, every operation (and consequently its parameters) parsed from the specification is
 * set as read-only. In order to be able to perform any modification, the operation must be cloned.
 */
public abstract class ParameterElement extends Taggable {

    protected ParameterName name;
    protected NormalizedParameterName normalizedName;
    protected String schemaName; // Name of the referred schema, if any; else null
    protected boolean required;
    protected ParameterType type;
    protected ParameterTypeFormat format;
    private ParameterLocation location; // Position of the parameter (e.g. path, header, query, etc. )
    private ParameterStyle style;
    private boolean explode;

    protected Object defaultValue;
    protected Set<Object> enumValues;
    protected Set<Object> examples;

    private final String description;

    private Operation operation; // Operation to which the parameter is associated
    private ParameterElement parent; // Reference to the parent Parameter if any; else null

    private static final String castedWarn = "' was not compliant to parameter type, but it has been " +
            "cast to fit the right type.";
    private static final String discardedWarn = "' is not compliant to parameter type. The value will be discarded.";
    private static final Logger logger = LogManager.getLogger(ParameterElement.class);

    public ParameterElement(ParameterElement parent, Map<String, Object> parameterMap, Operation operation, String name) {
        if (name != null) {
            this.name = new ParameterName(name);
        } else if (!parameterMap.containsKey("name")) {
            throw new ParameterCreationException("Missing name for parameter in operation '" + operation +
                    "' (parent: " + parent + ").");
        } else {
            this.name = new ParameterName((String) parameterMap.get("name"));
        }

        // Difference between parameter and request body/response body
        // Parameters can have a schema definition which contains type, format, default and enum values
        @SuppressWarnings("unchecked")
        Map<String, Object> sourceMap = parameterMap.containsKey("schema") ?
                (Map<String, Object>) parameterMap.get("schema") :
                parameterMap;

        this.schemaName = (String) sourceMap.get("x-schemaName");

        this.operation = operation;
        this.parent = parent;
        this.required = parameterMap.containsKey("required") ?
                (Boolean) parameterMap.get("required") :
                false;
        this.location = ParameterLocation.getLocationFromString((String) parameterMap.get("in"));
        // If style is absent apply default by OpenAPI standard
        this.style = ParameterStyle.getStyleFromString((String) parameterMap.get("style"));
        if (style == null) {
            switch (this.location) {
                case HEADER:
                case PATH:
                    this.style = ParameterStyle.SIMPLE;
                    break;
                case QUERY:
                case COOKIE:
                default:
                    this.style = ParameterStyle.FORM;
            }
        }

        Boolean specExplode = (Boolean) parameterMap.get("explode");
        this.explode = Objects.requireNonNullElseGet(specExplode, () -> this.style == ParameterStyle.FORM);

        this.type = ParameterType.getTypeFromString((String) sourceMap.get("type"));
        this.format = ParameterTypeFormat.getFormatFromString((String) sourceMap.get("format"));

        this.description = OpenAPIParser.safeGet(parameterMap, "description", String.class);

        Object defaultValue = sourceMap.get("default");
        if (defaultValue != null) {
            if (isObjectTypeCompliant(defaultValue)) {
                this.defaultValue = defaultValue;
            } else {
                try {
                    this.defaultValue = ObjectHelper.castToParameterValueType(defaultValue, type);
                    logger.warn("Default value " + defaultValue + castedWarn);
                } catch (ClassCastException e) {
                    this.defaultValue = null;
                    logger.warn("Default value " + defaultValue + discardedWarn);
                }
            }
        }
        
        this.enumValues = new HashSet<>();
        @SuppressWarnings("unchecked")
        List<Object> values = OpenAPIParser.safeGet(sourceMap, "enum", ArrayList.class);
        values.forEach(value -> {
            if (isObjectTypeCompliant(value)) {
                this.enumValues.add(value);
            } else {
                try {
                    this.enumValues.add(ObjectHelper.castToParameterValueType(value, type));
                    logger.warn("Enum value '" + value + castedWarn);
                } catch (ClassCastException e) {
                    logger.warn("Enum value '" + value + discardedWarn);
                }
            }
        });

        this.examples = new HashSet<>();
        // Example and examples should be mutually exclusive. Moreover, examples field is not allowed in request bodies.
        // The specification is parsed in a more relaxed way, pursuing fault tolerance and flexibility.
        Object exampleValue = parameterMap.get("example");
        if (exampleValue != null) {
            if (isObjectTypeCompliant(exampleValue)) {
                this.examples.add(exampleValue);
            } else {
                try {
                    this.examples.add(ObjectHelper.castToParameterValueType(exampleValue, type));
                    logger.warn("Example value " + exampleValue + " was not compliant to parameter type, but it has been " +
                            "cast to fit the right type.");
                } catch (ClassCastException e) {
                    logger.warn("Example value " + exampleValue + " is not compliant to parameter type. " +
                            "The value will be discarded.");
                }
            }
        }

        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> examples = OpenAPIParser.safeGet(parameterMap, "examples", LinkedTreeMap.class);
        examples.values().forEach(example -> {
            if (example.containsKey("value")) {
                Object value = example.get("value");
                if (isObjectTypeCompliant(value)) {
                    this.examples.add(value);
                } else {
                    try {
                        this.examples.add(ObjectHelper.castToParameterValueType(value, type));
                        logger.warn("Example value " + value + castedWarn);
                    } catch (ClassCastException e) {
                        logger.warn("Example value " + value + discardedWarn);
                    }
                }
            } else if (example.containsKey("externalValue")) {
                logger.warn("Examples containing external values are not currently supported.");
            }
        });

        this.normalizedName = NormalizedParameterName.computeParameterNormalizedName(this);
    }

    public ParameterElement (Map<String, Object> parameterMap, Operation operation, String name) {
        this(null, parameterMap, operation, name);
    }

    /*
     * Copy constructors used to clone parameters. They are declared as protected to force the use of the function
     * deepCopy externally.
     */
    protected ParameterElement(ParameterElement other) {
        name = other.name.deepClone();
        normalizedName = other.normalizedName;
        schemaName = other.schemaName;
        required = other.required;
        type = other.type; // Amedeo did: ParameterType.getTypeFromString(other.type.name());
        format = other.format;
        location = other.location;
        style = other.style;
        explode = other.explode;

        description = other.description;

        defaultValue = ObjectHelper.deepCloneObject(other.defaultValue);
        enumValues = new HashSet<>(ObjectHelper.deepCloneObject(other.enumValues));
        examples = new HashSet<>(ObjectHelper.deepCloneObject(other.examples));

        operation = other.operation;
        parent = other.parent;

        tags.addAll(other.tags);
    }

    protected ParameterElement(ParameterElement other, Operation operation, ParameterElement parent) {
        this(other);

        this.operation = operation;
        this.parent = parent;

        tags.addAll(other.tags);
    }

    protected ParameterElement(Operation operation, ParameterElement parent) {
        this.operation = operation;
        this.parent = parent;
        this.location = ParameterLocation.RESPONSE_BODY;
        this.explode = false;
        this.description = "";
        this.enumValues = new HashSet<>();
        this.examples = new HashSet<>();
    }

    public abstract ParameterElement merge(ParameterElement other);

    /**
     * To remove the parameter from the parent element (or operation).
     */
    public abstract boolean remove();

    /**
     * To replace a parameter with another one.
     * @param newParameter the new parameter to put.
     * @return true if the replacement could be completed.
     */
    public boolean replace(ParameterElement newParameter) {

        newParameter.setOperation(this.getOperation());
        newParameter.setParent(this.getParent());
        newParameter.setLocation(this.getLocation());

        // If the parameter has no parent (it is a root)
        if (getParent() == null) {
            switch (getLocation()) {
                case REQUEST_BODY:
                    if (this == getOperation().getRequestBody()) {
                        if (newParameter instanceof StructuredParameterElement) {
                            getOperation().setRequestBody((StructuredParameterElement) newParameter);
                            return true;
                        } else {
                            return false;
                        }
                    }
                    break;
                case RESPONSE_BODY:
                    if (this == getOperation().getResponseBody()) {
                        if (newParameter instanceof StructuredParameterElement) {
                            getOperation().setResponseBody((StructuredParameterElement) newParameter);
                            return true;
                        } else {
                            return false;
                        }
                    }
                    break;
                case QUERY:
                    if (getOperation().getQueryParameters().contains(this)) {
                        return getOperation().getQueryParameters().remove(this) && getOperation().getQueryParameters().add(newParameter);
                    }
                    return false;
                case PATH:
                    if (getOperation().getPathParameters().contains(this)) {
                        return getOperation().getPathParameters().remove(this) && getOperation().getQueryParameters().add(newParameter);
                    }
                    return false;
                case HEADER:
                    if (getOperation().getHeaderParameters().contains(this)) {
                        return getOperation().getHeaderParameters().remove(this) && getOperation().getQueryParameters().add(newParameter);
                    }
                    return false;
                case COOKIE:
                    if (getOperation().getCookieParameters().contains(this)) {
                        return getOperation().getCookieParameters().remove(this) && getOperation().getQueryParameters().add(newParameter);
                    }
                    return false;
            }
        }

        // If the parameter is contained in a parent element (array or object), remove it from the parent
        else {
            if (getParent() instanceof ParameterArray) {
                ((ParameterArray) getParent()).getElements().remove(this);
                ((ParameterArray) getParent()).getElements().add(newParameter);
                return true;
            } else if (getParent() instanceof ParameterObject) {
                ((ParameterObject) getParent()).getProperties().remove(this);
                ((ParameterObject) getParent()).getProperties().add(newParameter);
                return true;
            }
        }

        return false;
    }

    /**
     * Function to check whether the object passed as parameter is compliant to the Parameter type.
     * Each ParameterElement subclass implements it checking the type against the one that it expects for its
     * values/enum values/examples/etc.
     * @param o The object to be checked for compliance
     * @return True if o is compliant to the Parameter; false otherwise
     */
    public abstract boolean isObjectTypeCompliant(Object o);

    /**
     * Method to retrieve the heading for the JSON string. It was implemented to avoid errors caused by a missing
     * parameter name.
     */
    protected String getJSONHeading() {
        return name == null || name.toString().equals("") ? "" : "\"" + name + "\": ";
    }

    /**
     * Method to get the parameter as a JSON string. It can be used to construct JSON request bodies.
     * @return the JSON string.
     */
    public abstract String getJSONString();

    /**
     * Returns the JSON path for the element, e.g., owner.name
     * @return the JSON path for the element, e.g., owner.name
     */
    public abstract String getJsonPath();

    public abstract ParameterElement getParameterFromJsonPath(String jsonPath);

    /**
     * Function to retrieve the value of a Parameter as a string accordingly to given style and explode
     * @param style Describes how the parameter value will be serialized depending on the type of the parameter value
     * @param explode Parameter to change the way a specific style is rendered
     * @return A string with the rendered value
     */
    public abstract String getValueAsFormattedString (ParameterStyle style, boolean explode);

    /**
     * Shorthand for getValueAsFormattedString where the value of 'explode' is the same of the instance one.
     * @param style the style to be used for the rendering.
     * @return a string with the rendered value.
     */
    public String getValueAsFormattedString (ParameterStyle style) {
        return getValueAsFormattedString (style, this.explode);
    }

    /**
     * Shorthand for getValueAsFormattedString where the values of 'style' and 'explode' are the ones of the instance.
     * This function can be used to get the default rendering of a Parameter.
     * @return A string with the rendered value
     */
    public String getValueAsFormattedString () {
        return getValueAsFormattedString(this.style, this.explode);
    }

    public Object getValue() {
        return null;
    }

    public ParameterStyle getStyle() {
        return style;
    }

    public void setStyle(ParameterStyle style) {
        this.style = style;
    }

    public boolean isExplode() {
        return explode;
    }

    public void setExplode(boolean explode) {
        this.explode = explode;
    }

    public abstract boolean hasValue();

    public final boolean isEnum() {
        return !this.enumValues.isEmpty();
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public boolean setDefaultValue(Object defaultValue) {
        if (operation.isReadOnly()) {
            throw new EditReadOnlyOperationException(operation);
        }
        if (this.isObjectTypeCompliant(defaultValue)) {
            this.defaultValue = defaultValue;
        } else {
            try {
                this.defaultValue = ObjectHelper.castToParameterValueType(defaultValue, this.type);
                logger.warn("Example value '" + defaultValue + castedWarn);
            } catch (ClassCastException e) {
                logger.warn("Example value '" + defaultValue + discardedWarn);
                return false;
            }
        }
        return true;
    }

    public Set<Object> getEnumValues() {
        if (operation.isReadOnly()) {
            return Collections.unmodifiableSet(enumValues);
        }
        return enumValues;
    }

    public String getDescription() {
        return description;
    }

    public NormalizedParameterName getNormalizedName() {
        return this.normalizedName;
    }

    public ParameterName getName() {
        return name;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public boolean addExample(Object o) {
        if (operation.isReadOnly()) {
            throw new EditReadOnlyOperationException(operation);
        }
        if (this.isObjectTypeCompliant(o)) {
            this.examples.add(o);
        } else {
            try {
                this.examples.add(ObjectHelper.castToParameterValueType(o, this.type));
                logger.warn("Example value '" + o + castedWarn);
            } catch (ClassCastException e) {
                logger.warn("Example value '" + o + discardedWarn);
                return false;
            }
        }
        return true;
    }

    public Set<Object> getExamples() {
        if (operation.isReadOnly()) {
            return Collections.unmodifiableSet(examples);
        }
        return examples;
    }

    public ParameterType getType() {
        return type;
    }

    public ParameterTypeFormat getFormat() {
        return format;
    }

    public void setFormat(ParameterTypeFormat format) {
        // FIXME: move set format to leaves to check that type matches the format
        this.format = format;
    }

    public Operation getOperation() {
        return operation;
    }

    public void setOperation(Operation operation) {
        this.operation = operation;
    }

    public ParameterLocation getLocation() {
        return location;
    }

    public void setLocation(ParameterLocation location) {
        this.location = location;
    }

    protected void setNormalizedName(NormalizedParameterName normalizedName) {
        if (operation.isReadOnly()) {
            throw new EditReadOnlyOperationException(operation);
        }
        this.normalizedName = normalizedName;
    }

    @Override
    public String toString() {
        return this.name + " (" + normalizedName + ", " + location + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ParameterElement parameter = (ParameterElement) o;

        return Objects.equals(name, parameter.name) &&
                Objects.equals(type, parameter.type) &&
                Objects.equals(location, parameter.location) &&
                Objects.equals(operation, parameter.operation) &&
                // If even one of the parameters has null parent, then ignore normalized name. Else, consider it.
                // This behaviour is to restrict the most possible the use of normalizedName in equals
                (parent == null || parameter.parent == null || Objects.equals(normalizedName, parameter.normalizedName));
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, location, operation);
    }

    /**
     * Returns the root element of a structured parameter. If a parameter is not structured, then the root element is
     * itself.
     * @return the root element of a structured parameter.
     */
    public ParameterElement getRoot() {
        if (getParent() == null) {
            return this;
        } else {
            return getParent().getRoot();
        }
    }

    public ParameterElement getParent() {
        return parent;
    }

    public void setParent(ParameterElement parent) {
        this.parent = parent;

        // Also update operation of the parameter, to match the one of the new parent
        // FIXME: operation should be updated also when parent == null
        if (parent != null) {
            this.operation = parent.getOperation();
        }
    }

    /**
     * Clones the parameter by creating its exact, deep copy
     * @return deep copy of the parameter
     */
    public abstract ParameterElement deepClone();

    /**
     * Creates a deep copy of the parameter modifying its reference operation and parameters.
     * This function is mainly used when cloning an operation since the cloned parameters must reference to the new
     * operation instead referencing the same operation of the original parameter. New parent is necessary for the same
     * reason, since structured parameters need to give the clones of their elements/properties the reference to
     * themselves instead to the old parent.
     * @param operation New operation to be referenced.
     * @param parent New parent to be referenced.
     * @return the cloned parameter.
     */
    public abstract ParameterElement deepClone(Operation operation, ParameterElement parent);

    /**
     * Return a collection containing the arrays in the parameter element and underlying elements.
     * @return the collection of arrays in the parameter.
     */
    public abstract Collection<ParameterArray> getArrays();

    /**
     * Return a collection containing the objects in the parameter element and underlying elements.
     * @return the collection of objects in the parameter.
     */
    public abstract Collection<ParameterObject> getObjects();

    /**
     * Return a collection containing the objects in the parameter element and underlying elements. In case of arrays,
     * only the objects in reference element are returned.
     * @return the collection of objects in the parameter.
     */
    public abstract Collection<ParameterObject> getReferenceObjects();

    /**
     * Returns all parameters elements of this element.
     * @return all parameters elements of this element.
     */
    public abstract Collection<ParameterElement> getAllParameters();

    /**
     * Returns a collection containing the leaves in the parameter element and underlying elements.
     * @return the collection of leaves in the parameter
     */
    public abstract Collection<ParameterLeaf> getLeaves();

    /**
     * Get the leaves of the parameters. In case of arrays, the returned leaves are part of the reference element of the
     * array.
     * @return the reference leaves in the parameter.
     */
    public abstract Collection<ParameterLeaf> getReferenceLeaves();

    /**
     * Returns a collection containing the combined schemas in the parameter element and underlying elements.
     * @return the collection of combined schemas in the parameter
     */
    public abstract Collection<CombinedSchemaParameter> getCombinedSchemas();

    public abstract boolean isSet();

    public boolean isObject() {
        return this instanceof ParameterObject;
    }

    public boolean isArray() {
        return this instanceof ParameterArray;
    }

    public boolean isArrayOfLeaves() {
        return this instanceof ParameterArray && ((ParameterArray) this).getReferenceElement() instanceof ParameterLeaf;
    }

    public boolean isArrayOfArrays() {
        return this instanceof ParameterArray && ((ParameterArray) this).getReferenceElement() instanceof ParameterArray;
    }

    public boolean isArrayOfObjects() {
        return this instanceof ParameterArray && ((ParameterArray) this).getReferenceElement() instanceof ParameterObject;
    }

    public boolean isLeaf() {
        return this instanceof ParameterLeaf;
    }

    public boolean isNumber() {
        return this instanceof NumberParameter;
    }

    public boolean isString() {
        return this instanceof StringParameter;
    }

    public boolean isBoolean() {
        return this instanceof BooleanParameter;
    }

    public boolean isNull() {
        return this instanceof NullParameter;
    }

    /**
     * Check if a parameter element is a reference element of an array parameter.
     * @return true if parameter is reference element.
     */
    public boolean isReferenceElement() {
        return parent != null && parent instanceof ParameterArray && ((ParameterArray) parent).getReferenceElement() == this;
    }

    /**
     * Check if a parameter is an element of an array parameter.
     * @return true if parameter is an element of an array parameter.
     */
    public boolean isArrayElement() {
        return parent != null && parent instanceof ParameterArray && ((ParameterArray) parent).getElements().contains(this);
    }

    /**
     * Check if a parameter is a property of an object.
     * @return true if parameter is property of an object.
     */
    public boolean isObjectProperty() {
        return parent != null && parent instanceof ParameterObject && ((ParameterObject) parent).getProperties().contains(this);
    }
}
