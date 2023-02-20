package io.resttestgen.core.openapi;

import com.google.gson.internal.LinkedTreeMap;
import io.resttestgen.core.datatype.HttpMethod;
import io.resttestgen.core.datatype.OperationSemantics;
import io.resttestgen.core.datatype.NormalizedParameterName;
import io.resttestgen.core.datatype.ParameterName;
import io.resttestgen.core.datatype.parameter.*;
import kotlin.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Operation {

    private final String endpoint;
    private final HttpMethod method;
    private final String operationId;

    private final String description;

    private OperationSemantics operationSemantics;
    private OperationSemantics inferredOperationSemantics;

    private String crudResourceType;
    private String inferredCrudResourceType;

    private Set<ParameterElement> headerParameters;
    private Set<ParameterElement> pathParameters;
    private Set<ParameterElement> queryParameters;
    private Set<ParameterElement> cookieParameters;
    private String requestContentType;
    private StructuredParameterElement requestBody;
    private String requestBodyDescription;

    private final Map<String, StructuredParameterElement> outputParameters;
    private StructuredParameterElement responseBody;

    // Inter-parameter dependencies
    private final List<Pair<String, String>> requires;
    private final List<Set<ParameterName>> or;
    private final List<Set<ParameterName>> onlyOne;
    private final List<Set<ParameterName>> allOrNone;
    private final List<Set<ParameterName>> zeroOrOne;
    // TODO: support arithmetic/relational IPDs

    private boolean isReadOnly; // Field to avoid changes to template operation parsed from specification

    private static final Logger logger = LogManager.getLogger(Operation.class);

    @SuppressWarnings("unchecked")
    public Operation(String endpoint, HttpMethod method, Map<String, Object> operationMap) throws InvalidOpenAPIException {
        this.endpoint = endpoint;
        this.method = method;

        this.operationSemantics = OperationSemantics.parseSemantics(
                OpenAPIParser.safeGet(operationMap, "x-crudOperationSemantics", String.class));
        this.crudResourceType = OpenAPIParser.safeGet(operationMap, "x-crudResourceType", String.class).trim();

        headerParameters = new HashSet<>();
        pathParameters = new HashSet<>();
        queryParameters = new HashSet<>();
        cookieParameters = new HashSet<>();

        this.outputParameters = new HashMap<>();

        requires = new LinkedList<>();
        or = new LinkedList<>();
        onlyOne = new LinkedList<>();
        allOrNone = new LinkedList<>();
        zeroOrOne = new LinkedList<>();

        ArrayList<String> interParameterDependencies = OpenAPIParser.safeGet(operationMap, "x-dependencies", ArrayList.class);
        HashMap<String, List<Set<ParameterName>>> ipdTokenSetMapping = new HashMap<>();
        ipdTokenSetMapping.put("Or", or);
        ipdTokenSetMapping.put("OnlyOne", onlyOne);
        ipdTokenSetMapping.put("AllOrNone", allOrNone);
        ipdTokenSetMapping.put("ZeroOrOne", zeroOrOne);

        // Parsing of IPDs
        for (String dependency : interParameterDependencies) {
            dependency = dependency.trim();
            System.out.println(dependency);

            // Requires IPDs
            Pattern pattern = Pattern.compile("IF (.*) THEN (.*);");
            Matcher matcher = pattern.matcher(dependency);
            if (matcher.find()) {
                requires.add(new Pair<>(matcher.group(1), matcher.group(2)));
                continue;
            }

            // Set IPDs
            for (String idpToken : ipdTokenSetMapping.keySet()) {
                if (dependency.startsWith(idpToken + "(") && dependency.endsWith(");")) {
                    dependency = dependency.substring(idpToken.length() + 1, dependency.length() - 2);
                    List<Set<ParameterName>> currentIdp = ipdTokenSetMapping.get(idpToken);
                    Set<ParameterName> currentIdpSet = new HashSet<>();
                    String[] params = dependency.split(",");
                    for (String param : params) {
                        currentIdpSet.add(new ParameterName(param.trim()));
                    }
                    currentIdp.add(currentIdpSet);
                }
            }
        }

        logger.debug("Fetching operation " + method + " " + endpoint);

        operationId = OpenAPIParser.safeGet(operationMap, "operationId", String.class);

        description = OpenAPIParser.safeGet(operationMap, "description", String.class);

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
        this.requestBodyDescription = OpenAPIParser.safeGet(requestBody, "description", String.class);
        Map<String, Object> content = OpenAPIParser.safeGet(requestBody, "content", LinkedTreeMap.class);
        Map<String, Object> jsonContent = OpenAPIParser.safeGet(content, "application/json", LinkedTreeMap.class);
        if (!jsonContent.isEmpty()) {
            this.requestContentType = "application/json";
        } else {
            jsonContent = OpenAPIParser.safeGet(content, "application/x-www-form-urlencoded", LinkedTreeMap.class);
            if (!jsonContent.isEmpty()) {
                this.requestContentType = "application/x-www-form-urlencoded";
            } else {
                jsonContent = OpenAPIParser.safeGet(content, "*/*", LinkedTreeMap.class);
                if (!jsonContent.isEmpty()) {
                    this.requestContentType = "application/json";
                }
            }
        }
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
            if (jsonContent.isEmpty()) {
                jsonContent = OpenAPIParser.safeGet(content, "application/x-www-form-urlencoded", LinkedTreeMap.class);
                if (jsonContent.isEmpty()) {
                    jsonContent = OpenAPIParser.safeGet(content, "*/*", LinkedTreeMap.class);
                }
            }
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

        // TODO: check if this code is required
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
        inferredOperationSemantics = OperationSemantics.inferSemantics(this);
    }

    private Operation(Operation other) {
        endpoint = other.endpoint;
        method = HttpMethod.getMethod(other.method.toString());
        operationId = other.operationId;
        description = other.description;

        operationSemantics = other.operationSemantics;
        inferredOperationSemantics = other.inferredOperationSemantics;
        crudResourceType = other.crudResourceType;
        inferredCrudResourceType = other.inferredCrudResourceType;

        headerParameters = new HashSet<>();
        other.headerParameters.forEach(p -> headerParameters.add(p.deepClone(this, null)));

        pathParameters = new HashSet<>();
        other.pathParameters.forEach(p -> pathParameters.add(p.deepClone(this, null)));

        queryParameters = new HashSet<>();
        other.queryParameters.forEach(p -> queryParameters.add(p.deepClone(this, null)));

        cookieParameters = new HashSet<>();
        other.cookieParameters.forEach(p -> cookieParameters.add(p.deepClone(this, null)));

        requestContentType = other.requestContentType;
        requestBody = other.requestBody != null ? other.requestBody.deepClone(this, null) : null;
        requestBodyDescription = other.requestBodyDescription;
        responseBody = other.responseBody != null ? other.responseBody.deepClone(this, null) : null;

        outputParameters = new HashMap<>();
        other.outputParameters.forEach((key, value) ->
                outputParameters.put(key, value.deepClone(this, null)));

        requires = other.requires;
        // FIXME: do sets have to be cloned element by element?
        or = new LinkedList<>(other.or);
        onlyOne = new LinkedList<>(other.onlyOne);
        allOrNone = new LinkedList<>(other.allOrNone);
        zeroOrOne = new LinkedList<>(other.zeroOrOne);

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

    public String getDescription() {
        return description;
    }

    /**
     * Return all the parameters in the request that are not in the body.,
     * @return all the parameters in the request that are not in the body.,
     */
    public Collection<ParameterElement> getAllRequestParametersNotInBody() {
        Set<ParameterElement> parameters = new HashSet<>();

        for (ParameterElement element : headerParameters) {
            parameters.addAll(element.getAllParameters());
        }

        for (ParameterElement element : pathParameters) {
            parameters.addAll(element.getAllParameters());
        }

        for (ParameterElement element : queryParameters) {
            parameters.addAll(element.getAllParameters());
        }

        for (ParameterElement element : cookieParameters) {
            parameters.addAll(element.getAllParameters());
        }

        return parameters;
    }

    /**
     * Return all the parameter elements in the request.
     * @return all the parameter elements in the request.
     */
    public Collection<ParameterElement> getAllRequestParameters() {

        // Get all request parameters not in body
        Collection<ParameterElement> parameters = getAllRequestParametersNotInBody();

        // Add the parameters in the body, if any
        if (requestBody != null) {
            parameters.addAll(requestBody.getAllParameters());
        }

        return parameters;
    }

    public StructuredParameterElement getSuccessfulOutputParameters() {
        for (String key : outputParameters.keySet()) {
            if (key.startsWith("2")) {
                return outputParameters.get(key);
            }
        }
        return null;
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

    public String getRequestBodyDescription() {
        return requestBodyDescription;
    }

    public StructuredParameterElement getResponseBody() {
        return responseBody;
    }

    public List<Pair<String, String>> getRequires() {
        return requires;
    }

    public List<Set<ParameterName>> getOr() {
        return or;
    }

    public List<Set<ParameterName>> getOnlyOne() {
        return onlyOne;
    }

    public List<Set<ParameterName>> getAllOrNone() {
        return allOrNone;
    }

    public List<Set<ParameterName>> getZeroOrOne() {
        return zeroOrOne;
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

    public Collection<ParameterElement> getFirstLevelRequestParametersNotInBody() {
        Collection<ParameterElement> parameters = new HashSet<>();
        parameters.addAll(headerParameters);
        parameters.addAll(pathParameters);
        parameters.addAll(queryParameters);
        parameters.addAll(cookieParameters);
        return parameters;
    }

    public Collection<ParameterElement> getFirstLevelRequestParameters() {
        Collection<ParameterElement> parameters = getFirstLevelRequestParametersNotInBody();
        parameters.addAll(getFirstLevelParameters(requestBody));
        return parameters;
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

    public Set<ParameterElement> getOutputParametersSet() {
        Set<ParameterElement> outParams = new HashSet<>();
        for (StructuredParameterElement responseBody : outputParameters.values()) {
            outParams.addAll(responseBody.getReferenceLeaves());
        }
        return outParams;
    }

    public OperationSemantics getCrudSemantics() {
        return this.operationSemantics;
    }

    public void setCrudSemantics(OperationSemantics operationSemantics) {
        this.operationSemantics = operationSemantics;
    }

    public String getCrudResourceType() {
        return this.crudResourceType;
    }

    public void setCrudResourceType(String crudResourceType) {
        this.crudResourceType = crudResourceType;
    }

    public OperationSemantics getInferredCrudSemantics() {
        return inferredOperationSemantics;
    }

    public void setInferredCrudSemantics(OperationSemantics inferredOperationSemantics) {
        this.inferredOperationSemantics = inferredOperationSemantics;
    }

    public String getInferredCrudResourceType() {
        return inferredCrudResourceType;
    }

    public void setInferredCrudResourceType(String inferredCrudResourceType) {
        this.inferredCrudResourceType = inferredCrudResourceType;
    }

    public List<ParameterElement> searchRequestParametersByName(ParameterName parameterName) {
        return getAllRequestParameters().stream().filter(p -> p.getName().equals(parameterName)).collect(Collectors.toList());
    }

    public List<ParameterElement> searchResponseParametersByName(ParameterName parameterName) {
        List<ParameterElement> foundParameters = new LinkedList<>();
        getOutputParametersSet().forEach(p -> {
            if (p.getName().equals(parameterName)) {
                foundParameters.add(p);
            }
        });
        return foundParameters;
    }

    // FIXME: check if all parameters can be used, rather than only reference leaves
    public List<ParameterElement> searchReferenceRequestParametersByNormalizedName(NormalizedParameterName normalizedParameterName) {
        return getReferenceLeaves().stream().filter(p -> p.getNormalizedName().equals(normalizedParameterName)).collect(Collectors.toList());
    }

    public List<ParameterElement> searchResponseParametersByNormalizedName(NormalizedParameterName normalizedParameterName) {
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

    public String getRequestContentType() {
        return requestContentType;
    }

    public void setRequestContentType(String requestContentType) {
        this.requestContentType = requestContentType;
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
