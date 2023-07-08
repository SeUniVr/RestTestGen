package io.resttestgen.core.datatype.parameter.structured;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import io.resttestgen.core.datatype.ParameterName;
import io.resttestgen.core.datatype.parameter.*;
import io.resttestgen.core.datatype.parameter.attributes.ParameterLocation;
import io.resttestgen.core.datatype.parameter.attributes.ParameterStyle;
import io.resttestgen.core.datatype.parameter.attributes.ParameterType;
import io.resttestgen.core.datatype.parameter.exceptions.ParameterCreationException;
import io.resttestgen.core.datatype.parameter.leaves.LeafParameter;
import io.resttestgen.core.datatype.parameter.visitor.Visitor;
import io.resttestgen.core.openapi.EditReadOnlyOperationException;
import io.resttestgen.core.openapi.OpenApiParser;
import io.resttestgen.core.openapi.Operation;
import io.resttestgen.core.openapi.UnsupportedSpecificationFeature;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

import static io.resttestgen.core.datatype.parameter.ParameterUtils.isArrayOfLeaves;

public class ArrayParameter extends StructuredParameter {

    private Parameter referenceElement;
    private List<Parameter> elements = new LinkedList<>();

    private Integer minItems;
    private Integer maxItems;
    private boolean uniqueItems;

    private static final Logger logger = LogManager.getLogger(ArrayParameter.class);

    @SuppressWarnings("unchecked")
    public ArrayParameter(Map<String, Object> schema, String name) {
        super(schema, name);

        minItems = schema.containsKey("minItems") ?
                OpenApiParser.safeGet(schema, "minItems", Double.class).intValue() :
                null;
        maxItems = schema.containsKey("maxItems") ?
                OpenApiParser.safeGet(schema, "maxItems", Double.class).intValue() :
                null;
        uniqueItems = schema.containsKey("uniqueItems") ?
                OpenApiParser.safeGet(schema, "uniqueItems", Boolean.class) :
                false;

        // Catch the difference between leaf and structured parameters
        Map<String, Object> targetSource = schema.containsKey("items") ?
                schema :
                (Map<String, Object>) schema.get("schema");

        if (targetSource != null) {
            Map<String, Object> items = (Map<String, Object>) targetSource.get("items");
            try {
                items.put("in", getLocation().toString());
                setReferenceElement(ParameterFactory.getParameter(items, this.name.toString()));

                // Propagate example values to children
                for (Object example : super.examples) {
                    List<Object> exampleItems = (List<Object>) example;
                    for (Object item : exampleItems) {
                        referenceElement.addExample(item);
                    }
                }

            } catch (UnsupportedSpecificationFeature e) {
                throw new ParameterCreationException("Unable to parse reference element for property \"" + name +
                        "\" (normalized as: \"" + name + "\") " +
                        "due to an unsupported feature in OpenAPI specification.");

            }
        }
    }

    private ArrayParameter(ArrayParameter other) {
        super(other);

        setReferenceElement(other.referenceElement == null ? null : other.referenceElement.deepClone());

        for (Parameter otherElement: other.getElements()) {
            addElement(otherElement.deepClone());
        }
        minItems = other.minItems;
        maxItems = other.maxItems;
        uniqueItems = other.uniqueItems;
    }

    public ArrayParameter(Parameter other) {
        super(other);
        this.type = ParameterType.ARRAY;
    }

    public ArrayParameter() {
        super();
        this.type = ParameterType.ARRAY;
    }

    @Override
    public Collection<Parameter> getChildren() {
        if (referenceElement == null) {
            return Set.of();
        }

        return Set.of(referenceElement);
    }

    @Override
    public boolean addChild(Parameter parameter) {
        if (getOperation() != null && getOperation().isReadOnly()) {
            throw new EditReadOnlyOperationException(getOperation());
        }

        setReferenceElement(parameter);
        return true;
    }

    @Override
    public boolean removeChild(Parameter parameter) {
        if (getOperation() != null && getOperation().isReadOnly()) {
            throw new EditReadOnlyOperationException(getOperation());
        }

        if (referenceElement != null && referenceElement.equals(parameter)) {
            setReferenceElement(null);
            return true;
        }

        return false;
    }

    @Override
    public void setLocation(ParameterLocation location) {
        super.setLocation(location);
        elements.forEach(e -> e.setLocation(location));
    }

    public ArrayParameter(JsonArray jsonArray, String name) {
        super();
        this.name = new ParameterName(Objects.requireNonNullElse(name, ""));
        this.type = ParameterType.ARRAY;

        for (JsonElement jsonElement : jsonArray) {
            Parameter p =
                    ParameterFactory.getParameter(jsonElement, this.name.toString());
            if (p != null) {
                addElement(p);
            }
        }
    }

    @Override
    public void setOperation(Operation operation) {
        super.setOperation(operation);
        elements.forEach(e -> e.setOperation(operation));
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }

    public ArrayParameter merge(Parameter other) {
        if (!(other instanceof ArrayParameter)) {
            throw new IllegalArgumentException("Cannot merge a " + this.getClass() + " instance with a "
                    + other.getClass() + " one.");
        }

        ArrayParameter stringParameter = (ArrayParameter) other;
        ArrayParameter merged = new ArrayParameter(this);
        merged.setReferenceElement(referenceElement.merge(stringParameter.referenceElement));

        return merged;
    }

    public Integer getMinItems() {
        return minItems;
    }

    public void setMinItems(Integer minItems) {
        if (getOperation() != null && getOperation().isReadOnly()) {
            throw new EditReadOnlyOperationException(getOperation());
        }

        this.minItems = minItems;
    }

    public Integer getMaxItems() {
        return maxItems;
    }

    public void setMaxItems(Integer maxItems) {
        if (getOperation() != null && getOperation().isReadOnly()) {
            throw new EditReadOnlyOperationException(getOperation());
        }

        this.maxItems = maxItems;
    }

    public boolean isUniqueItems() {
        return uniqueItems;
    }

    public void setUniqueItems(boolean uniqueItems) {
        if (getOperation() != null && getOperation().isReadOnly()) {
            throw new EditReadOnlyOperationException(getOperation());
        }

        this.uniqueItems = uniqueItems;
    }

    public int indexOf(Parameter element) {
        return elements.indexOf(element);
    }

    @Override
    public boolean isObjectTypeCompliant(Object o) {
        if (o == null) {
            return false;
        }
        return List.class.isAssignableFrom(o.getClass());
    }

    public Parameter getReferenceElement() {
        return this.referenceElement;
    }

    public void setReferenceElement(Parameter referenceElement) {
        if (getOperation() != null && getOperation().isReadOnly()) {
            throw new EditReadOnlyOperationException(getOperation());
        }

        if (referenceElement != null) {
            referenceElement.setParent(this);
        }

        this.referenceElement = referenceElement;
    }

    /**
     * A ParameterArray is considered empty when it has no elements.
     * @return true if the instance has no elements; false otherwise.
     */
    @Override
    public boolean isEmpty() {
        return this.elements.isEmpty();
    }

    /**
     * This function only operates on elements in the instance element list. The reference item is only used as a
     * template, so it won't be removed even if empty-valued.
     */
    @Override
    public void removeUninitializedParameters() {
        if (getOperation() != null && getOperation().isReadOnly()) {
            throw new EditReadOnlyOperationException(getOperation());
        }

        List<Parameter> newElements = new LinkedList<>(this.elements);
        List<Parameter> elementsToRemove = new LinkedList<>();

        newElements.forEach(e -> {
            if (LeafParameter.class.isAssignableFrom(e.getClass())) {
                if (e.getValue() == null) {
                    //logger.warn("Empty valued parameter '" + e.getName() + "' found. It will be removed.");
                    elementsToRemove.add(e);
                }
            } else if (StructuredParameter.class.isAssignableFrom(e.getClass())) {
                StructuredParameter structuredE = (StructuredParameter) e;
                structuredE.removeUninitializedParameters();

                if (structuredE.isEmpty() && !structuredE.isKeepIfEmpty()) {
                    //logger.warn("Empty valued parameter '" + e.getName() + "' found. It will be removed.");
                    elementsToRemove.add(e);
                }
            }
        });

        newElements.removeAll(elementsToRemove);

        this.elements = newElements;
    }

    @Override
    public String getJSONString() {
        StringBuilder stringBuilder = new StringBuilder(getJSONHeading() + "[");
        elements.forEach(e -> stringBuilder.append(e.getJSONString()).append(", "));
        int index = stringBuilder.lastIndexOf(",");
        return stringBuilder.substring(0, index > 0 ? index : stringBuilder.length()) + "]";
    }

    public List<Parameter> getElements() {
        return Collections.unmodifiableList(elements);
    }

    public boolean removeElement(Parameter element) {
        if (getOperation() != null && getOperation().isReadOnly()) {
            throw new EditReadOnlyOperationException(getOperation());
        }

        return elements.remove(element);
    }

    /**
     * Adds the given element to the instance element list. Can throw a EditReadOnlyOperationException if the
     * instance is in read-only mode.
     * @param element The Parameter to be added to the elements list
     */
    public void addElement(Parameter element) {
        if (getOperation() != null && getOperation().isReadOnly()) {
            throw new EditReadOnlyOperationException(getOperation());
        }
        element.setParent(this);
        elements.add(element);
    }

    /**
     * Adds n copies of the reference element to the array. Can throw a EditReadOnlyOperationException if the instance
     * is in read-only mode.
     * @param n Number of copies to be put into the array
     */
    public void addReferenceElements(int n) {
        if (getOperation() != null && getOperation().isReadOnly()) {
            throw new EditReadOnlyOperationException(getOperation());
        }
        for (int i = 0; i < n; i++) {
            addElement(referenceElement.deepClone());
        }
    }

    /**
     * Removes all the elements in the elements list of the instance
     */
    public void clearElements() {
        if (getOperation() != null && getOperation().isReadOnly()) {
            throw new EditReadOnlyOperationException(getOperation());
        }
        this.elements.clear();
    }

    @Override
    public boolean addExample(Object o) {
        if (getOperation() != null && getOperation().isReadOnly()) {
            throw new EditReadOnlyOperationException(getOperation());
        }

        if (!super.addExample(o)) {
            return false;
        }

        @SuppressWarnings("unchecked")
        List<Object> exampleItems = (List<Object>) o;
        // Propagate example values to children
        exampleItems.forEach(item -> referenceElement.addExample(item));

        return true;
    }

    public String getValueAsFormattedString (ParameterStyle style, boolean explode) {
        logger.warn("Format for deep nested arrays is not defined in the reference RFC. Use this method only for " +
                "RFC defined behaviors.");
        StringBuilder stringBuilder = new StringBuilder();
        switch (style) {
            case MATRIX:
                stringBuilder.append(";");
                if (explode) {
                    elements.forEach(e -> {
                        stringBuilder.append(getName().toString());
                        stringBuilder.append("=");
                        stringBuilder.append(e.getValueAsFormattedString(ParameterStyle.SIMPLE));
                        stringBuilder.append(";");
                    });
                    if (stringBuilder.charAt(stringBuilder.length() - 1) == ';') {
                        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
                    }
                } else {
                    stringBuilder.append(this.getName().toString());
                    stringBuilder.append("=");
                    elements.forEach(e -> {
                        stringBuilder.append(e.getValueAsFormattedString(ParameterStyle.SIMPLE));
                        stringBuilder.append(",");
                    });
                    if (stringBuilder.charAt(stringBuilder.length() - 1) == ',') {
                        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
                    }
                }
                return stringBuilder.toString();

            case LABEL:
                elements.forEach(e -> {
                    stringBuilder.append(".");
                    stringBuilder.append(e.getValueAsFormattedString(ParameterStyle.SIMPLE));
                });
                return stringBuilder.toString();

            case FORM:
                if (explode) {
                    elements.forEach(e -> {
                        stringBuilder.append(this.getName().toString());
                        stringBuilder.append("=");
                        stringBuilder.append(e.getValueAsFormattedString(ParameterStyle.SIMPLE));
                        stringBuilder.append("&");
                    });
                    if (stringBuilder.charAt(stringBuilder.length() - 1) == '&') {
                        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
                    }
                } else {
                    stringBuilder.append(this.getName().toString());
                    stringBuilder.append("=");
                    elements.forEach(e -> {
                        stringBuilder.append(e.getValueAsFormattedString(ParameterStyle.SIMPLE));
                        stringBuilder.append(",");
                    });
                    if (stringBuilder.charAt(stringBuilder.length() - 1) == ',') {
                        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
                    }
                }
                return stringBuilder.toString();

            case SIMPLE:
                elements.forEach(e -> {
                    stringBuilder.append(e.getValueAsFormattedString(ParameterStyle.SIMPLE));
                    stringBuilder.append(",");
                });
                if (stringBuilder.charAt(stringBuilder.length() - 1) == ',') {
                    stringBuilder.deleteCharAt(stringBuilder.length() - 1);
                }
                return stringBuilder.toString();

            case SPACE_DELIMITED:
                elements.forEach(e -> {
                    stringBuilder.append(e.getValueAsFormattedString(ParameterStyle.SIMPLE));
                    stringBuilder.append("%20");
                });
                if (stringBuilder.length() > 0) {
                    return stringBuilder.substring(0, stringBuilder.length() - 3);
                }
                return stringBuilder.toString();

            case PIPE_DELIMITED:
                elements.forEach(e -> {
                    stringBuilder.append(e.getValueAsFormattedString(ParameterStyle.SIMPLE));
                    stringBuilder.append("|");
                });
                if (stringBuilder.charAt(stringBuilder.length() - 1) == '|') {
                    stringBuilder.deleteCharAt(stringBuilder.length() - 1);
                }
                return stringBuilder.toString();

            case DEEP_OBJECT:
            default:
                logger.warn(getName() + ": Style not consistent with parameter type. Returning 'simple' style.");
                return this.getValueAsFormattedString(ParameterStyle.SIMPLE);
        }
    }

    public boolean hasValue() {
        boolean hasValue = true;

        for (Parameter element : elements) {
            hasValue = hasValue && element.hasValue();
        }

        if (!hasValue) {
            logger.warn("Parameter " + getName() + " has an invalid value.");
        }

        return hasValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) return false;
        ArrayParameter that = (ArrayParameter) o;
        return Objects.equals(referenceElement, that.referenceElement);
    }

    @Override
    public ArrayParameter deepClone() {
        return new ArrayParameter(this);
    }

    @Override
    public String toString() {
        if (elements != null && elements.size() > 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(getName()).append(": [");
            elements.forEach(e -> stringBuilder.append(e.toString()).append(", "));
            stringBuilder.append("]");
            return stringBuilder.toString();
        } else if (referenceElement != null) {
            return getName() + ": [" + referenceElement.toString() + "]";
        } else {
            return getName() + ": []";
        }
    }

    @Override
    public Collection<Parameter> getAllParameters() {
        HashSet<Parameter> parameters = new HashSet<>();
        parameters.add(this);
        if (referenceElement != null) {
            parameters.addAll(referenceElement.getAllParameters());
        }
        elements.forEach(e -> parameters.addAll(e.getAllParameters()));
        return parameters;
    }

    @Override
    public boolean isSet() {
        return elements.size() > 0;
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
            int elementIndex = Integer.parseInt(jsonPath.substring(start + 1, end));
            // Index -1 stands for reference element
            if (elementIndex == -1) {
                return referenceElement.getParameterFromJsonPath(jsonPath.substring(end + 1));
            } else if (elementIndex >= 0 && elementIndex < elements.size()) {
                return elements.get(elementIndex).getParameterFromJsonPath(jsonPath.substring(end + 1));
            } else {
                // Index is invalid
                return null;
            }
        } else {
            // Missing parenthesis
            return null;
        }
    }

    /**
     * Check if the values of the elements in the array correspond to those specified in the provided string.
     * FIXME: deal with decimal numbers in some way.
     * @param commaSeparatedValues the values in the array, comma-separated.
     * @return true if the array has the provided values.
     */
    public boolean hasValues(String commaSeparatedValues) {

        // Only applicable to array of leaves
        if (isArrayOfLeaves(this)) {
            boolean hasValues = true;

            List<String> stringValues = elements.stream()
                    .map(e -> ((LeafParameter) e).getConcreteValue().toString()).collect(Collectors.toList());

            // Remove square brackets, if present
            if (commaSeparatedValues.length() > 2 && commaSeparatedValues.startsWith("[") && commaSeparatedValues.endsWith("]")) {
                commaSeparatedValues = commaSeparatedValues.substring(1, commaSeparatedValues.length() - 1);
            }

            String[] values = commaSeparatedValues.split(",");
            if (values.length == elements.size()) {
                for (String value : values) {

                    // Cut out quotes from values
                    if (value.length() > 2 && (value.startsWith("'") && value.endsWith("'")) ||
                            (value.startsWith("\"") && value.endsWith("\""))) {
                        value = value.substring(1, value.length() - 1);
                    }

                    hasValues = hasValues && stringValues.remove(value);
                }
                return hasValues;
            }
        }
        return false;
    }

    public void setValuesFromCommaSeparatedString(String commaSeparatedValues) {
        // Only applicable to array of leaves
        if (isArrayOfLeaves(this)) {

            // Remove square brackets, if present
            if (commaSeparatedValues.length() > 2 && commaSeparatedValues.startsWith("[") && commaSeparatedValues.endsWith("]")) {
                commaSeparatedValues = commaSeparatedValues.substring(1, commaSeparatedValues.length() - 1);
            }

            String[] values = commaSeparatedValues.split(",");

            for (String value : values) {

                // Cut out quotes from values
                if (value.length() > 2 && (value.startsWith("'") && value.endsWith("'")) ||
                        (value.startsWith("\"") && value.endsWith("\""))) {
                    value = value.substring(1, value.length() - 1);
                }

                // Create new element for the array from reference element
                LeafParameter newLeaf = (LeafParameter) this.getReferenceElement().deepClone();
                newLeaf.setValue(value);
                this.addElement(newLeaf);
            }
        }
    }

    public int size() {
        return elements.size();
    }
}
