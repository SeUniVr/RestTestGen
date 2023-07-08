package io.resttestgen.core.datatype.parameter.combined;

import io.resttestgen.core.datatype.parameter.*;
import io.resttestgen.core.datatype.parameter.attributes.ParameterStyle;
import io.resttestgen.core.datatype.parameter.exceptions.ParameterCreationException;
import io.resttestgen.core.datatype.parameter.leaves.NullParameter;
import io.resttestgen.core.datatype.parameter.visitor.Visitor;
import io.resttestgen.core.openapi.EditReadOnlyOperationException;
import io.resttestgen.core.openapi.OpenApiParser;
import io.resttestgen.core.openapi.Operation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

public abstract class CombinedSchemaParameter extends Parameter {

    protected final List<Parameter> parametersSchemas;
    protected Parameter outputParameterSchema;
    protected final List<Parameter> properties;

    private static final Logger logger = LogManager.getLogger(CombinedSchemaParameter.class);

    public CombinedSchemaParameter(Map<String, Object> parameterMap, String name) {
        super(parameterMap, name);

        this.parametersSchemas = new LinkedList<>();
        this.properties = new LinkedList<>();

        setupFields(parameterMap);
    }

    protected CombinedSchemaParameter(Parameter other) {
        super(other);

        this.parametersSchemas = new LinkedList<>();
        this.properties = new LinkedList<>();
    }

    protected CombinedSchemaParameter(CombinedSchemaParameter other) {
        super(other);

        this.parametersSchemas = new LinkedList<>();
        other.parametersSchemas.forEach(ps -> addToParameterSchema(ps.deepClone()));
        if (other.outputParameterSchema != null ) {
            this.outputParameterSchema = other.outputParameterSchema.deepClone();
        }
        this.properties = new LinkedList<>();
        other.properties.forEach(p -> this.properties.add(p.deepClone()));
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

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }

    protected abstract String getKeyFiledName();

    private void setupFields(Map<String, Object> parameterMap) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> schemas = OpenApiParser.safeGet(parameterMap, getKeyFiledName(), ArrayList.class);
        schemas.forEach(p -> {
            // Propagate location value to defined schemas
            p.put("in", getLocation().toString());
            try {
                Parameter parameter = ParameterFactory.getParameter(p, "");
                addToParameterSchema(parameter);
            } catch (ParameterCreationException e) {
                logger.warn("Discarding schema in '" + getKeyFiledName() + "' field of '" + getName() + "'.");
            }
        });

        if (this.parametersSchemas.size() == 0) {
            throw new ParameterCreationException("Combined schema with no valid schema listed will be discarded (" +
                    getName() + ").");
        }
    }

    public void addToParameterSchema(Parameter element) {
        if (getOperation() != null && getOperation().isReadOnly()) {
            throw new EditReadOnlyOperationException(getOperation());
        }

        element.setParent(this);
        this.parametersSchemas.add(element);
    }

    public List<Parameter> getParametersSchemas() {
        if (getOperation() != null && getOperation().isReadOnly()) {
            return Collections.unmodifiableList(parametersSchemas);
        }
        return parametersSchemas;
    }

    public List<Parameter> getProperties() {
        if (getOperation() != null && getOperation().isReadOnly()) {
            return Collections.unmodifiableList(properties);
        }
        return properties;
    }

    public Parameter getOutputParameterSchema() {
        return outputParameterSchema;
    }

    @Override
    public void setOperation(Operation operation) {
        super.setOperation(operation);
        properties.forEach(p -> p.setOperation(operation));
        parametersSchemas.forEach(p -> p.setOperation(operation));
        if (outputParameterSchema != null) {
            outputParameterSchema.setOperation(operation);
        }
    }

    public void addProperty(Parameter property) {
        if (getOperation() != null && getOperation().isReadOnly()) {
            throw new EditReadOnlyOperationException(getOperation());
        }

        this.properties.add(property);
    }

    // TODO: implement. When a new property is added we should add it to the actually selected parameter
    public Parameter updateSelectedParameter() {
        return null;
    }

    @Override
    public String getJSONString() {
        if (outputParameterSchema != null) {
            return outputParameterSchema.getJSONString();
        }
        logger.error("Requested JSON string for parameter '" + getName() + "(operation '" + getOperation() + "')," +
                " but no schema was selected. Returning an empty string.");
        return "";
    }

    public String getJsonPath() {
        // FIXME: implement when supporting combined parameters
        return "";
    }

    @Override
    public String getValueAsFormattedString(ParameterStyle style, boolean explode) {
        if (outputParameterSchema != null) {
            return outputParameterSchema.getValueAsFormattedString(style, explode);
        }
        logger.error("Requested formatted string for parameter '" + getName() + "(operation '" + getOperation() + "')," +
                " but no schema was selected. Returning an empty string.");
        return "";
    }

    @Override
    public boolean hasValue() {
        return outputParameterSchema != null && outputParameterSchema.hasValue();
    }

    @Override
    public boolean isSet() {
        return false;
    }

    @Override
    public boolean remove() {
        return false;
    }

    @Override
    public CombinedSchemaParameter merge(Parameter other) {
        // TODO: implement
        return null;
    }

    public abstract Parameter merge();

    public Parameter merge(List<Integer> schemaIndexes) {
        // remove all indexes out of bound
        schemaIndexes = schemaIndexes.stream().filter(i -> i < this.parametersSchemas.size())
                .collect(Collectors.toList());

        Parameter merged = this.parametersSchemas.get(schemaIndexes.get(0));

        for (Integer index : schemaIndexes) {
            merged = merged.merge(this.parametersSchemas.get(index));
        }

        return merged;
    }

    public void merge(int numberOfSchemas) {
        if (numberOfSchemas <= 0) {
            logger.warn("Tried to merge a non-positive number of schemas. The number of schemas to be" +
                    " merged MUST be greater than zero. Setting selected parameter to null.");
            this.outputParameterSchema = new NullParameter(this);
        } else if (numberOfSchemas >= this.parametersSchemas.size()) {
            logger.warn("Tried to merge more schemas than the declared ones. All the declared schemas will be " +
                    "merged.");
            numberOfSchemas = this.parametersSchemas.size();
        }

        List<Integer> schemaIndexes = new LinkedList<>();
        for (int i = 0; i < this.parametersSchemas.size(); ++i) {
            schemaIndexes.add(i);
        }

        List<Integer> indexes = new LinkedList<>();
        Random random = new Random();

        while (numberOfSchemas-- > 0) {
            int index = random.nextInt(schemaIndexes.size());
            indexes.add(schemaIndexes.get(index));
            schemaIndexes.remove(index);
        }

        merge(indexes);
    }

    @Override
    public Collection<Parameter> getAllParameters() {
        return new HashSet<>();
    }

    @Override
    public Parameter getParameterFromJsonPath(String jsonPath) {
        return null;
    }
}
