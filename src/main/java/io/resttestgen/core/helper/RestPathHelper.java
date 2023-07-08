package io.resttestgen.core.helper;

import io.resttestgen.core.datatype.ParameterName;
import io.resttestgen.core.datatype.parameter.*;
import io.resttestgen.core.datatype.parameter.attributes.ParameterLocation;
import io.resttestgen.core.datatype.parameter.structured.ArrayParameter;
import io.resttestgen.core.datatype.parameter.structured.ObjectParameter;
import io.resttestgen.core.datatype.parameter.structured.StructuredParameter;
import io.resttestgen.core.openapi.Operation;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;

/**
 * Class to manage REST paths, a way to uniquely identify parameters in REST requests and responses.
 * TODO: put examples of REST paths here
 */
public class RestPathHelper {

    private static final String request = "$[\"request\"]";
    private static final String response = "$[\"response\"]";
    private static final String header = "[\"header\"]";
    private static final String path = "[\"path\"]";
    private static final String query = "[\"query\"]";
    private static final String cookie = "[\"cookie\"]";
    private static final String body = "[\"body\"]";
    private static final String actual = "[\"actual\"]";

    /**
     * Computes the REST path of a parameter element.
     * @param parameter the subject parameter element.
     * @return the computed REST path as string.
     */
    public static String getRestPath(Parameter parameter) {

        // In case this is a root element
        if (parameter.getParent() == null) {

            // REST path will start with the prefix $ and the location of the parameter (request/response + header/query/path/etc.)
            StringBuilder restPath = new StringBuilder(getRestPathPrefixForRootElement(parameter));

            // Parameter name is added to REST path in case the parameter is not the root element of request or response body
            if (parameter.getLocation() != ParameterLocation.REQUEST_BODY && parameter.getLocation() != ParameterLocation.RESPONSE_BODY) {
                restPath.append("[\"").append(parameter.getName()).append("\"]");
            }

            return restPath.toString();
        }

        // In case it is an array element
        else if (ParameterUtils.isArrayElement(parameter)) {
            return parameter.getParent().getRestPath() + "[" + ((ArrayParameter) parameter.getParent()).getElements().indexOf(parameter) + "]";
        }

        // In case it is an array reference element
        else if (ParameterUtils.isReferenceElement(parameter)) {
            return parameter.getParent().getRestPath() + "[-1]";
        }

        // In all other cases
        return parameter.getParent().getRestPath() + "[\"" + parameter.getName() + "\"]";
    }

    /**
     * Retrieves a Parameter from the provided Operation based in the parameter's REST path.
     * @param operation the Operation in which the parameter is searched.
     * @param restPath the REST path of the desired parameter.
     * @return the identified parameter, or null if no parameter matches the provided REST path.
     */
    public static Parameter getParameterByRestPath(Operation operation, String restPath) {

        String editableRestPath = restPath;

        // If the parameter is in the request
        if (editableRestPath.startsWith(request)) {
            editableRestPath = editableRestPath.substring(request.length());

            if (editableRestPath.startsWith(header)) {
                editableRestPath = editableRestPath.substring(header.length());
                return findParameterByRestPathInCollection(operation.getHeaderParameters(), editableRestPath);
            } else if (editableRestPath.startsWith(path)) {
                editableRestPath = editableRestPath.substring(path.length());
                return findParameterByRestPathInCollection(operation.getPathParameters(), editableRestPath);
            } else if (editableRestPath.startsWith(query)) {
                editableRestPath = editableRestPath.substring(query.length());
                return findParameterByRestPathInCollection(operation.getQueryParameters(), editableRestPath);
            } else if (editableRestPath.startsWith(cookie)) {
                editableRestPath = editableRestPath.substring(cookie.length());
                return findParameterByRestPathInCollection(operation.getCookieParameters(), editableRestPath);
            } else if (editableRestPath.startsWith(body)) {
                editableRestPath = editableRestPath.substring(body.length());
                return operation.getRequestBody().getParameterByRestPath(editableRestPath);
            }
        }

        // If the parameter is in the response, or in response schemas
        else if (editableRestPath.startsWith(response)) {
            editableRestPath = editableRestPath.substring(response.length());

            // If the parameter is in the observed (actual) response body
            if (editableRestPath.startsWith(actual)) {
                editableRestPath = editableRestPath.substring(actual.length());

                if (editableRestPath.startsWith(body)) {
                    editableRestPath = removeFirstEncapsulatedData(editableRestPath);
                    return operation.getRequestBody().getParameterByRestPath(editableRestPath);
                }
            }

            // If the parameter is in the response schemas
            else {
                StructuredParameter schema = operation.getOutputParameters().get(extractFirstEncapsulatedData(editableRestPath));
                editableRestPath = removeFirstEncapsulatedData(removeFirstEncapsulatedData(editableRestPath));
                if (schema != null) {
                    return schema.getParameterByRestPath(editableRestPath);
                }
            }
            return null;
        }

        return null;
    }

    public static Parameter getParameterByRestPath(Parameter parameter, String restPath) {

        // If the current REST path starts with $ (root), the process is like an operation REST path
        if (restPath.startsWith("$")) {
            return getParameterByRestPath(parameter.getOperation(), restPath);
        }

        String editableRestPath = restPath;

        // Check the name of the parameter, unless root element in body, or array element (or reference element) in body
        if (!ParameterUtils.isArrayElement(parameter) && !ParameterUtils.isReferenceElement(parameter) &&
                !((parameter.getLocation() == ParameterLocation.REQUEST_BODY || parameter.getLocation() == ParameterLocation.RESPONSE_BODY) && parameter.getParent() == null)) {

            // Return null if the parameter name does not match the REST path
            if (!parameter.getName().equals(new ParameterName(extractFirstEncapsulatedData(editableRestPath)))) {
                return null;
            }

            editableRestPath = removeFirstEncapsulatedData(editableRestPath);
        }

        if (editableRestPath.length() < 1) {
            return parameter;
        } else {
            if (ParameterUtils.isObject(parameter)) {
                for (Parameter property : ((ObjectParameter) parameter).getProperties()) {
                    if (property.getName().equals(new ParameterName(extractFirstEncapsulatedData(editableRestPath)))) {
                        return property.getParameterByRestPath(editableRestPath);
                    }
                }
            } else if (ParameterUtils.isArray(parameter)) {
                ArrayParameter array = (ArrayParameter) parameter;
                int index;
                try {
                    index = Integer.parseInt(extractFirstEncapsulatedData(editableRestPath));
                    editableRestPath = removeFirstEncapsulatedData(editableRestPath);
                    if (index == -1) {
                        return array.getReferenceElement().getParameterByRestPath(editableRestPath);
                    } else if (index < array.size()) {
                        return array.getElements().get(index).getParameterByRestPath(editableRestPath);
                    }
                } catch (ClassCastException e) {
                    return null;
                }
            }
        }

        return null;
    }

    private static String getRestPathPrefixForRootElement(Parameter parameter) {

        // Proceed only in case of a root element
        if (parameter.getParent() == null) {
            switch (parameter.getLocation()) {
                case PATH:
                    return request + path;
                case QUERY:
                    return request + query;
                case COOKIE:
                    return request + cookie;
                case HEADER:
                    return request + header;
                case REQUEST_BODY:
                    return request + body;
                case RESPONSE_BODY:
                    if (parameter == parameter.getOperation().getResponseBody()) {
                        return response + actual + body;
                    }
                    for (Map.Entry<String, StructuredParameter> entry : parameter.getOperation().getOutputParameters().entrySet()) {
                        if (entry.getValue().equals(parameter)) {
                            return response + encapsulate(entry.getKey())  + body;
                        }
                    }
            }
        }
        
        return "";
    }

    private static String encapsulate(String string) {
        return "[\"" + string + "\"]";
    }

    /**
     * Given a string, extract the first encapsulated data (in square brackets and quotes).
     * @param string the source string.
     * @return the de-encapsulated data.
     */
    private static String extractFirstEncapsulatedData(String string) {

        // Location of start and end of encapsulation (square brackets)
        int start = string.indexOf('[');
        int end = string.indexOf(']');

        // If data is not encapsulated
        if (start == -1 || end < 3 || start >= end) {
            return "";
        }

        String result = string.substring(start + 1, end);

        // If data is also encapsulated in quotes (single or double)
        if ((result.charAt(0) == '"' && result.charAt(result.length() - 1) == '"') ||
                (result.charAt(0) == '\'' && result.charAt(result.length() - 1) == '\'')) {
            result = result.substring(1, result.length() - 1);
        }

        return result;
    }

    private static String removeFirstEncapsulatedData(String string) {
        int start = string.indexOf(']');
        if (start >= 0) {
            return string.substring(start + 1);
        }
        return string;
    }

    private static Parameter findParameterByRestPathInCollection(Collection<Parameter> parameters, String restPath) {
        for (Parameter parameter : parameters) {
            Parameter result = parameter.getParameterByRestPath(restPath);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    /**
     * Given a collection of parameters, returns the first parameter matching the provided name.
     * @param parameters collection of parameters.
     * @param name parameter name to match.
     * @return the matching Parameter.
     */
    private static Parameter findParameterByName(Collection<Parameter> parameters, ParameterName name) {
        for (Parameter parameter : parameters) {
            if (parameter.getName().equals(name)) {
                return parameter;
            }
        }
        return null;
    }

    private static String getLocationData(@NotNull Parameter parameter) {
        switch (parameter.getLocation()) {
            case HEADER:
                return header;
            case PATH:
                return path;
            case COOKIE:
                return cookie;
            case QUERY:
                return query;
            case RESPONSE_BODY:
            case REQUEST_BODY:
                return body;
            default:
                return "";
        }
    }
}
