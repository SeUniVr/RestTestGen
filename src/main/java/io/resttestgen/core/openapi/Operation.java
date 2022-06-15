package io.resttestgen.core.openapi;

import com.google.gson.internal.LinkedTreeMap;
import io.resttestgen.core.datatype.HttpMethod;
import io.resttestgen.core.datatype.NormalizedParameterName;
import io.resttestgen.core.datatype.parameter.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class Operation {

    private final String endpoint;
    private final HttpMethod method;
    private final String operationId;

    private Set<ParameterElement> headerParameters;
    private Set<ParameterElement> pathParameters;
    private Set<ParameterElement> queryParameters;
    private Set<ParameterElement> cookieParameters;
    private StructuredParameterElement requestBody;

    private final Map<String, StructuredParameterElement> outputParameters;
    private StructuredParameterElement responseBody;

    private boolean isReadOnly; // Field to avoid changes to template operation parsed from specification

    private static final Logger logger = LogManager.getLogger(Operation.class);

    public Operation(String endpoint, HttpMethod method, Map<String, Object> operationMap) throws InvalidOpenAPIException {
        this.endpoint = endpoint;
        this.method = method;

        headerParameters = new HashSet<>();
        pathParameters = new HashSet<>();
        queryParameters = new HashSet<>();
        cookieParameters = new HashSet<>();

        this.outputParameters = new HashMap<>();

        logger.debug("Fetching operation " + method + " " + endpoint);

        operationId = OpenAPIParser.safeGet(operationMap, "operationId", String.class);

        // Check for header/path/query parameters
        List<Map<String, Object>> parameters = OpenAPIParser.safeGet(operationMap, "parameters", ArrayList.class);
        ParameterElement parameterElement;
        for (Map<String, Object> parameter : parameters) {
            Set<ParameterElement> targetSet;
            switch ((String) parameter.get("in")) {
                case "header" :
                    targetSet = headerParameters;
                    break;
                case "path" :
                    targetSet = pathParameters;
                    break;
                case "query":
                    targetSet = queryParameters;
                    break;
                case "cookie":
                    targetSet = cookieParameters;
                    break;
                default :
                    logger.warn("Skipping parameter \"" + parameter.get("name") + "\" in operation \"" + this +
                            "\" due to a wrong \"in\" field value (actual value:" + parameter.get("in") + " ).");
                    continue;
            }
            try {
                parameterElement = ParameterFactory.getParameterElement(null, parameter, this);
                targetSet.add(parameterElement);
            } catch (ParameterCreationException e) {
                logger.warn("Skipping parameter \"" + parameter.get("name") + "\" in operation \"" + this + "\" due to " +
                        "a ParameterCreationException.");
            }
        }

        // Check for body parameters
        Map<String, Object> requestBody = OpenAPIParser.safeGet(operationMap, "requestBody", LinkedTreeMap.class);
        Map<String, Object> content = OpenAPIParser.safeGet(requestBody, "content", LinkedTreeMap.class);
        Map<String, Object> jsonContent = OpenAPIParser.safeGet(content, "application/json", LinkedTreeMap.class);
        Map<String, Object> schema = OpenAPIParser.safeGet(jsonContent, "schema", LinkedTreeMap.class);

        if (!schema.isEmpty()) {
            try {
                // Set location for Parameter init
                schema.put("in", "request_body");
                this.requestBody = ParameterFactory.getStructuredParameter(null, schema, this, "");
            } catch (ParameterCreationException e) {
                logger.warn("Skipping \"request body\" in operation \"" + this + "\" due to " +
                        "a ParameterCreationException.");
            } catch (UnsupportedSpecificationFeature e) {
                logger.warn("Skipping \"request body\" in operation \"" + this + "\" due to " +
                        "an unsupported feature in OpenAPI specification.");
            }
        }

        // Check for output parameters (response body)
        Map<String, Object> responses = OpenAPIParser.safeGet(operationMap, "responses", LinkedTreeMap.class);

        StructuredParameterElement structuredParameterElement;
        for (Map.Entry<String, Object> responseMap : responses.entrySet()) {
            Map<String, Object> response = (Map<String, Object>) responseMap.getValue();
            content = OpenAPIParser.safeGet(response, "content", LinkedTreeMap.class);
            jsonContent = OpenAPIParser.safeGet(content, "application/json", LinkedTreeMap.class);
            schema = OpenAPIParser.safeGet(jsonContent, "schema", LinkedTreeMap.class);

            if (!schema.isEmpty()) {
                try {
                    // Set location for Parameter init
                    schema.put("in", "response_body");
                    structuredParameterElement = ParameterFactory.getStructuredParameter(null, schema, this, "");
                    outputParameters.put(responseMap.getKey(), structuredParameterElement);
                } catch (ParameterCreationException e) {
                    logger.warn("Skipping \"response body\" for status code \"" + responseMap.getKey() + "\" due to " +
                            "a ParameterCreationException.");
                } catch (UnsupportedSpecificationFeature e) {
                    logger.warn("Skipping \"response body\" for status code \"" + responseMap.getKey() + "\" due to " +
                            "an unsupported feature in OpenAPI specification.");
                }
            }
        }

        // Fill targetSchemas with the schemaNames found at the first level of depth
        /*this.targetSchemas.addAll(
                pathParameters.stream().map(p -> p.getSchemaName())
                        .filter(s -> s != null).collect(Collectors.toList())
        );
        this.targetSchemas.addAll(
                queryParameters.stream().map(p -> p.getSchemaName())
                        .filter(s -> s != null).collect(Collectors.toList())
        );
        this.targetSchemas.addAll(
                headerParameters.stream().map(p -> p.getSchemaName())
                        .filter(s -> s != null).collect(Collectors.toList())
        );
        this.targetSchemas.addAll(
                cookieParameters.stream().map(p -> p.getSchemaName())
                        .filter(s -> s != null).collect(Collectors.toList())
        );

        if (this.requestBody != null && this.requestBody.getSchemaName() != null) {
            this.targetSchemas.add(this.requestBody.getSchemaName());
        }*/

        logger.debug("\tPathParams: " + pathParameters);
        logger.debug("\tQueryParams: " + queryParameters);
        logger.debug("\tHeaderParams: " + headerParameters);
        logger.debug("\tCookieParams: " + cookieParameters);
        logger.debug("\tRequestBody: " + this.requestBody);
        logger.debug("\tOutputParams: " + outputParameters);
        logger.debug("\tOutputParamsSet: " + getOutputParametersSet());

        this.isReadOnly = true;
    }

    private Operation(Operation other) {
        endpoint = other.endpoint;
        method = HttpMethod.getMethod(other.method.toString());
        operationId = other.operationId;

        headerParameters = new HashSet<>();
        other.headerParameters.forEach(p -> headerParameters.add(p.deepClone(this, null)));

        pathParameters = new HashSet<>();
        other.pathParameters.forEach(p -> pathParameters.add(p.deepClone(this, null)));

        queryParameters = new HashSet<>();
        other.queryParameters.forEach(p -> queryParameters.add(p.deepClone(this, null)));

        cookieParameters = new HashSet<>();
        other.cookieParameters.forEach(p -> cookieParameters.add(p.deepClone(this, null)));

        requestBody = other.requestBody != null ? other.requestBody.deepClone(this, null) : null;
        responseBody = other.responseBody != null ? other.responseBody.deepClone(this, null) : null;

        outputParameters = new HashMap<>();
        other.outputParameters.forEach((key, value) ->
                outputParameters.put(key, value.deepClone(this, null)));

        isReadOnly = false;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public String getOperationId() {
        return operationId;
    }

    public Set<ParameterElement> getHeaderParameters() {
        if (isReadOnly) {
            return Collections.unmodifiableSet(headerParameters);
        }
        return headerParameters;
    }

    public Set<ParameterElement> getPathParameters() {
        if (isReadOnly) {
            return Collections.unmodifiableSet(pathParameters);
        }
        return pathParameters;
    }

    public Set<ParameterElement> getQueryParameters() {
        if (isReadOnly) {
            return Collections.unmodifiableSet(queryParameters);
        }
        return queryParameters;
    }

    public Set<ParameterElement> getCookieParameters() {
        if (isReadOnly) {
            return Collections.unmodifiableSet(cookieParameters);
        }
        return cookieParameters;
    }

    public StructuredParameterElement getRequestBody() {
        return requestBody;
    }

    public StructuredParameterElement getResponseBody() {
        return responseBody;
    }

    public Collection<ParameterLeaf> getLeaves() {

        Collection<ParameterLeaf> leaves = new LinkedList<>();

        for (ParameterElement element : headerParameters) {
            leaves.addAll(element.getLeaves());
        }

        for (ParameterElement element : pathParameters) {
            leaves.addAll(element.getLeaves());
        }

        for (ParameterElement element : queryParameters) {
            leaves.addAll(element.getLeaves());
        }

        for (ParameterElement element : cookieParameters) {
            leaves.addAll(element.getLeaves());
        }

        if (requestBody != null) {
            leaves.addAll(requestBody.getLeaves());
        }

        return leaves;

        // Old version by Amedeo
        /*
        List<ParameterLeaf> leaves = new LinkedList<>();
        if (this.requestBody != null) {
            leaves.addAll(this.requestBody.getLeaves());
        }

        List<ParameterElement> params = new LinkedList<>(this.headerParameters);
        params.addAll(this.pathParameters);
        params.addAll(this.cookieParameters);
        params.stream().forEach(h -> {
            if (h instanceof ParameterLeaf) {
                leaves.add((ParameterLeaf) h);
            } else if (h instanceof StructuredParameterElement) {
                leaves.addAll(((StructuredParameterElement) h).getLeaves());
            } else {
                leaves.addAll(((CombinedSchemaParameter) h).getLeaves());
            }
        });

        return leaves;*/
    }

    /**
     * Returns the collection consisting of all the arrays in the parameters of the operation
     * @return the collection of arrays
     */
    public Collection<ParameterArray> getArrays() {
        Collection<ParameterArray> arrays = new LinkedList<>();

        for (ParameterElement element : headerParameters) {
            arrays.addAll(element.getArrays());
        }

        for (ParameterElement element : pathParameters) {
            arrays.addAll(element.getArrays());
        }

        for (ParameterElement element : queryParameters) {
            arrays.addAll(element.getArrays());
        }

        for (ParameterElement element : cookieParameters) {
            arrays.addAll(element.getArrays());
        }

        if (requestBody != null) {
            arrays.addAll(requestBody.getArrays());
        }

        return arrays;
    }

    public Collection<CombinedSchemaParameter> getCombinedSchemas() {
        Collection<CombinedSchemaParameter> combinedSchemas = new LinkedList<>();

        for (ParameterElement element : headerParameters) {
            combinedSchemas.addAll(element.getCombinedSchemas());
        }

        for (ParameterElement element : pathParameters) {
            combinedSchemas.addAll(element.getCombinedSchemas());
        }

        for (ParameterElement element : queryParameters) {
            combinedSchemas.addAll(element.getCombinedSchemas());
        }

        for (ParameterElement element : cookieParameters) {
            combinedSchemas.addAll(element.getCombinedSchemas());
        }

        if (requestBody != null) {
            combinedSchemas.addAll(requestBody.getCombinedSchemas());
        }

        return combinedSchemas;
    }

    public void setHeaderParameters(Set<ParameterElement> headerParameters) {
        if (isReadOnly) {
            throw new EditReadOnlyOperationException(this);
        }
        this.headerParameters = headerParameters;
    }

    public void setPathParameters(Set<ParameterElement> pathParameters) {
        if (isReadOnly) {
            throw new EditReadOnlyOperationException(this);
        }
        this.pathParameters = pathParameters;
    }

    public void setQueryParameters(Set<ParameterElement> queryParameters) {
        if (isReadOnly) {
            throw new EditReadOnlyOperationException(this);
        }
        this.queryParameters = queryParameters;
    }

    public void setCookieParameters(Set<ParameterElement> cookieParameters) {
        if (isReadOnly) {
            throw new EditReadOnlyOperationException(this);
        }
        this.cookieParameters = cookieParameters;
    }

    public void setRequestBody(StructuredParameterElement requestBody) {
        if (isReadOnly) {
            throw new EditReadOnlyOperationException(this);
        }
        this.requestBody = requestBody;
    }

    public void setResponseBody(StructuredParameterElement responseBody) {
        this.responseBody = responseBody;
    }

    // TODO: add getFuzzableParameterSet (exclude not fuzzable params, e.g., auth)?
    // FIXME: add combined parameter management
    // The commented part is the old implementation. Newer implementation should work better
    public List<ParameterLeaf> getReferenceLeaves() {
        /*Set<ParameterElement> parameters = new HashSet<>(pathParameters);
        parameters.addAll(headerParameters);
        parameters.addAll(queryParameters);
        parameters.addAll(cookieParameters);

        if (requestBody != null) {
            parameters.addAll(requestBody.getLeaves());
            requestBody.getArrays().forEach(parameterArray ->
                    parameters.addAll(parameterArray.getReferenceElement().getLeaves()));
        }

        return parameters;*/

        List<ParameterLeaf> parameters = new LinkedList<>();

        pathParameters.forEach(p -> parameters.addAll(p.getReferenceLeaves()));
        headerParameters.forEach(p -> parameters.addAll(p.getReferenceLeaves()));
        queryParameters.forEach(p -> parameters.addAll(p.getReferenceLeaves()));
        cookieParameters.forEach(p -> parameters.addAll(p.getReferenceLeaves()));

        if (requestBody != null) {
            parameters.addAll(requestBody.getReferenceLeaves());
        }

        return parameters;
    }

    public Map<String, StructuredParameterElement> getOutputParameters() {
        if (isReadOnly) {
            return Collections.unmodifiableMap(outputParameters);
        }
        return outputParameters;
    }

    public Set<ParameterElement> getFirstLevelInputParameters() {
        Set<ParameterElement> firstLevelInputParameters = new HashSet<>();
        firstLevelInputParameters.addAll(headerParameters);
        firstLevelInputParameters.addAll(pathParameters);
        firstLevelInputParameters.addAll(queryParameters);
        firstLevelInputParameters.addAll(cookieParameters);
        firstLevelInputParameters.addAll(getFirstLevelParameters(requestBody));
        return firstLevelInputParameters;
    }

    public Set<ParameterElement> getFirstLevelOutputParameters() {
        Set<ParameterElement> firstLevelOutputParameters = new HashSet<>();
        outputParameters.values().forEach(p -> firstLevelOutputParameters.addAll(getFirstLevelParameters(p)));
        return firstLevelOutputParameters;
    }

    private Set<ParameterElement> getFirstLevelParameters(ParameterElement element) {
        Set<ParameterElement> firstLevelParameters = new HashSet<>();
        if (element instanceof ParameterObject) {
            firstLevelParameters.addAll(((ParameterObject) element).getProperties());
        } else if (element instanceof ParameterArray) {
            firstLevelParameters.addAll(getFirstLevelParameters(((ParameterArray) element).getReferenceElement()));
        } else if (element instanceof ParameterLeaf) {
            firstLevelParameters.add(element);
        }
        return firstLevelParameters;
    }

    // FIXME: add combined parameter management
    // The commented part is the old implementation. Newer implementation should work better
    public Set<ParameterElement> getOutputParametersSet() {
         /*Set<ParameterElement> outParams = new HashSet<>();

         for (StructuredParameterElement responseBody : outputParameters.values()) {
             outParams.addAll(responseBody.getLeaves());
             responseBody.getArrays().forEach(parameterArray ->
                     outParams.addAll(parameterArray.getReferenceElement().getLeaves()));
         }

        return outParams;*/
        Set<ParameterElement> outParams = new HashSet<>();

        for (StructuredParameterElement responseBody : outputParameters.values()) {
            outParams.addAll(responseBody.getReferenceLeaves());
        }

        return outParams;
    }

    public List<ParameterLeaf> searchInputParameterByNormalizedName(NormalizedParameterName normalizedParameterName) {
        List<ParameterLeaf> foundParameters = new LinkedList<>();
        getReferenceLeaves().forEach(p -> {
            if (p.getNormalizedName().equals(normalizedParameterName)) {
                foundParameters.add(p);
            }
        });
        return foundParameters;
    }

    public List<ParameterElement> searchOutputParameterByNormalizedName(NormalizedParameterName normalizedParameterName) {
        List<ParameterElement> foundParameters = new LinkedList<>();
        getOutputParametersSet().forEach(p -> {
            if (p.getNormalizedName().equals(normalizedParameterName)) {
                foundParameters.add(p);
            }
        });
        return foundParameters;
    }

    public Operation deepClone() {
        return new Operation(this);
    }

    public boolean isReadOnly() {
        return isReadOnly;
    }

    public void setReadOnly() {
        this.isReadOnly = true;
    }

    @Override
    public String toString() {
        return this.method + " " + this.endpoint;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Operation operation = (Operation) o;
        return
                Objects.equals(endpoint, operation.endpoint) &&
                Objects.equals(method, operation.method) &&
                Objects.equals(operationId, operation.operationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(endpoint, method, operationId);
    }
}
