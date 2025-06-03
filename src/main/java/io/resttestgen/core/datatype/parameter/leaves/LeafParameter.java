package io.resttestgen.core.datatype.parameter.leaves;

import io.resttestgen.core.datatype.parameter.Parameter;
import io.resttestgen.core.datatype.parameter.attributes.ParameterLocation;
import io.resttestgen.core.datatype.parameter.attributes.ParameterStyle;
import io.resttestgen.core.datatype.parameter.structured.ArrayParameter;
import io.resttestgen.core.datatype.parameter.visitor.Visitor;
import io.resttestgen.core.helper.ObjectHelper;
import io.resttestgen.core.openapi.EditReadOnlyOperationException;
import io.resttestgen.core.openapi.OpenApiParser;
import io.resttestgen.core.testing.parametervalueprovider.ParameterValueProvider;
import io.resttestgen.core.testing.parametervalueprovider.ValueNotAvailableException;
import kotlin.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class LeafParameter extends Parameter {
    private static final Logger logger = LogManager.getLogger(LeafParameter.class);

    protected Object value;
    protected ParameterValueProvider valueSource = null;
    protected boolean resourceIdentifier = false;
    protected boolean inferredResourceIdentifier = false;

    public LeafParameter(Map<String, Object> parameterMap, String name) {
        super(parameterMap, name);
        if (parameterMap.containsKey("x-crudResourceIdentifier")) {
            this.resourceIdentifier = OpenApiParser.safeGet(parameterMap, "x-crudResourceIdentifier", Boolean.class);
        }
    }

    protected LeafParameter(LeafParameter other) {
        super(other);
        value = ObjectHelper.deepCloneObject(other.value);
        valueSource = other.valueSource;
        resourceIdentifier = other.resourceIdentifier;
        inferredResourceIdentifier = other.inferredResourceIdentifier;
    }

    protected LeafParameter(LeafParameter other, Parameter parent) {
        super(other);
        setParent(parent);
        value = ObjectHelper.deepCloneObject(other.value);
        valueSource = other.valueSource;
        resourceIdentifier = other.resourceIdentifier;
        inferredResourceIdentifier = other.inferredResourceIdentifier;
    }

    protected LeafParameter(Parameter other) {
        super(other);
    }

    public LeafParameter() {
        super();
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }

    public abstract boolean isValueCompliant(Object value);

    private boolean inferResourceIdentifier() {
        return normalizedName.toString().toLowerCase().endsWith("id") ||
                normalizedName.toString().toLowerCase().endsWith("usernam") ||
                normalizedName.toString().toLowerCase().endsWith("username");
    }

    @Override
    public Collection<Parameter> getChildren() {
        return Set.of();
    }

    @Override
    public boolean addChild(Parameter parameter) {
        if (getOperation() != null && getOperation().isReadOnly()) {
            throw new EditReadOnlyOperationException(getOperation());
        }

        return false;
    }

    @Override
    public boolean removeChild(Parameter parameter) {
        if (getOperation() != null && getOperation().isReadOnly()) {
            throw new EditReadOnlyOperationException(getOperation());
        }

        return false;
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
                logger.warn("{}: Style not consistent with parameter type. Returning '{}' style.", getName(), parameterStyle);
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
        if (value instanceof LeafParameter) {
            return ((LeafParameter) value).getConcreteValue();
        }
        return value;
    }

    public void setValueManually(Object value) {
        if (getOperation() != null && getOperation().isReadOnly()) {
            throw new EditReadOnlyOperationException(getOperation());
        }
        if (this.isObjectTypeCompliant(value)) {
            this.value = value;
            this.valueSource = null;
        } else {
            logger.warn("Setting value '{}' to parameter '{}' is not possible due to type mismatch. Consider changing the type of this parameter before setting this value again.", value, this.getName());
        }
    }

    public void setValueWithProvider(ParameterValueProvider provider) {
        if (getOperation() != null && getOperation().isReadOnly()) {
            throw new EditReadOnlyOperationException(getOperation());
        }
        try {
            Pair<ParameterValueProvider, Object> providerResult = provider.provideValueFor(this);
            if (this.isObjectTypeCompliant(providerResult.getSecond())) {
                this.value = providerResult.getSecond();
                this.valueSource = providerResult.getFirst();
            } else {
                logger.warn("Setting value '{}' to parameter '{}' is not possible due to type mismatch. Consider changing the type of this parameter before setting this value again.", value, this.getName());
            }
        } catch (ValueNotAvailableException e) {
            logger.warn(e.getMessage());
        }

    }

    public void removeValue() {
        if (getOperation() != null && getOperation().isReadOnly()) {
            throw new EditReadOnlyOperationException(getOperation());
        }

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
        } else if (getParent() instanceof ArrayParameter) {

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
        return value != null;
    }

    public ParameterValueProvider getValueSource() {
        return valueSource;
    }

    @Override
    public Set<Parameter> getAllParameters() {
        HashSet<Parameter> parameters = new HashSet<>();
        parameters.add(this);
        return parameters;
    }

    @Override
    public boolean remove() {
        if (getOperation() != null && getOperation().isReadOnly()) {
            throw new EditReadOnlyOperationException(getOperation());
        }

        // If the leaf has no parent (it is a root), then remove it from the operation
        if (getParent() == null && getOperation() != null) {
            getOperation().removeParameter(getLocation(), this);
        }

        // If the leaf is contained in a parent element (array or object), remove it from the parent
        else {
            getParent().removeChild(this);
        }

        return false;
    }

    /**
     * Return a parameter element according to its JSON path.
     * @param jsonPath the JSON path of the parameter we want to get.
     * @return the parameter matching the JSON path.
     */
    @Override
    public Parameter getParameterFromJsonPath(String jsonPath) {

        // If the JSON path starts with $, then start the search from the root element
        Parameter rootElement = getRoot();
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
