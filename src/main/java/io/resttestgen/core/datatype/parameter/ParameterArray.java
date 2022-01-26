package io.resttestgen.core.datatype.parameter;

import io.resttestgen.core.datatype.NormalizedParameterName;
import io.resttestgen.core.openapi.EditReadOnlyOperationException;
import io.resttestgen.core.openapi.Operation;
import io.resttestgen.core.openapi.UnsupportedSpecificationFeature;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.*;

public class ParameterArray extends StructuredParameterElement {

    private ParameterElement referenceElement;
    private List<ParameterElement> elements;

    private static final Logger logger = LogManager.getLogger(ParameterArray.class);

    public ParameterArray(ParameterElement parent, Map<String, Object> schema, Operation operation, String name) {
        super(parent, schema, operation, name);
        elements = new LinkedList<>();

        setNormalizedName(NormalizedParameterName.computeParameterNormalizedName(this));

        // Catch the difference between leaf and structured parameters
        Map<String, Object> targetSource = schema.containsKey("items") ?
                schema :
                (Map<String, Object>) schema.get("schema");

        Map<String, Object> items = (Map<String, Object>) targetSource.get("items");
        try {
            items.put("in", getLocation().toString());
            referenceElement = ParameterFactory.getParameterElement(this, items, operation, getNormalizedName().toString());

            // Propagate example values to children
            for (Object example : super.examples) {
                List<Object> exampleItems = (List<Object>) example;
                for (Object item : exampleItems) {
                    referenceElement.addExample(item);
                }
            }

        } catch (UnsupportedSpecificationFeature e) {
            throw new ParameterCreationException("Unable to parse  reference element for property \"" + name +
                    "\" (normalized as: \"" + name + "\") " +
                    "due to an unsupported feature in OpenAPI specification.");

        }

    }

    private ParameterArray(ParameterArray other) {
        super(other);

        referenceElement = other.referenceElement.deepClone();
        elements = new LinkedList<>();
        other.elements.forEach(e -> elements.add(e.deepClone()));
    }

    private ParameterArray(ParameterArray other, Operation operation, ParameterElement parent) {
        super(other, operation, parent);

        referenceElement = other.referenceElement.deepClone(operation, this);
        elements = new LinkedList<>();
        other.elements.forEach(e -> elements.add(e.deepClone(operation, this)));
    }

    public ParameterArray(ParameterElement other) {
        super(other);

        referenceElement = null;
        elements = new LinkedList<>();
    }

    public ParameterArray merge(ParameterElement other) {
        if (!(other instanceof ParameterArray)) {
            throw new IllegalArgumentException("Cannot merge a " + this.getClass() + " instance with a "
                    + other.getClass() + " one.");
        }

        ParameterArray stringParameter = (ParameterArray) other;
        ParameterArray merged = new ParameterArray(this);
        merged.referenceElement = this.referenceElement.merge(stringParameter.referenceElement);

        return merged;
    }

    @Override
    public boolean isObjectTypeCompliant(Object o) {
        if (o == null) {
            return false;
        }
        return List.class.isAssignableFrom(o.getClass());
    }

    public ParameterElement getReferenceElement() {
        return this.referenceElement;
    }

    @Override
    public Collection<ParameterLeaf> getLeaves() {
        Collection<ParameterLeaf> leaves = new LinkedList<>();

        for (ParameterElement element : elements) {
            leaves.addAll(element.getLeaves());
        }

        return leaves;
    }

    /**
     * A ParameterArray is considered empty when it has no elements.
     * @return True if the instance has no elements; false otherwise
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
        if (getOperation().isReadOnly()) {
            throw new EditReadOnlyOperationException(getOperation());
        }

        List<ParameterElement> newElements = new LinkedList<>(this.elements);
        List<ParameterElement> elementsToRemove = new LinkedList<>();

        newElements.forEach(e -> {
            if (ParameterLeaf.class.isAssignableFrom(e.getClass())) {
                if (e.getValue() == null) {
                    logger.warn("Empty valued parameter '" + e.getName() + "' found. It will be removed.");
                    elementsToRemove.add(e);
                }
            } else if (StructuredParameterElement.class.isAssignableFrom(e.getClass())) {
                StructuredParameterElement structuredE = (StructuredParameterElement) e;
                structuredE.removeUninitializedParameters();

                if (structuredE.isEmpty() && !structuredE.isKeepIfEmpty()) {
                    logger.warn("Empty valued parameter '" + e.getName() + "' found. It will be removed.");
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

    public List<ParameterElement> getElements() {
        if (getOperation().isReadOnly()) {
            return Collections.unmodifiableList(elements);
        }
        return elements;
    }

    /**
     * Adds the given element to the instance element list. Can throw a EditReadOnlyOperationException if the
     * instance is in read-only mode.
     * @param element The ParameterElement to be added to the elements list
     */
    public void addElement(ParameterElement element) {
        if (getOperation().isReadOnly()) {
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
        if (getOperation().isReadOnly()) {
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
        if (getOperation().isReadOnly()) {
            throw new EditReadOnlyOperationException(getOperation());
        }
        this.elements.clear();
    }

    @Override
    public boolean addExample(Object o) {
        if (getOperation().isReadOnly()) {
            throw new EditReadOnlyOperationException(getOperation());
        }

        if (!super.addExample(o)) {
            return false;
        }

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

        for (ParameterElement element : elements) {
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
        ParameterArray that = (ParameterArray) o;
        return Objects.equals(referenceElement, that.referenceElement);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), referenceElement);
    }

    @Override
    public ParameterArray deepClone() {
        return new ParameterArray(this);
    }

    @Override
    public ParameterArray deepClone(Operation operation, ParameterElement parent) {
        return new ParameterArray(this, operation, parent);
    }

    @Override
    public String toString() {
        return getName() + ": [" + referenceElement.toString() + "]";
    }

    /**
     * Returns itself, plus the arrays contained in its elements.
     * @return an empty list
     */
    @Override
    public Collection<ParameterArray> getArrays() {
        Collection<ParameterArray> arrays = new LinkedList<>();

        // Add this array
        arrays.add(this);

        // For each parameter contained in the array, add their arrays.
        for (ParameterElement element : elements) {
            arrays.addAll(element.getArrays());
        }

        return arrays;
    }

    @Override
    public Collection<CombinedSchemaParameter> getCombinedSchemas() {
        Collection<CombinedSchemaParameter> combinedSchemas = new LinkedList<>();

        for (ParameterElement element : elements) {
            combinedSchemas.addAll(element.getCombinedSchemas());
        }

        return combinedSchemas;
    }
}
