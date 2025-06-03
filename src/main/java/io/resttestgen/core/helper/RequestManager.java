package io.resttestgen.core.helper;

import io.resttestgen.boot.AuthenticationInfo;
import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.HttpMethod;
import io.resttestgen.core.datatype.ParameterName;
import io.resttestgen.core.datatype.parameter.Parameter;
import io.resttestgen.core.datatype.parameter.ParameterUtils;
import io.resttestgen.core.datatype.parameter.attributes.ParameterLocation;
import io.resttestgen.core.datatype.parameter.combined.CombinedSchemaParameter;
import io.resttestgen.core.datatype.parameter.leaves.LeafParameter;
import io.resttestgen.core.datatype.parameter.leaves.NullParameter;
import io.resttestgen.core.datatype.parameter.leaves.StringParameter;
import io.resttestgen.core.datatype.parameter.structured.ArrayParameter;
import io.resttestgen.core.datatype.parameter.structured.ObjectParameter;
import io.resttestgen.core.datatype.parameter.structured.StructuredParameter;
import io.resttestgen.core.openapi.Operation;
import okhttp3.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class RequestManager {

    private final Operation source;
    private final Operation operation;

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    public static final MediaType FORM = MediaType.parse("application/x-www-form-urlencoded");

    private static final Logger logger = LogManager.getLogger(RequestManager.class);

    private AuthenticationInfo authenticationInfo = Environment.getInstance().getApiUnderTest().getDefaultAuthenticationInfo();
    public RequestManager(Operation operation) {
        this.source = operation;
        this.operation = operation.deepClone();
    }

    /**
     * Builds the request including the authorization defined in the configuration file. If that field has already been
     * initialized in the operation, it will be overridden. This method guarantees authorization as long as the
     * authorization script used in the configuration returns a valid authorization.
     * @return an okhttp Request with config-defined authorization
     */
    public Request buildRequest() {
        return requestBuilder(null, false, false);
    }

    /**
     * Builds the request substituting the authorization token defined in the configuration file with the given
     * parameter. If no auth is defined within the configuration the parameter is ignored.
     * @param authToken Custom value for the authorization token
     * @return an okhttp Request with a custom authorization token
     */
    public Request buildRequest(String authToken) {
        return requestBuilder(authToken, false, false);
    }

    /**
     * Builds the request removing, if any, the authorization defined in the configuration file. Hence, if in the
     * operation has been defined a field compliant with the defined authorization, that field will be dropped.
     * @return an okhttp Request with no authorization
     */
    public Request buildRequestDroppingAuth() {
        return requestBuilder(null, true, false);
    }

    /**
     * Builds the request applying no modification (no auth added or removed).
     * @return an okhttp Request crafted using only the operation
     */
    public Request buildRequestAsFuzzed() {
        return requestBuilder(null, false, true);
    }

    /**
     * Function that implements the Request building following the 4 different behaviors described in
     * 'buildRequestAsFuzzed', 'buildRequestDroppingAuth', 'buildRequest(authToken)', 'buildRequest'.
     * Each of the 3 parameters taken in input is exclusive. This means that only one of them can have a valid value.
     * @param token Custom token for auth. If null, is ignored.
     * @param dropAuth Boolean used to select if the auth should be dropped.
     * @param asFuzzed Boolean used to select if build the request strictly as defined in the operation.
     * @return the built request.
     */
    private Request requestBuilder(String token, boolean dropAuth, boolean asFuzzed) {
        logger.debug("Building request for operation {}", operation);

        // To avoid OkHttp crashes, remove parameters with null values
        removeUninitializedParameters();

        // URL (basepath) from specification
        URL server = Environment.getInstance().getOpenAPI().getDefaultServer();

        // If another host is manually provided
        String host = Environment.getInstance().getApiUnderTest().getHost();
        if (host != null && host.length() > 1) {
            try {
                URL tempServer = new URL(Environment.getInstance().getApiUnderTest().getHost());
                server = new URL(tempServer.getProtocol(), tempServer.getHost(), tempServer.getPort(), server.getFile());
            } catch (MalformedURLException e) {
                logger.warn("The manually specified host ({}) is invalid. Using the OpenAPI specification provided host.", host);
            }
        }
        HttpUrl.Builder httpBuilder = new HttpUrl.Builder();
        Request.Builder requestBuilder = new Request.Builder();

        for (String segment : server.getPath().split("/")) {
            httpBuilder.addPathSegment(segment);
        }

        // Extract endpoint and substitute path parameters
        String endpoint = operation.getEndpoint().replaceFirst("^/", "");

        Set<Parameter> pathParameters = operation.getPathParameters();
        Matcher matcher = Pattern.compile("\\{[^{}]*\\}").matcher(endpoint);
        while (matcher.find()) {
            ParameterName parameterName = new ParameterName(matcher.group().substring(1, matcher.group().length() - 1));

            Optional<Parameter> pathParameter =
                    pathParameters.stream().filter(p -> p.getName().equals(parameterName)).findFirst();

            if (pathParameter.isPresent()) {
                try {
                    endpoint = endpoint.replaceFirst("\\{" + parameterName + "\\}",
                            pathParameter.get().getValueAsFormattedString());
                } catch (Exception e) {
                    logger.warn("Could not apply path parameter. Could not find parameter with matching name.");
                }
            }
        }

        httpBuilder.addPathSegments(endpoint);
        httpBuilder.scheme(server.getProtocol());
        httpBuilder.host(server.getHost());
        if (server.getPort() != -1) {
            httpBuilder.port(server.getPort());
        }

        // Put query parameters in a map before adding them to the request. Need to check for auth before adding
        Map<String, String> queryParametersMap = new HashMap<>();
        operation.getQueryParameters().forEach(
                p -> queryParametersMap.put(p.getName().toString(), p.getValueAsFormattedString())
        );

        // TODO: add cookie parameters support

        RequestBody requestBody;
        if (operation.getMethod().equals(HttpMethod.POST) || operation.getMethod().equals(HttpMethod.PUT) ||
                operation.getMethod().equals(HttpMethod.PATCH)) {
            if (operation.getRequestBody() != null) {
                if (operation.getRequestContentType().contains("application/x-www-form-urlencoded") &&
                        operation.getRequestBody() instanceof ObjectParameter) {
                    Map<String, String> formParametersMap = new HashMap<>();
                    ((ObjectParameter) operation.getRequestBody()).getProperties().forEach(
                            p -> formParametersMap.put(p.getName().toString(), p.getValueAsFormattedString())
                    );
                    StringJoiner formUrlEncoded = new StringJoiner("&");
                    formParametersMap.values().forEach(formUrlEncoded::add);
                    requestBody = RequestBody.create(formUrlEncoded.toString(), FORM);
                } else {
                    requestBody = RequestBody.create(operation.getRequestBody().getJSONString(), JSON);
                }
            } else {
                requestBody = RequestBody.create(new byte[0], null);
            }
        } else {
            requestBody = null;
        }

        setMethod(requestBuilder, operation.getMethod(), requestBody);
        // By default, accept application/json. If differently defined (in the specification) it will be overridden
        requestBuilder.header("Accept", "application/json");

        // Add header parameters with encoded values but remove chars that are unsupporter in HTTP headers
        operation.getHeaderParameters().forEach(p ->
                requestBuilder.header(p.getName().toString(),
                        p.getValueAsFormattedString().replaceAll("[^\\x20-\\x7E]", "")));

        // Apply authorization
        if (authenticationInfo != null) {

            authenticationInfo.authenticateIfNot();

            if (authenticationInfo.isAuthenticated()) {

                if (!asFuzzed) {

                    if (dropAuth) {
                        switch (authenticationInfo.getIn()) {
                            case HEADER:
                                requestBuilder.removeHeader(authenticationInfo.getParameterName().toString());
                                break;
                            case QUERY:
                                queryParametersMap.remove(authenticationInfo.getParameterName().toString());
                                break;
                            case COOKIE:
                                logger.warn("Cookie parameters are not already supported.");
                                break;
                        }
                    } else {
                        String authToken = authenticationInfo.getValue();
                        if (token != null) {
                            authToken = token;
                        }
                        switch (authenticationInfo.getIn()) {
                            case HEADER:
                                requestBuilder.header(authenticationInfo.getParameterName().toString(), authToken);
                                break;
                            case QUERY:
                                queryParametersMap.put(authenticationInfo.getParameterName().toString(), authenticationInfo.getParameterName().toString() + "=" + authToken);
                                break;
                            case COOKIE:
                                logger.warn("Cookie parameters are not supported.");
                                break;
                        }
                    }
                }
            }
        }

        // Add query parameters
        if (!queryParametersMap.isEmpty()) {
            StringJoiner queryString = new StringJoiner("&");
            queryParametersMap.values().forEach(queryString::add);
            httpBuilder.query(queryString.toString());
        }

        requestBuilder.url(httpBuilder.build());
        return requestBuilder.build();
    }

    /**
     * Adds the given parameter to the operation. If another parameter that results equals to it is already defined,
     * it will be substituted.
     * @param parameter the parameter to be added.
     */
    public void addParameter(Parameter parameter) {
        switch (parameter.getLocation()) {

            case HEADER:
                this.operation.getHeaderParameters().add(parameter);
                break;
            case PATH:
                this.operation.getPathParameters().add(parameter);
                break;
            case QUERY:
                this.operation.getQueryParameters().add(parameter);
                break;
            case COOKIE:
                this.operation.getCookieParameters().add(parameter);
                break;
            case REQUEST_BODY:
                if (!(parameter instanceof StructuredParameter)) {
                    logger.error("Cannot cast parameter '{}' to a structured parameter in '{}' request body.",
                            parameter.getName(), this.operation);
                } else {
                    this.operation.setRequestBody((StructuredParameter) parameter);
                }
                break;
            case RESPONSE_BODY:
                logger.error("Cannot add parameter '{}' to operation '{}since it is declared as a response body.",
                        parameter.getName(), this.operation);
                break;
            case MISSING:
            case UNKNOWN:
                logger.error("Cannot add parameter '{}' to operation '{}since its location (' {}') is not valid.",
                        parameter.getName(), this.operation, parameter.getLocation());
                break;
        }
    }

    public void removeParameter(Parameter parameter) {
        switch (parameter.getLocation()) {

            case HEADER:
                this.operation.getHeaderParameters().remove(parameter);
                break;
            case PATH:
                this.operation.getPathParameters().remove(parameter);
                break;
            case QUERY:
                this.operation.getQueryParameters().remove(parameter);
                break;
            case COOKIE:
                this.operation.getCookieParameters().remove(parameter);
                break;
            case REQUEST_BODY:
                this.operation.setRequestBody(null);
                break;
            case RESPONSE_BODY:
                logger.error("Cannot remove response body in operation '{}'.", this.operation);
                break;
            case MISSING:
            case UNKNOWN:
                logger.error("Cannot remove response body in operation '{}' since location '{}' is not valid.", this.operation, parameter.getLocation());
                break;
        }
    }

    public void setNullParameter(Parameter parameter) {
        if (parameter.getLocation() == ParameterLocation.REQUEST_BODY) {
            removeParameter(parameter);
            return;
        }
        NullParameter nullParameter = new NullParameter(parameter);
        removeParameter(parameter);
        addParameter(nullParameter);
    }

    public void removeUninitializedParameters() {
        Set<Parameter> newPathParameters = new HashSet<>(this.operation.getPathParameters());
        this.operation.getPathParameters().stream().filter(p -> p.getValue() == null).forEach(p -> {
            /*logger.warn("Empty-valued path parameter '" + p.getName() + "' found in operation '" + this.operation
                    + "'. It will be valued with its name.");*/
            newPathParameters.remove(p);
            StringParameter sp = new StringParameter(p);
            sp.setValueManually(sp.getName().toString());
            newPathParameters.add(sp);
        });
        this.operation.setPathParameters(newPathParameters);

        this.operation.setHeaderParameters(
                cleanupEmptyValuedParametersFromList(this.operation.getHeaderParameters())
        );
        this.operation.setQueryParameters(
                cleanupEmptyValuedParametersFromList(this.operation.getQueryParameters())
        );
        this.operation.setCookieParameters(
                cleanupEmptyValuedParametersFromList(this.operation.getCookieParameters())
        );

        if (operation.getRequestBody() != null) {
            operation.getRequestBody().removeUninitializedParameters();
        }
    }

    public Operation getOperation() {
        return operation;
    }

    public Collection<LeafParameter> getLeaves() {
        return this.operation.getLeaves();
    }

    public Collection<ArrayParameter> getArrays() {
        return this.operation.getArrays();
    }

    public Collection<CombinedSchemaParameter> getCombinedSchemas() {
        return this.operation.getCombinedSchemas();
    }

    public boolean isWellFormed() {
        // Path parameters are all required
        if (!source.getPathParameters().equals(operation.getPathParameters())) {
            return false;
        }

        Set<Parameter> sourceHeaderRequired =
                source.getHeaderParameters().stream().filter(Parameter::isRequired).collect(Collectors.toSet());
        if (!operation.getHeaderParameters().containsAll(sourceHeaderRequired)) {
            return false;
        }

        Set<Parameter> sourceQueryRequired =
                source.getQueryParameters().stream().filter(Parameter::isRequired).collect(Collectors.toSet());
        if (!operation.getQueryParameters().containsAll(sourceQueryRequired)) {
            return false;
        }

        Set<Parameter> sourceCookieRequired =
                source.getCookieParameters().stream().filter(Parameter::isRequired).collect(Collectors.toSet());
        if (!operation.getCookieParameters().containsAll(sourceCookieRequired)) {
            return false;
        }

        if (source.getRequestBody() != null) {
            Set<LeafParameter> leavesRequired = ParameterUtils.getLeaves(source.getRequestBody()).stream()
                    .filter(Parameter::isRequired)
                    .collect(Collectors.toSet());
            return (leavesRequired.isEmpty() || operation.getRequestBody() != null) &&
                    ParameterUtils.getLeaves(operation.getRequestBody()).containsAll(leavesRequired);
        }

        return true;
    }

    private static void setMethod(Request.Builder builder, HttpMethod method, RequestBody body) {
        switch (method) {
            case GET:
                builder.get();
                break;
            case PUT:
                builder.put(body);
                break;
            case HEAD:
                builder.head();
                break;
            case POST:
                builder.post(body);
                break;
            case PATCH:
                builder.patch(body);
                break;
            case DELETE:
                builder.delete();
                break;
        }
    }

    private static Set<Parameter> cleanupEmptyValuedParametersFromList(Set<Parameter> elements) {
        Set<Parameter> newElements = new HashSet<>(elements);

        elements.forEach(e -> {
            if (LeafParameter.class.isAssignableFrom(e.getClass())) {
                if (e.getValue() == null) {
                    //logger.warn("Empty valued parameter '" + e.getName() + "' found. It will be removed.");
                    newElements.remove(e);
                }
            } else if (StructuredParameter.class.isAssignableFrom(e.getClass())) {
                StructuredParameter structuredE = (StructuredParameter) e;
                structuredE.removeUninitializedParameters();

                if (structuredE.isEmpty() && !structuredE.isKeepIfEmpty()) {
                    //logger.warn("Empty valued parameter '" + e.getName() + "' found. It will be removed.");
                    newElements.remove(e);
                }
            }
        });

        return newElements;
    }

    public void setAuthenticationInfo(AuthenticationInfo authenticationInfo) {
        this.authenticationInfo = authenticationInfo;
    }
}
