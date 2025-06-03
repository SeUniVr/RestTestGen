package io.resttestgen.core.openapi;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import io.resttestgen.boot.ApiUnderTest;
import io.resttestgen.core.datatype.HttpMethod;
import io.resttestgen.core.helper.ObjectHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jgrapht.alg.util.Pair;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@SuppressWarnings("unchecked")
public class OpenApiParser {

    private static final Logger logger = LogManager.getLogger(OpenApiParser.class);

    private static Map<String, Object> openApiMap;

    private static OpenApi openApi = new OpenApi();

    public static Map<String, Object> getOpenApiMap() {
        return openApiMap;
    }

    /**
     * The main function of the class that is used to parse the specification file of the API under test.
     * @return An OpenApi object that contains the parsed structure of the parsed OpenAPI specification.
     * @throws InvalidOpenApiException if the provided specification is invalid. Reason will be reported in the message.
     */
    public static OpenApi parse(ApiUnderTest apiUnderTest) throws InvalidOpenApiException {

        // Reset previous parsing data
        openApiMap = new HashMap<>();
        openApi = new OpenApi();

        // Parsing JSON document of the specification
        logger.debug("Parsing JSON document of the specification.");
        parseSpecificationAsJson(apiUnderTest);

        // Processing info
        logger.debug("Processing info.");
        processInfo();

        // Processing servers
        logger.debug("Processing servers.");
        processServers();

        // Adding name and references to components
        logger.debug("Adding name and references to components.");
        addNameAndRefToComponents();

        // Check if the specification contains at least one path
        if (!openApiMap.containsKey("paths")) {
            throw new InvalidOpenApiException("Missing 'paths' field in the OpenAPI specification. At least one path " +
                    "or operation in required in your API specification.");
        }

        // Expand all the refs (replace refs with their actual schema)
        logger.debug("Expanding references.");
        expandRefs(new LinkedList<>(), new HashSet<>(), (Map<String, Object>) openApiMap.get("paths"));

        // Unfold parameters shared by operations in the same path
        logger.debug("Unfolding parameters shared by operations in the same path.");
        unfoldPathSharedParameters();

        // Infer parameter types where missing
        logger.debug("Inferring parameter types where missing.");
        inferParameterTypes("paths", (Map<String, Object>) openApiMap.get("paths"));

        // Unfolding 'required' attributes
        logger.debug("Unfolding required attributes.");
        unfoldRequired((Map<String, Object>) openApiMap.get("paths"));

        // Instantiates Operation objects
        logger.debug("Instantiating Operation objects.");
        parseOperations();

        logger.info("OpenAPI specification parsed.");
        return openApi;
    }

    /**
     * Parses the JSON specification file into a map using the GSON library.
     * @param apiUnderTest the instance of API under test for which the specification has to be parsed.
     */
    private static void parseSpecificationAsJson(ApiUnderTest apiUnderTest) {

        Path specificationPath = Path.of(apiUnderTest.getComputedJsonSpecificationPath());

        // Check if the specification file exists
        if (!Files.exists(specificationPath)) {
            throw new InvalidOpenApiException("The OpenAPI specification file for " + apiUnderTest + " cannot be" +
                    "found. Did you place it the in the proper directory? More info in the README file.");
        }

        // Parse the OpenAPI specification JSON file
        Gson gson = new Gson();
        try {
            Reader reader = Files.newBufferedReader(specificationPath);
            openApiMap = gson.fromJson(reader, Map.class);
        } catch (Exception e) {
            logger.error(e);
            String message = "Error while parsing the OpenAPI specification as JSON. Please check " +
                    "that the provided specification is a valid JSON file.";
            OpenApiIssueWriter.writeIssue(message);
            throw new InvalidOpenApiException(message);
        }
    }

    /**
     * Processes the 'info' fields in the specification.
     */
    private static void processInfo() {
        Map<String, Object> infoMap = safeGet(openApiMap, "info", LinkedTreeMap.class);
        Map<String, Object> contactMap = safeGet(infoMap, "contact", LinkedTreeMap.class);
        Map<String, Object> licenseMap = safeGet(infoMap, "license", LinkedTreeMap.class);
        openApi.setTitle(safeGet(infoMap, "title", String.class));
        openApi.setSummary(safeGet(infoMap, "summary", String.class));
        openApi.setDescription(safeGet(infoMap, "description", String.class));
        openApi.setTermsOfService(safeGet(infoMap, "termsOfService", String.class));
        openApi.setContactName(safeGet(contactMap, "name", String.class));
        openApi.setContactUrl(safeGet(contactMap, "url", String.class));
        openApi.setContactEmail(safeGet(contactMap, "email", String.class));
        openApi.setLicenseName(safeGet(licenseMap, "name", String.class));
        openApi.setLicenseUrl(safeGet(licenseMap, "url", String.class));
        openApi.setVersion(safeGet(infoMap, "version", String.class));
    }

    /**
     * Processes the servers in the specification checking their validity.
     */
    private static void processServers() {

        // Check if the specification map has the 'server' property
        if (!openApiMap.containsKey("servers")) {
            throw new InvalidOpenApiException("Missing 'servers' field in the OpenAPI specification. This field is " +
                    "mandatory as it is used as target for the requests generated by RestTestGen.");
        }

        List<Map<String, Object>> servers;

        try {
            servers = (List<Map<String, Object>>) openApiMap.get("servers");
        } catch (ClassCastException e) {
            throw new InvalidOpenApiException("The provided list of servers is in an unsupported format.");
        }

        for (Map<String, Object> server : servers) {
            try {
                openApi.addServer(new URL((String) server.get("url")));
            } catch (MalformedURLException|ClassCastException e) {
                logger.warn(e);
            }
        }

        if (openApi.getServers().isEmpty()) {
            throw new InvalidOpenApiException("No valid server found in the OpenAPI specification.");
        }
    }

    /**
     * Add the name and the reference path to components of the specification using the extensions 'x-componentName'
     * and 'x-componentRef' for future use, e.g., when computing normalized parameter names.
     */
    private static void addNameAndRefToComponents() {
        if (openApiMap.containsKey("components")) {
            Map<String, Object> components = (Map<String, Object>) openApiMap.get("components");

            // Iterate on components' types
            for (String componentType : components.keySet()) {
                try {

                    // Get components of a given type
                    Map<String, Object> componentsOfType = (Map<String, Object>) components.get(componentType);

                    // Iterate on components of that type
                    for (String componentName : componentsOfType.keySet()) {
                        try {
                            Map<String, Object> component = (Map<String, Object>) componentsOfType.get(componentName);
                            component.put("x-componentName", componentName);
                            component.put("x-componentRef", "#/components/" + componentType + "/" + componentName);
                        } catch (ClassCastException e) {
                            String message = "Component " + componentName + " is declared in the wrong format.";
                            logger.warn(message);
                            OpenApiIssueWriter.writeIssue(message);
                        }
                    }
                } catch (ClassCastException e) {
                    String message = "Component type " + componentType + " is declared in the wrong format.";
                    logger.warn(message);
                    OpenApiIssueWriter.writeIssue(message);
                }
            }
        }
    }

    /**
     * Expands references in the specification by replacing '$ref' with the actual content of the reference. Expansion
     * is halted in the case of cyclic references, e.g., A -> B -> C -> A.
     * @param expansionPath the current expansion path, stored to stop expansion in case of cyclic references.
     * @param haltedExpansionPaths list of already halted expansion paths, to prevent duplicate warning messages.
     * @param map the map to process.
     */
    public static void expandRefs(LinkedList<String> expansionPath,
                                  HashSet<LinkedList<String>> haltedExpansionPaths,
                                  Map<String, Object> map) {

        // Check if the current map has a $ref key and expand (only it if no cycle is found)
        while (map.containsKey("$ref")) {

            // Check if the $ref property is a string, and in case expand it
            if (map.get("$ref") instanceof String) {
                String elementReference = (String) map.get("$ref");
                map.remove("$ref");
                if (!expansionPath.contains(elementReference)) {
                    Map<String, Object> element = getComponentCloneByRef(elementReference);
                    map.putAll(element);
                    expansionPath.add(elementReference);
                } else {
                    if (!haltedExpansionPaths.contains(expansionPath)) {
                        haltedExpansionPaths.add(expansionPath);
                        logger.warn("Found cyclic reference with expansion path {}. " +
                                "Halting expansion of {}.", expansionPath, elementReference);
                    }
                }
            } else {
                map.remove("$ref");
                String message = "Found invalid (non-string) $ref property. Ignoring.";
                logger.warn(message);
                OpenApiIssueWriter.writeIssue(message);
            }
        }

        // Propagate visit to children
        for (String key : map.keySet()) {
            Object value = map.get(key);
            if (value instanceof LinkedTreeMap) {
                LinkedList<String> updatedExpansionPath = new LinkedList<>(expansionPath);
                expandRefs(updatedExpansionPath, haltedExpansionPaths, (Map<String, Object>) value);
            } else if (value instanceof ArrayList) {
                LinkedList<String> updatedExpansionPath = new LinkedList<>(expansionPath);
                expandRefs(updatedExpansionPath, haltedExpansionPaths, (ArrayList<Object>) value);
            }
        }
    }

    /**
     * Propagates the expansion of references to all elements of a list.
     * @param expansionPath the current expansion path, stored to stop expansion in case of cyclic references.
     * @param haltedExpansionPaths list of already halted expansion paths, to prevent duplicate warning messages.
     * @param list the list to propagate.
     */
    public static void expandRefs(LinkedList<String> expansionPath,
                                  HashSet<LinkedList<String>> haltedExpansionPaths,
                                  ArrayList<Object> list) {
        for (Object element : list) {
            if (element instanceof LinkedTreeMap) {
                LinkedList<String> updatedExpansionPath = new LinkedList<>(expansionPath);
                expandRefs(updatedExpansionPath, haltedExpansionPaths, (Map<String, Object>) element);
            } else if (element instanceof ArrayList) {
                LinkedList<String> updatedExpansionPath = new LinkedList<>(expansionPath);
                expandRefs(updatedExpansionPath, haltedExpansionPaths, (ArrayList<Object>) element);
            }
        }
    }

    /**
     * When paths share common parameters (defined at path level), this method will copy parameters to each operation
     * under that path.
     */
    public static void unfoldPathSharedParameters() {
        Map<String, Object> paths = (Map<String, Object>) openApiMap.get("paths");

        // Iterate through the paths and look for 'parameters'
        for (String path : paths.keySet()) {
            Map<String, Object> pathItem = (Map<String, Object>) paths.get(path);

            // Retrieve parameters for the path (different from path parameters!) if present
            List<Map<String, Object>> parameters = (List<Map<String, Object>>) pathItem.get("parameters");
            pathItem.remove("parameters");

            // Skip to the next path if no parameters for the path are declared
            if (parameters == null || parameters.isEmpty()) {
                continue;
            }

            // Index parameters by name and location (this information is used to prevent overriding of parameters
            // declared for the operation with the parameters declared for the path)
            Map<Pair<String, String>, Map<String, Object>> parametersIndexMap = new HashMap<>();
            for (Map<String, Object> parameter : parameters) {
                parametersIndexMap.put(new Pair<>((String) parameter.get("name"), (String) parameter.get("in")), parameter);
            }

            // Iterate on operations of the path
            for (String httpMethod : pathItem.keySet()) {

                // Continue only if the string is an actual HTTP method and not 'parameters',
                // 'summary', or other supported keys
                if (!HttpMethod.isHttpMethod(httpMethod)) {
                    continue;
                }

                // Get the operation map
                Map<String, Object> operation = (Map<String, Object>) pathItem.get(httpMethod);

                // If there is no 'parameters' key in the operation, or if the list is empty, put the list in the map
                if (!operation.containsKey("parameters") || (operation.get("parameters") instanceof List && ((List<?>) operation.get("parameters")).isEmpty())) {
                    operation.put("parameters", ObjectHelper.deepCloneObject(parameters));
                    continue;
                }

                // If, instead, the operation has already a 'parameters' list, fill it with the parameters from the
                // path, unless parameters with the same name and location are already defined for the operation

                List<Map<String, Object>> operationParameters = (List<Map<String, Object>>) operation.get("parameters");

                Set<Pair<String, String>> operationParametersIndex = new HashSet<>();
                operationParameters.forEach(parameter ->
                        operationParametersIndex.add(new Pair<>((String) parameter.get("name"), (String) parameter.get("in")))
                );

                // Add each parameter of the path to the operation, unless already defined there
                for (Pair<String, String> parameter : parametersIndexMap.keySet()) {
                    if (!operationParametersIndex.contains(parameter)) {
                        operationParameters.add(ObjectHelper.deepCloneObject(parametersIndexMap.get(parameter)));
                    }
                }
            }
        }
    }

    /**
     * Infers parameter types based on other keys found in the map.
     * @param parentKey the name of the key of the parent object.
     * @param map the map to process.
     */
    public static void inferParameterTypes(String parentKey, Map<String, Object> map) {

        // Proceed only if the map has no 'type' key defined and if we are not in the 'properties' item, which can
        // contain keys that are user-defined and could match our checks
        if (!parentKey.equals("properties") && !map.containsKey("type")) {

            // An object will contain 'properties' and, possibly, a list of required properties
            if (map.containsKey("properties") || map.get("required") instanceof List) {
                map.put("type", "object");
            }

            // An array will describe the schema of the 'items'
            else if (map.containsKey("items")) {
                map.put("type", "array");
            }

            // A number can contain 'multipleOf', 'minimum', etc.
            else if (map.containsKey("multipleOf") || map.containsKey("maximum") ||
                    map.containsKey("exclusiveMaximum") || map.containsKey("minimum") ||
                    map.containsKey("exclusiveMinimum")) {
                map.put("type", "number");
            }

            // A string can contain 'maxLength', 'minLength', and 'pattern'
            else if (map.containsKey("maxLength") || map.containsKey("minLength") ||
                    map.containsKey("pattern")) {
                map.put("type", "string");
            }
        }

        // Propagate visit to children
        for (String key : map.keySet()) {
            Object value = map.get(key);
            if (value instanceof LinkedTreeMap) {
                inferParameterTypes(key, (Map<String, Object>) value);
            } else if (value instanceof ArrayList) {
                inferParameterTypes(key, (ArrayList<Object>) value);
            }
        }
    }

    /**
     * Propagates type inference to all elements of a list.
     * @param parentKey the name of the key of the parent object.
     * @param list the list to propagate.
     */
    public static void inferParameterTypes(String parentKey, ArrayList<Object> list) {

        // Propagate the visit to all the elements of the list
        for (Object element : list) {
            if (element instanceof LinkedTreeMap) {
                inferParameterTypes(parentKey, (Map<String, Object>) element);
            } else if (element instanceof ArrayList) {
                inferParameterTypes(parentKey, (ArrayList<Object>) element);
            }
        }
    }

    /**
     * Unfolds required attributes to individual parameters, i.e., when an Object Parameter has a list of required
     * properties, this method will move the required attribute within the individual properties.
     * Visit of children of the map is performed before unfolding to avoid unintentional overwriting of the values of
     * the 'required' key in the map.
     * @param map the map to analyze.
     */
    public static void unfoldRequired(Map<String, Object> map) {

        // Propagate the visit to all the elements of the map
        for (String key : map.keySet()) {
            Object value = map.get(key);
            if (value instanceof LinkedTreeMap) {
                unfoldRequired((LinkedTreeMap<String, Object>) value);
            } else if (value instanceof ArrayList) {
                unfoldRequired((ArrayList<Object>) value);
            }
        }

        // In objects, 'required' is a list of strings in this map. Unfold it to the object's properties
        // This is done after visiting the children, to prevent replacing the list of required fields with required=true
        if (map.containsKey("required") && map.get("required") instanceof List) {
            List<String> required = (List<String>) map.get("required");
            map.remove("required");
            for (String requiredItem : required) {
                Map<String, Object> properties = (Map<String, Object>) map.get("properties");

                // Check if the object parameter has properties declared
                if (properties == null) {
                    String message = "Found an object parameter that has no properties declared.";
                    logger.warn(message);
                    OpenApiIssueWriter.writeIssue(message);
                    continue;
                }


                Map<String, Object> requiredProperty = (Map<String, Object>) ((Map<String, Object>) map.get("properties")).get(requiredItem);
                if (requiredProperty == null) {
                    String message = "Property '" + requiredItem + "' is declared as required, but is not a property in the object.";
                    logger.warn(message);
                    OpenApiIssueWriter.writeIssue(message);
                    continue;
                }

                // TODO: do not overwrite if required is false!
                requiredProperty.put("required", true);
            }
        }

        // In requestBody, propagate the required=true to the root items of the body
        if (map.containsKey("requestBody")) {
            Map<String, Object> requestBodyMap = safeGet(map, "requestBody", LinkedTreeMap.class);

            if (requestBodyMap.containsKey("required") && requestBodyMap.get("required") instanceof Boolean &&
                    requestBodyMap.get("required").equals(true)) {
                Map<String, Object> contentMap = safeGet(requestBodyMap, "content", LinkedTreeMap.class);

                // Iterate con content types
                for (String contentType : contentMap.keySet()) {
                    Map<String, Object> contentTypeMap = safeGet(contentMap, contentType, LinkedTreeMap.class);
                    Map<String, Object> schemaMap = safeGet(contentTypeMap, "schema", LinkedTreeMap.class);
                    schemaMap.put("required", true);
                }
            }
        }
    }

    /**
     * Propagated the unfolding of required parameters to all elements of a list.
     * @param list the list to propagate.
     */
    public static void unfoldRequired(ArrayList<Object> list) {

        // Propagate the visit to all the elements of the list
        for (Object element : list) {
            if (element instanceof LinkedTreeMap) {
                unfoldRequired((Map<String, Object>) element);
            } else if (element instanceof ArrayList) {
                unfoldRequired((ArrayList<Object>) element);
            }
        }
    }

    /**
     * The operations in the OpenAPI map will be turned into instances of the Operation class.
     */
    public static void parseOperations() {

        // Read paths
        Map<String, Object> paths = (Map<String, Object>) openApiMap.get("paths");

        // Iterate on the paths
        for (String path : paths.keySet()) {

            // Skip paths that are OpenAPI extensions
            if (path.startsWith("x-")) {
                continue;
            }

            Map<String, Object> pathOperations = (Map<String, Object>) paths.get(path);

            // Fetch operations
            for (String operation : pathOperations.keySet()) {

                if (!HttpMethod.isHttpMethod(operation)) {
                    continue;
                }

                Operation o = new Operation(path, HttpMethod.getMethod(operation), (Map<String, Object>) pathOperations.get(operation));
                o.setReadOnly();
                openApi.addOperation(o);
            }
        }
    }

    /**
     * The function returns a deep clone of the component referenced by the input string.
     * The choice of the deep clone is to avoid unintentional modifications on the original object
     * @param ref path of the referenced resource
     * @return Deep clone of the referenced object
     */
    private static Map<String, Object> getComponentCloneByRef(String ref) {
        Map<String, Object> map = openApiMap;
        String[] componentPath = ref.split("/");

        // Skip the first element, since it is a '#'
        for (int i = 1; i < componentPath.length; ++i) {
            map = (Map<String, Object>) map.get(componentPath[i]);
            if (map == null) {
                String message = "Component " + ref + " not found. Cannot expand the reference.";
                logger.warn(message);
                OpenApiIssueWriter.writeIssue(message);
                return new HashMap<>();
            }
        }

        // Copy object instead of put it directly to avoid unexpected behaviors
        // Used to avoid unintentional modifications to the map passed as parameter
        return ObjectHelper.deepCloneObject(map);
    }

    // TODO: move to ObjectHelper?
    // TODO: investigate if we can avoid taking a class as parameter
    /*
    Actually it is needed since T is always an abstract superclass or an interface (such as Set, List, ..).
    - We can avoid it using always Ts that are standard classes, but we could have to use the same classes of the map object
      (can be MAYBE avoided if T is a collection, since we could pass another collection to the constructor)
    - We can avoid it using the runtime type of the map object
    - ...?
     */
    public static <T, E extends T> E safeGet(Map<String, Object> map, String key, Class<E> type) throws CannotParseMapException {
        try {
            T res = (res = (T) map.get(key)) != null ? res : type.getDeclaredConstructor().newInstance();
            return type.cast(res);
        } catch (InstantiationException|IllegalAccessException|InvocationTargetException|NoSuchMethodException e) {
            throw new CannotParseMapException();
        }
    }

}
