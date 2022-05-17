package io.resttestgen.core.datatype.parameter;

import io.resttestgen.core.dictionary.Dictionary;
import io.resttestgen.core.helper.ObjectHelper;
import io.resttestgen.core.openapi.EditReadOnlyOperationException;
import io.resttestgen.core.openapi.Operation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public abstract class ParameterLeaf extends ParameterElement {

    private static final Logger logger = LogManager.getLogger(ParameterLeaf.class);

    protected Object value;

    public ParameterLeaf(ParameterElement parent, Map<String, Object> parameterMap, Operation operation, String name) {
        super(parent, parameterMap, operation, name);
    }

    public ParameterLeaf(ParameterElement parent, Map<String, Object> parameterMap, Operation operation) {
        this(parent, parameterMap, operation,null);
    }

    protected ParameterLeaf(ParameterLeaf other) {
        super(other);
        value = ObjectHelper.deepCloneObject(other.value);
    }

    protected ParameterLeaf(ParameterLeaf other, Operation operation, ParameterElement parent) {
        super(other, operation, parent);
        value = ObjectHelper.deepCloneObject(other.value);
    }

    protected ParameterLeaf(ParameterElement other) {
        super(other);
    }

    public ParameterLeaf(Operation operation, ParameterElement parent) {
        super(operation, parent);
    }

    public String getValueAsFormattedString(ParameterStyle style, boolean explode) {
        if (value == null) {
            logger.warn("Called 'getValueAsFormattedString' function on null-valued parameter.");
            return "";
        }

        Object renderedValue = value;

        if (renderedValue instanceof ParameterLeaf) {
            renderedValue = ((ParameterLeaf) renderedValue).getValue();
        }

        String encodedValue = URLEncoder.encode(renderedValue.toString(), StandardCharsets.UTF_8);
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

    public int countValuesInNormalizedDictionary(Dictionary dictionary) {
        return dictionary.getEntriesByNormalizedParameterName(normalizedName, type).size();
    }

    public int countValuesInDictionary(Dictionary dictionary) {
        return dictionary.getEntriesByParameterName(name, type).size();
    }

    public abstract Object generateCompliantValue();

    /**
     * Replaces self with another leaf.
     * @param newLeaf the new leaf.
     */
    public boolean replace(ParameterLeaf newLeaf) {
        ParameterElement parent = getParent();
        if (parent != null) {
            if (parent instanceof ParameterArray) {
                List<ParameterElement> elements = ((ParameterArray) parent).getElements();
                int index = elements.indexOf(this);
                elements.set(index, newLeaf);
                return true;
            } else if (parent instanceof ParameterObject) {
                List<ParameterElement> properties = ((ParameterObject) parent).getProperties();
                int index = properties.indexOf(this);
                properties.set(index, newLeaf);
                return true;
            }
        } else {
            if (getLocation() == ParameterLocation.HEADER) {
                if (getOperation().getHeaderParameters().contains(this)) {
                    getOperation().getHeaderParameters().remove(this);
                    getOperation().getHeaderParameters().add(newLeaf);
                    return true;
                }
            } else if (getLocation() == ParameterLocation.PATH) {
                if (getOperation().getPathParameters().contains(this)) {
                    getOperation().getPathParameters().remove(this);
                    getOperation().getPathParameters().add(newLeaf);
                    return true;
                }
            } else if (getLocation() == ParameterLocation.QUERY) {
                if (getOperation().getQueryParameters().contains(this)) {
                    getOperation().getQueryParameters().remove(this);
                    getOperation().getQueryParameters().add(newLeaf);
                    return true;
                }
            } else if (getLocation() == ParameterLocation.COOKIE) {
                if (getOperation().getPathParameters().contains(this)) {
                    getOperation().getPathParameters().remove(this);
                    getOperation().getPathParameters().add(newLeaf);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean hasValue() {
        if (value == null) {
            logger.warn("Parameter " + getName() + " has an invalid value.");
        }
        return value != null;
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
}
