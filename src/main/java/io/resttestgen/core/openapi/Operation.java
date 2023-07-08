package io.resttestgen.core.openapi;

import com.google.gson.internal.LinkedTreeMap;
import io.resttestgen.core.datatype.HttpMethod;
import io.resttestgen.core.datatype.OperationSemantics;
import io.resttestgen.core.datatype.NormalizedParameterName;
import io.resttestgen.core.datatype.ParameterName;
import io.resttestgen.core.datatype.parameter.*;
import io.resttestgen.core.datatype.rule.Rule;
import io.resttestgen.core.datatype.parameter.attributes.ParameterLocation;
import io.resttestgen.core.datatype.parameter.combined.CombinedSchemaParameter;
import io.resttestgen.core.datatype.parameter.exceptions.ParameterCreationException;
import io.resttestgen.core.datatype.parameter.leaves.LeafParameter;
import io.resttestgen.core.datatype.parameter.structured.ObjectParameter;
import io.resttestgen.core.datatype.parameter.structured.ArrayParameter;
import io.resttestgen.core.datatype.parameter.structured.StructuredParameter;
import io.resttestgen.core.datatype.parameter.visitor.ArraysVisitor;
import io.resttestgen.core.datatype.parameter.visitor.CombinedSchemasVisitor;
import io.resttestgen.core.datatype.parameter.visitor.LeavesVisitor;
import io.resttestgen.core.datatype.parameter.visitor.Visitor;
import io.resttestgen.core.helper.RestPathHelper;
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
    private final String summary;
    private final HashSet<Rule> rulesToValidate;

    private OperationSemantics operationSemantics;
    private OperationSemantics inferredOperationSemantics;

    private String crudResourceType;
    private String inferredCrudResourceType;

    private Set<Parameter> headerParameters = new HashSet<>();
    private Set<Parameter> pathParameters = new HashSet<>();
    private Set<Parameter> queryParameters = new HashSet<>();
    private Set<Parameter> cookieParameters = new HashSet<>();
    private String requestContentType;
    private StructuredParameter requestBody;
    private String requestBodyDescription;

    private final Map<String, StructuredParameter> outputParameters = new HashMap<>();
    private StructuredParameter responseBody;

    // Inter-parameter dependencies
    private final List<Pair<String, String>> requires = new LinkedList<>();
    private final List<Set<ParameterName>> or = new LinkedList<>();
    private final List<Set<ParameterName>> onlyOne = new LinkedList<>();
    private final List<Set<ParameterName>> allOrNone = new LinkedList<>();
    private final List<Set<ParameterName>> zeroOrOne = new LinkedList<>();
    // TODO: support arithmetic/relational IPDs

    private boolean isReadOnly = false; // Field to avoid changes to template operation parsed from specification

    private static final Logger logger = LogManager.getLogger(Operation.class);

    @SuppressWarnings("unchecked")
    public Operation(String endpoint, HttpMethod method, Map<String, Object> operationMap) throws InvalidOpenApiException {
        this.endpoint = endpoint;
        this.method = method;

        this.operationSemantics = OperationSemantics.parseSemantics(
                OpenApiParser.safeGet(operationMap, "x-crudOperationSemantics", String.class));
        this.crudResourceType = OpenApiParser.safeGet(operationMap, "x-crudResourceType", String.class).trim();

        rulesToValidate = new HashSet<>();

        ArrayList<String> interParameterDependencies = OpenApiParser.safeGet(operationMap, "x-dependencies", ArrayList.class);
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

        operationId = OpenApiParser.safeGet(operationMap, "operationId", String.class);
        description = OpenApiParser.safeGet(operationMap, "description", String.class);
        summary = OpenApiParser.safeGet(operationMap, "summary", String.class);

        // Check for header/path/query parameters
        List<Map<String, Object>> params = OpenApiParser.safeGet(operationMap, "parameters", ArrayList.class);
        for (Map<String, Object> param : params) {
            ParameterLocation location = ParameterLocation.getLocationFromString((String) param.get("in"));
            Parameter parameter = null;

            try {
                parameter = ParameterFactory.getParameter(param);
            } catch (ParameterCreationException e) {
                logger.warn("Skipping parameter \"" + param.get("name") + "\" in operation \"" + this + "\" due to " +
                        "a ParameterCreationException.");
            }

            if (parameter != null) {
                if (!addParameter(location, parameter)) {
                    logger.warn("Skipping parameter \"" + param.get("name") + "\" in operation \"" + this +
                            "\" due to a wrong \"in\" field value (actual value:" + param.get("in") + " ).");
                }
            }
        }

        // Check for body parameters
        Map<String, Object> requestBody = OpenApiParser.safeGet(operationMap, "requestBody", LinkedTreeMap.class);
        this.requestBodyDescription = OpenApiParser.safeGet(requestBody, "description", String.class);
        Map<String, Object> content = OpenApiParser.safeGet(requestBody, "content", LinkedTreeMap.class);
        Map<String, Object> jsonContent = OpenApiParser.safeGet(content, "application/json", LinkedTreeMap.class);
        if (!jsonContent.isEmpty()) {
            this.requestContentType = "application/json";
        } else {
            jsonContent = OpenApiParser.safeGet(content, "application/x-www-form-urlencoded", LinkedTreeMap.class);
            if (!jsonContent.isEmpty()) {
                this.requestContentType = "application/x-www-form-urlencoded";
            } else {
                jsonContent = OpenApiParser.safeGet(content, "*/*", LinkedTreeMap.class);
                if (!jsonContent.isEmpty()) {
                    this.requestContentType = "application/json";
                }
            }
        }
        Map<String, Object> schema = OpenApiParser.safeGet(jsonContent, "schema", LinkedTreeMap.class);

        if (!schema.isEmpty()) {
            try {
                // Set location for Parameter init
                schema.put("in", "request_body");
                setRequestBody(ParameterFactory.getStructuredParameter(schema, ""));
            } catch (ParameterCreationException e) {
                logger.warn("Skipping \"request body\" in operation \"" + this + "\" due to " +
                        "a ParameterCreationException.");
            } catch (UnsupportedSpecificationFeature e) {
                logger.warn("Skipping \"request body\" in operation \"" + this + "\" due to " +
                        "an unsupported feature in OpenAPI specification.");
            }
        }

        // Check for output parameters (response body)
        Map<String, Object> responses = OpenApiParser.safeGet(operationMap, "responses", LinkedTreeMap.class);

        for (Map.Entry<String, Object> responseMap : responses.entrySet()) {
            Map<String, Object> response = (Map<String, Object>) responseMap.getValue();
            content = OpenApiParser.safeGet(response, "content", LinkedTreeMap.class);
            jsonContent = OpenApiParser.safeGet(content, "application/json", LinkedTreeMap.class);
            if (jsonContent.isEmpty()) {
                jsonContent = OpenApiParser.safeGet(content, "application/x-www-form-urlencoded", LinkedTreeMap.class);
                if (jsonContent.isEmpty()) {
                    jsonContent = OpenApiParser.safeGet(content, "*/*", LinkedTreeMap.class);
                }
            }
            schema = OpenApiParser.safeGet(jsonContent, "schema", LinkedTreeMap.class);

            if (!schema.isEmpty()) {
                try {
                    // Set location for Parameter init
                    schema.put("in", "response_body");
                    putOutputParameter(
                            responseMap.getKey(),
                            ParameterFactory.getStructuredParameter(schema, "")
                    );
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
        summary = other.summary;
        rulesToValidate = new HashSet<>(other.rulesToValidate);

        operationSemantics = other.operationSemantics;
        inferredOperationSemantics = other.inferredOperationSemantics;
        crudResourceType = other.crudResourceType;
        inferredCrudResourceType = other.inferredCrudResourceType;

        other.headerParameters.forEach(p -> addParameter(ParameterLocation.HEADER, p.deepClone()));
        other.pathParameters.forEach(p -> addParameter(ParameterLocation.PATH, p.deepClone()));
        other.queryParameters.forEach(p -> addParameter(ParameterLocation.QUERY, p.deepClone()));
        other.cookieParameters.forEach(p -> addParameter(ParameterLocation.COOKIE, p.deepClone()));

        requestContentType = other.requestContentType;

        setRequestBody(other.requestBody != null ? other.requestBody.deepClone() : null);
        requestBodyDescription = other.requestBodyDescription;

        setResponseBody(other.responseBody != null ? other.responseBody.deepClone() : null);
        other.outputParameters.forEach((key, value) ->
                putOutputParameter(key, value.deepClone()));

        requires.addAll(other.requires);
        other.or.forEach(s -> or.add(s.stream().map(ParameterName::deepClone).collect(Collectors.toSet())));
        other.onlyOne.forEach(s -> onlyOne.add(s.stream().map(ParameterName::deepClone).collect(Collectors.toSet())));
        other.allOrNone.forEach(s -> allOrNone.add(s.stream().map(ParameterName::deepClone).collect(Collectors.toSet())));
        other.zeroOrOne.forEach(s -> zeroOrOne.add(s.stream().map(ParameterName::deepClone).collect(Collectors.toSet())));

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

    public String getSummary() {
        return summary;
    }

    public HashSet<Rule> getRulesToValidate() {
        return rulesToValidate;
    }

    public void applyRules(Collection<Rule> rules) {
        isReadOnly = false;
        rules.forEach(r -> r.apply(this));
        rules.clear();
        isReadOnly = true;
    }

    /**
     * Return all the parameters in the request that are not in the body.,
     * @return all the parameters in the request that are not in the body.,
     */
    public Collection<Parameter> getAllRequestParametersNotInBody() {
        Set<Parameter> parameters = new HashSet<>();

        for (Parameter element : headerParameters) {
            parameters.addAll(element.getAllParameters());
        }

        for (Parameter element : pathParameters) {
            parameters.addAll(element.getAllParameters());
        }

        for (Parameter element : queryParameters) {
            parameters.addAll(element.getAllParameters());
        }

        for (Parameter element : cookieParameters) {
            parameters.addAll(element.getAllParameters());
        }

        return parameters;
    }

    /**
     * Return all the parameter elements in the request.
     * @return all the parameter elements in the request.
     */
    public Collection<Parameter> getAllRequestParameters() {

        // Get all request parameters not in body
        Collection<Parameter> parameters = getAllRequestParametersNotInBody();

        // Add the parameters in the body, if any
        if (requestBody != null) {
            parameters.addAll(requestBody.getAllParameters());
        }

        return parameters;
    }

    public StructuredParameter getSuccessfulOutputParameters() {
        for (String key : outputParameters.keySet()) {
            if (key.startsWith("2")) {
                return outputParameters.get(key);
            }
        }
        return null;
    }

    public Set<Parameter> getHeaderParameters() {
        if (isReadOnly) {
            return Collections.unmodifiableSet(headerParameters);
        }
        return headerParameters;
    }

    public Set<Parameter> getPathParameters() {
        if (isReadOnly) {
            return Collections.unmodifiableSet(pathParameters);
        }
        return pathParameters;
    }

    public Set<Parameter> getQueryParameters() {
        if (isReadOnly) {
            return Collections.unmodifiableSet(queryParameters);
        }
        return queryParameters;
    }

    public Set<Parameter> getCookieParameters() {
        if (isReadOnly) {
            return Collections.unmodifiableSet(cookieParameters);
        }
        return cookieParameters;
    }

    public StructuredParameter getRequestBody() {
        return requestBody;
    }

    public String getRequestBodyDescription() {
        return requestBodyDescription;
    }

    public StructuredParameter getResponseBody() {
        return responseBody;
    }

    public void addRulesToValidate(HashSet<Rule> rulesToValidate) {
        this.rulesToValidate.addAll(rulesToValidate);
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

    public Collection<LeafParameter> getLeaves() {
        return getVisitResult(new LeavesVisitor());
    }

    public boolean addParameter(ParameterLocation location, Parameter element) {
        if (isReadOnly) {
            throw new EditReadOnlyOperationException(this);
        }

        if (element == null) {
            return false;
        }

        element.setOperation(this);
        element.setLocation(location);

        switch (location) {
            case PATH: return this.pathParameters.add(element);
            case HEADER: return this.headerParameters.add(element);
            case QUERY: return this.queryParameters.add(element);
            case COOKIE: return this.cookieParameters.add(element);
            default: return false;
        }
    }

    public boolean removeParameter(ParameterLocation location, Parameter element) {
        if (isReadOnly) {
            throw new EditReadOnlyOperationException(this);
        }

        if (element == null) {
            return false;
        }

        boolean removed = false;

        switch (location) {
            case PATH: removed = this.pathParameters.remove(element); break;
            case HEADER: removed = this.headerParameters.remove(element); break;
            case QUERY: removed = this.queryParameters.remove(element); break;
            case COOKIE: removed = this.cookieParameters.remove(element); break;
        }

        if (removed) {
            element.setOperation(null);
        }

        return removed;
    }

    public boolean containsParameter(ParameterLocation location, Parameter element) {
        if (element == null) {
            return false;
        }

        switch (location) {
            case PATH: return this.pathParameters.contains(element);
            case HEADER: return this.headerParameters.contains(element);
            case QUERY: return this.queryParameters.contains(element);
            case COOKIE: return this.cookieParameters.contains(element);
            default: return false;
        }
    }

    public void putOutputParameter(String code, StructuredParameter parameter) {
        if (parameter != null) {
            parameter.setOperation(this);
            parameter.setLocation(ParameterLocation.RESPONSE_BODY);
            outputParameters.put(code, parameter);
        }
    }

    /**
     * Returns the collection consisting of all the arrays in the parameters of the operation
     * @return the collection of arrays
     */
    public Collection<ArrayParameter> getArrays() {
        return getVisitResult(new ArraysVisitor());
    }

    public Collection<CombinedSchemaParameter> getCombinedSchemas() {
        return getVisitResult(new CombinedSchemasVisitor());
    }

    protected <T> Collection<T> getVisitResult(Visitor<Collection<T>> visitor) {
        Collection<T> result = new LinkedList<>();

        for (Parameter element : headerParameters) {
            result.addAll(element.accept(visitor));
        }

        for (Parameter element : pathParameters) {
            result.addAll(element.accept(visitor));
        }

        for (Parameter element : queryParameters) {
            result.addAll(element.accept(visitor));
        }

        for (Parameter element : cookieParameters) {
            result.addAll(element.accept(visitor));
        }

        if (requestBody != null) {
            result.addAll(requestBody.accept(visitor));
        }

        return result;
    }

    public void setHeaderParameters(Set<Parameter> headerParameters) {
        if (isReadOnly) {
            throw new EditReadOnlyOperationException(this);
        }

        this.headerParameters = headerParameters;

        if (this.headerParameters != null) {
            this.headerParameters.forEach(p -> p.setOperation(this));
        }
    }

    public void setPathParameters(Set<Parameter> pathParameters) {
        if (isReadOnly) {
            throw new EditReadOnlyOperationException(this);
        }

        this.pathParameters = pathParameters;

        if (this.pathParameters != null) {
            this.pathParameters.forEach(p -> p.setOperation(this));
        }
    }

    public void setQueryParameters(Set<Parameter> queryParameters) {
        if (isReadOnly) {
            throw new EditReadOnlyOperationException(this);
        }

        this.queryParameters = queryParameters;

        if (this.queryParameters != null) {
            this.queryParameters.forEach(p -> p.setOperation(this));
        }
    }

    public void setCookieParameters(Set<Parameter> cookieParameters) {
        if (isReadOnly) {
            throw new EditReadOnlyOperationException(this);
        }

        this.cookieParameters = cookieParameters;

        if (this.cookieParameters != null) {
            this.cookieParameters.forEach(p -> p.setOperation(this));
        }
    }

    public void setRequestBody(StructuredParameter requestBody) {
        if (isReadOnly) {
            throw new EditReadOnlyOperationException(this);
        }

        this.requestBody = requestBody;

        if (this.requestBody != null) {
            this.requestBody.setOperation(this);
            this.requestBody.setLocation(ParameterLocation.REQUEST_BODY);
        }
    }

    public void setResponseBody(StructuredParameter responseBody) {
        if (isReadOnly) {
            throw new EditReadOnlyOperationException(this);
        }

        this.responseBody = responseBody;

        if (this.responseBody != null) {
            this.responseBody.setOperation(this);
        }
    }

    // TODO: add getFuzzableParameterSet (exclude not fuzzable params, e.g., auth)?
    // FIXME: add combined parameter management
    // The commented part is the old implementation. Newer implementation should work better
    public List<LeafParameter> getReferenceLeaves() {

        List<LeafParameter> parameters = new LinkedList<>();

        pathParameters.forEach(p -> parameters.addAll(ParameterUtils.getReferenceLeaves(p)));
        headerParameters.forEach(p -> parameters.addAll(ParameterUtils.getReferenceLeaves(p)));
        queryParameters.forEach(p -> parameters.addAll(ParameterUtils.getReferenceLeaves(p)));
        cookieParameters.forEach(p -> parameters.addAll(ParameterUtils.getReferenceLeaves(p)));

        if (requestBody != null) {
            parameters.addAll(ParameterUtils.getReferenceLeaves(requestBody));
        }

        return parameters;
    }

    public Map<String, StructuredParameter> getOutputParameters() {
        if (isReadOnly) {
            return Collections.unmodifiableMap(outputParameters);
        }
        return outputParameters;
    }

    public Collection<Parameter> getFirstLevelRequestParametersNotInBody() {
        Collection<Parameter> parameters = new HashSet<>();
        parameters.addAll(headerParameters);
        parameters.addAll(pathParameters);
        parameters.addAll(queryParameters);
        parameters.addAll(cookieParameters);
        return parameters;
    }

    public Collection<Parameter> getFirstLevelRequestParameters() {
        Collection<Parameter> parameters = getFirstLevelRequestParametersNotInBody();
        parameters.addAll(getFirstLevelParameters(requestBody));
        return parameters;
    }

    public Set<Parameter> getFirstLevelOutputParameters() {
        Set<Parameter> firstLevelOutputParameters = new HashSet<>();
        outputParameters.values().forEach(p -> firstLevelOutputParameters.addAll(getFirstLevelParameters(p)));
        return firstLevelOutputParameters;
    }

    private Set<Parameter> getFirstLevelParameters(Parameter element) {
        Set<Parameter> firstLevelParameters = new HashSet<>();
        if (element instanceof ObjectParameter) {
            firstLevelParameters.addAll(((ObjectParameter) element).getProperties());
        } else if (element instanceof ArrayParameter) {
            firstLevelParameters.addAll(getFirstLevelParameters(((ArrayParameter) element).getReferenceElement()));
        } else if (element instanceof LeafParameter) {
            firstLevelParameters.add(element);
        }
        return firstLevelParameters;
    }

    public Set<Parameter> getOutputParametersSet() {
        Set<Parameter> outParams = new HashSet<>();
        for (StructuredParameter responseBody : outputParameters.values()) {
            outParams.addAll(ParameterUtils.getReferenceLeaves(responseBody));
        }
        return outParams;
    }

    public OperationSemantics getCrudSemantics() {
        return this.operationSemantics;
    }

    public void setCrudSemantics(OperationSemantics operationSemantics) {
        if (isReadOnly) {
            throw new EditReadOnlyOperationException(this);
        }

        this.operationSemantics = operationSemantics;
    }

    public String getCrudResourceType() {
        return this.crudResourceType;
    }

    public void setCrudResourceType(String crudResourceType) {
        if (isReadOnly) {
            throw new EditReadOnlyOperationException(this);
        }

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

    public List<Parameter> searchRequestParametersByName(ParameterName parameterName) {
        return getAllRequestParameters().stream().filter(p -> p.getName().equals(parameterName)).collect(Collectors.toList());
    }

    public List<Parameter> searchResponseParametersByName(ParameterName parameterName) {
        List<Parameter> foundParameters = new LinkedList<>();
        getOutputParametersSet().forEach(p -> {
            if (p.getName().equals(parameterName)) {
                foundParameters.add(p);
            }
        });
        return foundParameters;
    }

    // FIXME: check if all parameters can be used, rather than only reference leaves
    public List<Parameter> searchReferenceRequestParametersByNormalizedName(NormalizedParameterName normalizedParameterName) {
        return getReferenceLeaves().stream().filter(p -> p.getNormalizedName().equals(normalizedParameterName)).collect(Collectors.toList());
    }

    public List<Parameter> searchResponseParametersByNormalizedName(NormalizedParameterName normalizedParameterName) {
        List<Parameter> foundParameters = new LinkedList<>();
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

    /**
     * Returns a parameter by its REST path.
     * @param restPath the REST path of the wanted parameter.
     * @return the parameter corresponding to the provided REST path, or null if no parameter matches the REST path.
     */
    public Parameter getParameterByRestPath(String restPath) {
        return RestPathHelper.getParameterByRestPath(this, restPath);
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
