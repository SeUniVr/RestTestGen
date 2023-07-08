package io.resttestgen.core.datatype.parameter;

import com.google.gson.internal.LinkedTreeMap;
import io.resttestgen.core.datatype.NormalizedParameterName;
import io.resttestgen.core.datatype.ParameterName;
import io.resttestgen.core.datatype.parameter.attributes.ParameterLocation;
import io.resttestgen.core.datatype.parameter.attributes.ParameterStyle;
import io.resttestgen.core.datatype.parameter.attributes.ParameterType;
import io.resttestgen.core.datatype.parameter.attributes.ParameterTypeFormat;
import io.resttestgen.core.datatype.parameter.exceptions.ParameterCreationException;
import io.resttestgen.core.datatype.parameter.structured.StructuredParameter;
import io.resttestgen.core.datatype.parameter.visitor.Visitor;
import io.resttestgen.core.helper.ObjectHelper;
import io.resttestgen.core.helper.RestPathHelper;
import io.resttestgen.core.helper.Taggable;
import io.resttestgen.core.openapi.EditReadOnlyOperationException;
import io.resttestgen.core.openapi.OpenApiParser;
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
public abstract class Parameter extends Taggable {
    protected ParameterName name;
    protected NormalizedParameterName normalizedName;
    protected String schemaName; // Name of the referred schema, if any; else null
    protected boolean required;
    protected ParameterTypeFormat format = ParameterTypeFormat.MISSING;
    private ParameterLocation location = ParameterLocation.UNKNOWN; // Position of the parameter (e.g. path, header, query, etc. )
    private ParameterStyle style = ParameterStyle.FORM;
    protected ParameterType type = ParameterType.MISSING;
    private boolean explode;

    protected Object defaultValue;
    protected Set<Object> enumValues = new HashSet<>();
    protected Set<Object> examples = new HashSet<>();

    private String description;

    private Operation operation; // Operation to which the parameter is associated
    private Parameter parent; // Reference to the parent Parameter if any; else null

    private static final String castedWarn = "' was not compliant to parameter type, but it has been " +
            "cast to fit the right type.";
    private static final String discardedWarn = "' is not compliant to parameter type. The value will be discarded.";
    private static final Logger logger = LogManager.getLogger(Parameter.class);

    public Parameter(Map<String, Object> parameterMap, String name) {
        if (name != null) {
            this.name = new ParameterName(name);
        } else if (!parameterMap.containsKey("name")) {
            throw new ParameterCreationException("Missing name for parameter");
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

        this.description = OpenApiParser.safeGet(parameterMap, "description", String.class);

        setDefaultValue(sourceMap.get("default"));

        @SuppressWarnings("unchecked")
        List<Object> values = OpenApiParser.safeGet(sourceMap, "enum", ArrayList.class);
        values.forEach(value -> {
            if (isObjectTypeCompliant(value)) {
                enumValues.add(value);
            } else {
                try {
                    enumValues.add(ObjectHelper.castToParameterValueType(value, type));
                    logger.warn("Enum value '" + value + castedWarn);
                } catch (ClassCastException e) {
                    logger.warn("Enum value '" + value + discardedWarn);
                }
            }
        });

        // Example and examples should be mutually exclusive. Moreover, examples field is not allowed in request bodies.
        // The specification is parsed in a more relaxed way, pursuing fault tolerance and flexibility.
        Object exampleValue = parameterMap.get("example");
        if (exampleValue != null) {
            if (isObjectTypeCompliant(exampleValue)) {
                examples.add(exampleValue);
            } else {
                try {
                    examples.add(ObjectHelper.castToParameterValueType(exampleValue, type));
                    logger.warn("Example value '" + exampleValue + castedWarn);
                } catch (ClassCastException e) {
                    logger.warn("Example value '" + exampleValue + discardedWarn);
                }
            }
        }

        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> examples = OpenApiParser.safeGet(parameterMap, "examples", LinkedTreeMap.class);
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
    }

    /*
     * Copy constructors used to clone parameters. They are declared as protected to force the use of the function
     * deepCopy externally.
     */
    protected Parameter(Parameter other) {
        name = other.name.deepClone();
        normalizedName = other.normalizedName;
        schemaName = other.schemaName;
        required = other.required;
        format = other.format;
        location = other.location;
        style = other.style;
        explode = other.explode;
        type = other.type;

        description = other.description;

        defaultValue = ObjectHelper.deepCloneObject(other.defaultValue);
        enumValues.addAll(ObjectHelper.deepCloneObject(other.enumValues));
        examples.addAll(ObjectHelper.deepCloneObject(other.examples));

        operation = null;
        parent = null;

        tags.addAll(other.tags);
    }

    public Parameter() {}

    public abstract Collection<Parameter> getChildren();


    /**
     * This function adds a child to a parameter if it is more than a simple parameter
     * @param parameter to add
     * @return true if the child is added
     */
    public abstract boolean addChild(Parameter parameter);

    /**
     * This function removes a child from a parameter if it is more than a simple parameter
     * @param parameter to add
     * @return true if the child is removed
     */
    public abstract boolean removeChild(Parameter parameter);

    /**
     * this method is provided in order to implement the visitor pattern
     * @param visitor that will visit the parameter
     * @return the output from the visitor interface
     */
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }

    public abstract Parameter merge(Parameter other);

    /**
     * To remove the parameter from the parent element (or operation).
     */
    public abstract boolean remove();

    /**
     * To replace a parameter with another one.
     * @param newParameter the new parameter to put.
     * @return true if the replacement could be completed.
     */
    public boolean replace(Parameter newParameter) {
        Operation operation = getOperation();
        Parameter parent = getParent();

        newParameter.setOperation(operation);
        newParameter.setParent(parent);
        newParameter.setLocation(getLocation());

        // If the parameter has no parent (it is a root)
        if (getParent() == null) {
            switch (getLocation()) {
                case REQUEST_BODY:
                    if (this == operation.getRequestBody()) {
                        if (newParameter instanceof StructuredParameter) {
                            operation.setRequestBody((StructuredParameter) newParameter);
                            return true;
                        }
                    }
                    return false;
                case RESPONSE_BODY:
                    if (this == getOperation().getResponseBody()) {
                        if (newParameter instanceof StructuredParameter) {
                            operation.setResponseBody((StructuredParameter) newParameter);
                            return true;
                        }
                    } else {
                        for (String code: operation.getOutputParameters().keySet()) {
                            StructuredParameter element = operation.getOutputParameters().get(code);
                            if (element == this && newParameter instanceof StructuredParameter) {
                                operation.putOutputParameter(code, (StructuredParameter) newParameter);
                                return true;
                            }
                        }
                    }
                    return false;
                default:
                    return operation.removeParameter(getLocation(), this)
                            && operation.addParameter(getLocation(), newParameter);
            }
        }

        // If the parameter is contained in a parent element (array or object), remove it from the parent
        return parent.removeChild(this) && parent.addChild(newParameter);
    }

    /**
     * Function to check whether the object passed as parameter is compliant to the Parameter type.
     * Each Parameter subclass implements it checking the type against the one that it expects for its
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

    public abstract Parameter getParameterFromJsonPath(String jsonPath);

    public String getRestPath() {
        return RestPathHelper.getRestPath(this);
    }

    public Parameter getParameterByRestPath(String restPath) {
        return RestPathHelper.getParameterByRestPath(this, restPath);
    }

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
        return getValueAsFormattedString(style, explode);
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
        return !enumValues.isEmpty();
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(Object defaultValue) {
        if (operation != null && operation.isReadOnly()) {
            throw new EditReadOnlyOperationException(operation);
        }
        if (this.isObjectTypeCompliant(defaultValue)) {
            this.defaultValue = defaultValue;
        } else {
            if (defaultValue != null) {
                try {
                    this.defaultValue = ObjectHelper.castToParameterValueType(defaultValue, getType());
                    logger.warn("Default value '" + defaultValue + castedWarn);
                } catch (ClassCastException e) {
                    logger.warn("Default value '" + defaultValue + discardedWarn);
                }
            }
        }
    }

    public Set<Object> getEnumValues() {
        return Collections.unmodifiableSet(enumValues);
    }

    public String getDescription() {
        return description;
    }

    public NormalizedParameterName getNormalizedName() {
        if (normalizedName == null) {
            normalizedName = NormalizedParameterName.computeParameterNormalizedName(this);
        }

        return normalizedName;
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

    public boolean addEnumValue(Object o) {
        if (operation != null && operation.isReadOnly()) {
            throw new EditReadOnlyOperationException(operation);
        }
        if (isObjectTypeCompliant(o)) {
            enumValues.add(o);
        } else {
            try {
                enumValues.add(ObjectHelper.castToParameterValueType(o, type));
                logger.warn("Enum value '" + o + castedWarn);
            } catch (ClassCastException e) {
                logger.warn("Enum value '" + o + discardedWarn);
                return false;
            }
        }
        return true;
    }

    public boolean addExample(Object o) {
        if (operation != null && operation.isReadOnly()) {
            throw new EditReadOnlyOperationException(operation);
        }
        if (this.isObjectTypeCompliant(o)) {
            this.examples.add(o);
        } else {
            try {
                this.examples.add(ObjectHelper.castToParameterValueType(o, getType()));
                logger.warn("Example value '" + o + castedWarn);
            } catch (ClassCastException e) {
                logger.warn("Example value '" + o + discardedWarn);
                return false;
            }
        }
        return true;
    }

    public Set<Object> getExamples() {
        return Collections.unmodifiableSet(examples);
    }

    public ParameterType getType() { return type; }

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

    /**
     * Sets the operation, it should be overriden in more complex parameters
     * classes in order to have consistent updates and or propagate it to the children
     * @param operation to assign this parameter to
     */
    public void setOperation(Operation operation) {
        if (getOperation() != null && getOperation().isReadOnly()) {
            throw new EditReadOnlyOperationException(getOperation());
        }

        this.operation = operation;
        getChildren().forEach(child -> child.setOperation(operation));
    }

    public ParameterLocation getLocation() {
        return location;
    }

    public void setLocation(ParameterLocation location) {
        if (getOperation() != null && getOperation().isReadOnly()) {
            throw new EditReadOnlyOperationException(getOperation());
        }

        this.location = location;
        getChildren().forEach(child -> child.setLocation(location));
    }

    public void setName(ParameterName name) {
        if (getOperation() != null && getOperation().isReadOnly()) {
            throw new EditReadOnlyOperationException(getOperation());
        }

        this.name = name;
    }

    protected void setNormalizedName(NormalizedParameterName normalizedName) {
        if (operation != null && operation.isReadOnly()) {
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
        Parameter parameter = (Parameter) o;

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
        return Objects.hash(name, getType(), location, parent, operation);
    }

    /**
     * Returns the root element of a structured parameter. If a parameter is not structured, then the root element is
     * itself.
     * @return the root element of a structured parameter.
     */
    public Parameter getRoot() {
        if (getParent() == null) {
            return this;
        } else {
            return getParent().getRoot();
        }
    }

    public Parameter getParent() {
        return parent;
    }

    public void setParent(Parameter parent) {
        if (getOperation() != null && getOperation().isReadOnly()) {
            throw new EditReadOnlyOperationException(getOperation());
        }

        if (this.parent != null) {
            this.parent.removeChild(this);
        }

        this.parent = parent;

        // Also update operation of the parameter, to match the one of the new parent
        if (parent != null) {
            setLocation(parent.getLocation());
            setOperation(parent.getOperation());
        }
    }

    /**
     * Clones the parameter by creating its exact, deep copy
     * @return deep copy of the parameter
     */
    public abstract Parameter deepClone();

    /**
     * Returns all parameters elements of this element.
     * @return all parameters elements of this element.
     */
    public abstract Collection<Parameter> getAllParameters();

    public abstract boolean isSet();
}
