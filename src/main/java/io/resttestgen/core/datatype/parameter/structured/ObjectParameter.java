package io.resttestgen.core.datatype.parameter.structured;

import com.google.gson.JsonObject;
import com.google.gson.internal.LinkedTreeMap;
import io.resttestgen.core.datatype.ParameterName;
import io.resttestgen.core.datatype.parameter.Parameter;
import io.resttestgen.core.datatype.parameter.ParameterFactory;
import io.resttestgen.core.datatype.parameter.attributes.ParameterLocation;
import io.resttestgen.core.datatype.parameter.attributes.ParameterStyle;
import io.resttestgen.core.datatype.parameter.attributes.ParameterType;
import io.resttestgen.core.datatype.parameter.leaves.LeafParameter;
import io.resttestgen.core.datatype.parameter.visitor.Visitor;
import io.resttestgen.core.openapi.EditReadOnlyOperationException;
import io.resttestgen.core.openapi.OpenApiParser;
import io.resttestgen.core.openapi.UnsupportedSpecificationFeature;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

import static io.resttestgen.core.datatype.parameter.ParameterUtils.getLeaves;

public class ObjectParameter extends StructuredParameter {

    /**
     * We keep track of the order of the properties inside objects, but we do not consider it in the equals/hash
     * functions since it should have no importance.
     * From a testing perspective, instead, it could be useful to discover implementation defects.
     */
    private List<Parameter> properties = new LinkedList<>();

    private static final Logger logger = LogManager.getLogger(ObjectParameter.class);

    @SuppressWarnings("unchecked")
    public ObjectParameter(Map<String, Object> schema, String name)  {
        super(schema, name);

        Map<String, Object> properties = OpenApiParser.safeGet(schema, "properties", LinkedTreeMap.class);
        if (properties.isEmpty()) {
            schema = OpenApiParser.safeGet(schema, "schema", LinkedTreeMap.class);
            properties = OpenApiParser.safeGet(schema, "properties", LinkedTreeMap.class);
        }

        Parameter parameter;
        for (Map.Entry<String, Object> propertyMap : properties.entrySet()) {
            try {
                Map<String, Object> map = (Map<String, Object>) propertyMap.getValue();
                // Propagate location value to children
                map.put("in", getLocation().toString());
                parameter = ParameterFactory.getParameter(map, propertyMap.getKey());
                addChild(parameter);

                // Propagate example values to children
                for (Object example : super.examples) {
                    Map<String, Object> exampleMap = (Map<String, Object>) example;
                    Object exampleValue = exampleMap.get(parameter.getName().toString());
                    if (exampleValue != null) {
                        parameter.addExample(exampleValue);
                    }
                }
            } catch (UnsupportedSpecificationFeature e) {
                logger.warn("Skipping property '{}' in object '{}' due to an unsupported feature in OpenAPI specification.", propertyMap.getKey(), name);
            }
        }
    }

    private ObjectParameter(ObjectParameter other) {
        super(other);

        other.properties.forEach(p -> addChild(p.deepClone()));
    }

    public ObjectParameter(Parameter other) {
        super(other);
        type = ParameterType.OBJECT;
    }

    public ObjectParameter() { super(); type = ParameterType.OBJECT; }

    @Override
    public void setLocation(ParameterLocation location) {
        super.setLocation(location);
        properties.forEach(p -> p.setLocation(location));
    }

    @Override
    public Collection<Parameter> getChildren() {
        return Set.copyOf(properties);
    }

    @Override
    public boolean addChild(Parameter child) {
        if (getOperation() != null && getOperation().isReadOnly()) {
            throw new EditReadOnlyOperationException(getOperation());
        }

        if (child != null) {
            Parameter match = getChildren().stream()
                    .filter(p -> p.getName().equals(child.getName())).findFirst().orElse(null);

            if (match == null) {
                child.setParent(this);
                return properties.add(child);
            }

            Parameter merged = match.merge(child);
            merged.setParent(this);
            if (!properties.remove(match)) {
                return false;
            }
            return properties.add(merged);
        }

        return false;
    }

    @Override
    public boolean removeChild(Parameter parameter) {
        if (getOperation() != null && getOperation().isReadOnly()) {
            throw new EditReadOnlyOperationException(getOperation());
        }

        return properties.remove(parameter);
    }

    public ObjectParameter(JsonObject jsonObject, String name) {
        super();
        this.properties = new LinkedList<>();
        this.name = new ParameterName(Objects.requireNonNullElse(name, ""));

        for (String entryName : jsonObject.keySet()) {
            Parameter p =
                    ParameterFactory.getParameter(jsonObject.get(entryName), entryName);
            if (p != null) {
                addChild(p);
            }
        }
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public ObjectParameter merge(Parameter other) {
        if (!(other instanceof ObjectParameter)) {
            throw new IllegalArgumentException("Cannot merge a " + this.getClass() + " instance with a "
                    + other.getClass() + " one.");
        }

        ObjectParameter objectParameter = (ObjectParameter) other;
        ObjectParameter merged = new ObjectParameter(this);

        merged.mergeProperties(objectParameter.getChildren());
        return merged;
    }

    private void mergeProperties(Collection<Parameter> other) {
        Set<ParameterName> otherNames = other.stream()
                .map(Parameter::getName)
                .collect(Collectors.toSet());
        Set<ParameterName> commonNames = getChildren().stream()
                .map(Parameter::getName)
                .collect(Collectors.toSet()).stream()
                .filter(otherNames::contains)
                .collect(Collectors.toSet());

        getChildren().forEach(child -> {
            if (commonNames.contains(child.getName())) {
                Parameter otherMatch = other.stream()
                        .filter(param -> param.getName().equals(child.getName()))
                        .findFirst()
                        .get();
                Parameter merged = child.merge(otherMatch);
                removeChild(child);
                addChild(merged);
            }
        });

        other.stream()
                .filter(otherParam -> !commonNames.contains(otherParam.getName()))
                .forEach(this::addChild);
    }

    @Override
    public boolean isObjectTypeCompliant(Object o) {
        if (o == null) {
            return false;
        }
        return Map.class.isAssignableFrom(o.getClass());
    }

    @Override
    public String getJSONString() {
        StringBuilder stringBuilder = new StringBuilder();

        // If object is inside an array, no heading is printed
        if (getParent() instanceof ArrayParameter) {
            stringBuilder.append("{");
        } else {
            stringBuilder.append(getJSONHeading()).append("{");
        }
        properties.forEach(p -> stringBuilder.append(p.getJSONString()).append(", "));
        int index = stringBuilder.lastIndexOf(",");
        return stringBuilder.substring(0, index > 0 ? index : stringBuilder.length()) + "}";
    }

    @Override
    public String toString() {
        //StringBuilder stringBuilder = new StringBuilder(super.getNormalizedName() + ": {");
        StringBuilder stringBuilder = new StringBuilder(getNormalizedName() + ": {");
        properties.forEach(p -> stringBuilder.append(p.toString()).append(", "));
        int index = stringBuilder.lastIndexOf(",");
        return stringBuilder.substring(0, index > 0 ? index : stringBuilder.length()) + "}";
    }

    public List<Parameter> getProperties() {
        return Collections.unmodifiableList(properties);
    }

    @Override
    public boolean addExample(Object o) {
        if (getOperation() != null && getOperation().isReadOnly()) {
            throw new EditReadOnlyOperationException(getOperation());
        }

        if (!super.addExample(o)) {
            return false;
        }

        // Propagate example values to children
        @SuppressWarnings("unchecked")
        Map<String, Object> exampleMap = (Map<String, Object>) o;
        for (Map.Entry<String, Object> example : exampleMap.entrySet()) {
                properties.stream().filter(p -> p.getName().toString().equals(example.getKey()))
                        .findFirst().ifPresent(parameter -> parameter.addExample(example.getValue()));
        }

        return true;
    }

    // TODO: create function to remove from a StringBuilder last matching chars? Maybe in a Helper class
    public String getValueAsFormattedString (ParameterStyle style, boolean explode) {
        logger.warn("Format for deep nested object is not defined in the reference RFC. Use this method only for " +
                "RFC defined behaviors.");
        StringBuilder stringBuilder = new StringBuilder();
        switch (style) {
            case MATRIX:
                if (explode) {
                    // ;R=100;G=200;B=150
                    properties.forEach(p -> {
                        stringBuilder.append(";");
                        stringBuilder.append(p.getName().toString());
                        stringBuilder.append("=");
                        stringBuilder.append(p.getValueAsFormattedString(ParameterStyle.SIMPLE));
                    });
                } else {
                    // ;color=R,100,G,200,B,150
                    stringBuilder.append(";");
                    stringBuilder.append(getName().toString());
                    stringBuilder.append("=");
                    properties.forEach(p -> {
                        stringBuilder.append(p.getName().toString());
                        stringBuilder.append(",");
                        stringBuilder.append(p.getValueAsFormattedString(ParameterStyle.SIMPLE));
                        stringBuilder.append(",");
                    });
                    if (stringBuilder.charAt(stringBuilder.length() - 1) == ',') {
                        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
                    }
                }
                return stringBuilder.toString();

            case LABEL:
                if (explode) {
                    // 	.R=100.G=200.B=150
                    properties.forEach(p -> {
                        stringBuilder.append(".");
                        stringBuilder.append(p.getName().toString());
                        stringBuilder.append("=");
                        stringBuilder.append(p.getValueAsFormattedString(ParameterStyle.SIMPLE));
                    });
                } else {
                    // .R.100.G.200.B.150
                    properties.forEach(p -> {
                        stringBuilder.append(".");
                        stringBuilder.append(p.getName().toString());
                        stringBuilder.append(".");
                        stringBuilder.append(p.getValueAsFormattedString(ParameterStyle.SIMPLE));
                    });
                }
                return stringBuilder.toString();

            case FORM:
                if (explode) {
                    // R=100&G=200&B=150
                    properties.forEach(p -> {
                        stringBuilder.append(p.getName().toString());
                        stringBuilder.append("=");
                        stringBuilder.append(p.getValueAsFormattedString(ParameterStyle.SIMPLE));
                        stringBuilder.append("&");
                    });
                    if (stringBuilder.charAt(stringBuilder.length() - 1) == '&') {
                        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
                    }
                } else {
                    // color=R,100,G,200,B,150
                    stringBuilder.append(this.getName().toString());
                    stringBuilder.append("=");
                    properties.forEach(p -> {
                        stringBuilder.append(p.getName().toString());
                        stringBuilder.append(",");
                        stringBuilder.append(p.getValueAsFormattedString(ParameterStyle.SIMPLE));
                        stringBuilder.append(",");
                    });
                    if (stringBuilder.charAt(stringBuilder.length() - 1) == ',') {
                        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
                    }
                }
                return stringBuilder.toString();

            case SIMPLE:
                if (explode) {
                    // R=100,G=200,B=150
                    properties.forEach(p -> {
                        stringBuilder.append(p.getName().toString());
                        stringBuilder.append("=");
                        stringBuilder.append(p.getValueAsFormattedString(ParameterStyle.SIMPLE));
                        stringBuilder.append(",");
                    });
                } else {
                    // R,100,G,200,B,150
                    properties.forEach(p -> {
                        stringBuilder.append(p.getName().toString());
                        stringBuilder.append(",");
                        stringBuilder.append(p.getValueAsFormattedString(ParameterStyle.SIMPLE));
                        stringBuilder.append(",");
                    });
                }
                if (stringBuilder.charAt(stringBuilder.length() - 1) == ',') {
                    stringBuilder.deleteCharAt(stringBuilder.length() - 1);
                }
                return stringBuilder.toString();

            case SPACE_DELIMITED:
                // R%20100%20G%20200%20B%20150
                properties.forEach(p -> {
                    stringBuilder.append(p.getName().toString());
                    stringBuilder.append("%20");
                    stringBuilder.append(p.getValueAsFormattedString(ParameterStyle.SIMPLE));
                    stringBuilder.append("%20");
                });
                if (stringBuilder.charAt(stringBuilder.length() - 1) == '0') {
                    return stringBuilder.substring(0, stringBuilder.length() - 3);
                }
                return stringBuilder.toString();

            case PIPE_DELIMITED:
                // R|100|G|200|B|150
                properties.forEach(p -> {
                    stringBuilder.append(p.getName().toString());
                    stringBuilder.append("|");
                    stringBuilder.append(p.getValueAsFormattedString(ParameterStyle.SIMPLE));
                    stringBuilder.append("|");
                });
                if (stringBuilder.charAt(stringBuilder.length() - 1) == '|') {
                    stringBuilder.deleteCharAt(stringBuilder.length() - 1);
                }
                return stringBuilder.toString();

            case DEEP_OBJECT:
                // color[R]=100&color[G]=200&color[B]=150
                properties.forEach(p -> {
                    stringBuilder.append(this.getName().toString());
                    stringBuilder.append("[");
                    stringBuilder.append(p.getName().toString());
                    stringBuilder.append("]=");
                    stringBuilder.append(p.getValueAsFormattedString(ParameterStyle.SIMPLE));
                    stringBuilder.append("&");
                });
                if (stringBuilder.charAt(stringBuilder.length() - 1) == '&') {
                    stringBuilder.deleteCharAt(stringBuilder.length() - 1);
                }
                return stringBuilder.toString();

            default:
                logger.warn("{}: Style not consistent with parameter type. Returning {} style.", getName(), getStyle());
                return getValueAsFormattedString();
        }
    }

    public boolean hasValue() {
        boolean hasValue = true;

        for (Parameter element : properties) {
            hasValue = hasValue && element.hasValue();
        }

        if (!hasValue) {
            logger.warn("Parameter {} has an invalid value.", getName());
        }

        return hasValue;
    }

    /**
     * A ParameterObject is considered empty when it has no property.
     * @return True if the instance has no property; false otherwise
     */
    @Override
    public boolean isEmpty() {
        return properties.isEmpty();
    }

    @Override
    public void removeUninitializedParameters() {
        if (getOperation() != null && getOperation().isReadOnly()) {
            throw new EditReadOnlyOperationException(getOperation());
        }
        List<Parameter> newProperties = new LinkedList<>(this.properties);

        //logger.warn("Empty valued parameter '" + p.getName() + "' found. It will be removed.");
        this.properties.stream().filter(
                p -> LeafParameter.class.isAssignableFrom(p.getClass()) && p.getValue() == null
        ).forEach(newProperties::remove);

        this.properties.stream().filter(
                p -> StructuredParameter.class.isAssignableFrom(p.getClass())
        ).forEach(p -> {
            StructuredParameter structuredP = (StructuredParameter) p;
            structuredP.removeUninitializedParameters();
            if (structuredP.isEmpty() && !structuredP.isKeepIfEmpty()) {
                //logger.warn("Empty valued parameter '" + p.getName() + "' found. It will be removed.");
                newProperties.remove(p);
            }
        });

        this.properties = newProperties;
    }

    @Override
    public ObjectParameter deepClone() {
        return new ObjectParameter(this);
    }

    // Does not take into account properties order
    @Override
    public boolean equals(Object o) {
        return equalsTemplate(o, false);
    }

    // Takes into account properties order
    public boolean enforcedParameterOrderEquals(Object o) {
        return equalsTemplate(o, true);
    }

    private boolean equalsTemplate(Object o, boolean isParameterOrderEnforced) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        ObjectParameter that = (ObjectParameter) o;
        if (properties.isEmpty()) {
            return getNormalizedName().equals(that.getNormalizedName());
        }
        if (!isParameterOrderEnforced) {
            Set<Parameter> propertiesSet = new HashSet<>(properties);
            Set<Parameter> thatPropertiesSet = new HashSet<>(that.properties);
            return propertiesSet.equals(thatPropertiesSet);
        }
        return properties.equals(that.properties);
    }

    @Override
    public Collection<Parameter> getAllParameters() {
        HashSet<Parameter> parameters = new HashSet<>();
        parameters.add(this);
        properties.forEach(p -> parameters.addAll(p.getAllParameters()));
        return parameters;
    }

    @Override
    public boolean isSet() {
        for (Parameter parameter : properties) {
            if (parameter.isSet()) {
                return true;
            }
        }
        return false;
    }

    private int getIndexOfParameterObject(){
        List<Parameter> elements = ((ArrayParameter) getParent()).getElements();
        int elementPosition=0;
        for(Parameter element : elements){
            if(getLeaves(this).equals(getLeaves(element))){
                return elementPosition;
            }
            elementPosition++;
        }
        return elementPosition;
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
                String propertyName = jsonPath.substring(start + 2, end - 1);
                for (Parameter property : properties) {
                    if (property.getName().toString().equals(propertyName)) {
                        if (property instanceof ArrayParameter) {
                            return property.getParameterFromJsonPath(jsonPath.substring(end + 1));
                        } else {
                            return property.getParameterFromJsonPath(jsonPath);
                        }
                    }
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
