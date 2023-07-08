package io.resttestgen.core.openapi;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import io.resttestgen.boot.ApiUnderTest;
import io.resttestgen.core.datatype.HttpMethod;
import io.resttestgen.core.datatype.parameter.attributes.ParameterType;
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

    private static final String INVALID_SPEC = "Failed to load spec at '%s'";
    private static final Logger logger = LogManager.getLogger(OpenApiParser.class);

    private final Map<String, Object> openAPIMap; // Map parsed by GSON

    /**
     * Constructor for creating an OpenAPI parser given an OpenAPI specification
     * @param filePath Path to the OpenAPI specification file. If the given path does not exist throws a
     * CannotParseOpenAPIException.
     */
    @Deprecated
    public OpenApiParser(Path filePath) throws CannotParseOpenApiException {
        if (filePath == null || !Files.exists(filePath)) {
            throw new CannotParseOpenApiException(INVALID_SPEC);
        }

        Gson gson = new Gson();
        try {
            Reader reader = Files.newBufferedReader(filePath);
            this.openAPIMap = gson.fromJson(reader, Map.class);
        } catch (Exception e) {
            logger.error(e);
            throw new CannotParseOpenApiException();
        }

        logger.info("OpenAPI specification loaded correctly.");
    }

    public OpenApiParser(ApiUnderTest apiUnderTest) throws CannotParseOpenApiException {
        Path specificationPath = Path.of(apiUnderTest.getComputedJsonSpecificationPath());

        if (!Files.exists(specificationPath)) {
            throw new CannotParseOpenApiException(INVALID_SPEC);
        }

        Gson gson = new Gson();
        try {
            Reader reader = Files.newBufferedReader(specificationPath);
            this.openAPIMap = gson.fromJson(reader, Map.class);
        } catch (Exception e) {
            logger.error(e);
            throw new CannotParseOpenApiException();
        }

        logger.info("OpenAPI specification loaded correctly.");
    }

    /**
     * Main function of the class that is used to parse the given specification file.
     * This method resolves the '$ref' attributes within the specification, adds an extension field 'x-schemaName' to
     * each resolved field and normalizes parameters defined at path level adding them to every path item.
     * As a result, an OpenAPI object is instantiated.
     * @return An OpenAPI object that contains the parsed structure of the parser OpenAPI specification.
     * @throws InvalidOpenApiException if the provided specification is invalid.
     */
    public OpenApi parse() throws InvalidOpenApiException {

        // The specification is invalid in case it does not contain servers or path properties
        if (!this.openAPIMap.containsKey("servers")) {
            throw new InvalidOpenApiException("Missing 'servers' field.");
        }
        if (!this.openAPIMap.containsKey("paths")) {
            throw new InvalidOpenApiException("Missing 'paths' field.");
        }

        /*
         * Add extension with schema names to enrich the specification and keep track of the specification fields
         * that have exactly the same schema
         */
        addSchemasNames();
        // Solve all the refs normalizing (replace refs with their actual schema)
        solveOpenAPIrefs();
        // Normalize common parameters
        normalizeCommonParameters();
        // Infer parameter type where missing
        inferParameterTypes();
        // Normalize 'required' attribute in request/response bodies
        unfoldRequiredAttributes();

        // Start parsing specification fields
        OpenApi openAPI = new OpenApi();

        // Read servers
        List<Map<String, Object>> servers = (List<Map<String, Object>>) this.openAPIMap.get("servers");
        servers.forEach(server -> {
            try {
                openAPI.addServer(new URL((String) server.get("url")));
            } catch (MalformedURLException|ClassCastException e) {
                logger.error(e);
            }
        });
        if (openAPI.getServers().isEmpty()) {
            throw new InvalidOpenApiException("No valid server found within the OpenAPI specification.");
        }

        // Read paths and create operations
        Map<String, Object> paths = (Map<String, Object>) this.openAPIMap.get("paths");

        // Fetch paths
        for (Map.Entry<String, Object> path : paths.entrySet()) {

            if (path.getKey().startsWith("x-")) {
                continue;
            }

            // Fetch operations
            for (Map.Entry<String, Object> operation : ((Map<String, Object>) path.getValue()).entrySet()) {

                if (operation.getKey().startsWith("x-")) {
                    continue;
                }

                Operation o = new Operation(path.getKey(),
                        HttpMethod.getMethod(operation.getKey()),
                        (Map<String, Object>) operation.getValue());
                o.setReadOnly();
                openAPI.addOperation(o);
            }
        }

        // Finally, parse specification information
        Map<String, Object> infoMap = safeGet(this.openAPIMap, "info", LinkedTreeMap.class);
        Map<String, Object> contactMap = safeGet(infoMap, "contact", LinkedTreeMap.class);
        Map<String, Object> licenseMap = safeGet(infoMap, "license", LinkedTreeMap.class);
        openAPI.setTitle(safeGet(infoMap, "title", String.class));
        openAPI.setSummary(safeGet(infoMap, "summary", String.class));
        openAPI.setDescription(safeGet(infoMap, "description", String.class));
        openAPI.setTermsOfService(safeGet(infoMap, "termsOfService", String.class));
        openAPI.setContactName(safeGet(contactMap, "name", String.class));
        openAPI.setContactUrl(safeGet(contactMap, "url", String.class));
        openAPI.setContactEmail(safeGet(contactMap, "email", String.class));
        openAPI.setLicenseName(safeGet(licenseMap, "name", String.class));
        openAPI.setLicenseUrl(safeGet(licenseMap, "url", String.class));
        openAPI.setVersion(safeGet(infoMap, "version", String.class));

        logger.info("Specification parsed.");
        return openAPI;
    }

    /**
     * Function that infers types when missing. For every pathItem, parameter, request body and response is checked
     * for missing 'type' field
     */
    private void inferParameterTypes() {
        logger.info("Inferring parameter types where missing...");

        Map<String, Map<String, Map<String, Object>>> paths =
                (Map<String, Map<String, Map<String, Object>>>) this.openAPIMap.get("paths");

        paths.values().forEach(
                operationMap -> operationMap.entrySet().stream().filter((entry -> !entry.getKey().startsWith("x-")))
                        .forEach(entry -> {
                            Map<String, Object> operation = entry.getValue();

                            // Infer types for parameters filed
                            safeGet(operation, "parameters", ArrayList.class).forEach(
                                    parameter -> recursiveTypeInference((Map<String, Object>) parameter)
                            );

                            Map<String, Object> requestBody = safeGet(operation, "requestBody", LinkedTreeMap.class);
                            Map<String, Object> content = safeGet(requestBody, "content", LinkedTreeMap.class);

                            // At the moment we only support JSON and x-www-form-urlencoded
                            Map<String, Object> jsonContent = safeGet(content, "application/json", LinkedTreeMap.class);

                            // In case no JSON content is provided, we try to use x-www-form-urlencoded content
                            if (jsonContent.isEmpty()) {
                                jsonContent = safeGet(content, "application/x-www-form-urlencoded", LinkedTreeMap.class);
                            }

                            if (jsonContent.isEmpty()) {
                                jsonContent = safeGet(content, "*/*", LinkedTreeMap.class);
                            }

                            Map<String, Object> schema = safeGet(jsonContent, "schema", LinkedTreeMap.class);
                            if (!schema.isEmpty()) {
                                recursiveTypeInference(schema);
                            }

                            Map<String, Object> responses = safeGet(operation, "responses", LinkedTreeMap.class);

                            for (Map.Entry<String, Object> responseMap : responses.entrySet()) {
                                Map<String, Object> response = (Map<String, Object>) responseMap.getValue();
                                content = safeGet(response, "content", LinkedTreeMap.class);

                                // At the moment we only support JSON and x-www-form-urlencoded
                                jsonContent = safeGet(content, "application/json", LinkedTreeMap.class);

                                // In case no JSON content is provided, we try to use x-www-form-urlencoded content
                                if (jsonContent.isEmpty()) {
                                    jsonContent = safeGet(content, "application/x-www-form-urlencoded", LinkedTreeMap.class);
                                }

                                if (jsonContent.isEmpty()) {
                                    jsonContent = safeGet(content, "*/*", LinkedTreeMap.class);
                                }

                                schema = safeGet(jsonContent, "schema", LinkedTreeMap.class);

                                if (!schema.isEmpty()) {
                                    recursiveTypeInference(schema);
                                }
                            }
                        })
        );
    }

    /**
     * Function that recursively search for missing 'type' field in parameters schema. When a missing one is found,
     * if the parameter is compatible with an object or an array, the type is added to the parameter schema.
     * @param parameter the parameter for which the type should be inferred.
     */
    private void recursiveTypeInference(Map<String, Object> parameter) {
        // distinction between standard parameters and request/response body parameters
        Map<String, Object> targetMap = parameter.containsKey("schema") ?
                (Map<String, Object>) parameter.get("schema") :
                parameter;

        // if type is missing, infer it
        if (!targetMap.containsKey("type")) {
            // Structured types
            if (targetMap.containsKey("properties") || targetMap.get("required") instanceof List) {
                targetMap.put("type", "object");
            } else if (targetMap.containsKey("items")) {
                targetMap.put("type", "array");
            }

            // Terminal types
            if (targetMap.containsKey("multipleOf") || targetMap.containsKey("maximum") ||
                    targetMap.containsKey("exclusiveMaximum") || targetMap.containsKey("minimum") ||
                    targetMap.containsKey("exclusiveMinimum")
            ) {
                targetMap.put("type", "number");
            }

            if (targetMap.containsKey("maxLength") || targetMap.containsKey("minLength") ||
                    targetMap.containsKey("pattern")
            ) {
                targetMap.put("type", "string");
            }

        }

        // Infer types also for combined schemas fields
        for (String field : new String[]{"oneOf", "allOf", "anyOf"}) {
            safeGet(targetMap, field, ArrayList.class).forEach(
                    s -> recursiveTypeInference((Map<String, Object>) s)
            );
        }

        if (targetMap.containsKey("not")) {
            recursiveTypeInference((Map<String, Object>) targetMap.get("not"));
        }

        // For structured types move downward in the structure to infer missing types
        switch (ParameterType.getTypeFromString((String) targetMap.get("type"))) {
            case OBJECT:
                Map<String, Map<String, Object>> properties = safeGet(targetMap, "properties", LinkedTreeMap.class);
                properties.values().forEach(this::recursiveTypeInference);
            case ARRAY:
                Map<String, Object> items = safeGet(targetMap, "items", LinkedTreeMap.class);
                recursiveTypeInference(items);
                break;
        }
    }

    /**
     * Enriches the specification with an extension to keep track, through the additional field 'x-schemaName', of
     * the name of the schemas before their normalization
     */
    private void addSchemasNames() {
        logger.info("Enriching specification with schema names..");

        if (this.openAPIMap.containsKey("components")) {
            Map<String, Object> components = (Map<String, Object>) openAPIMap.get("components");

            Map<String, Map<String, Object>> schemas = safeGet(components, "schemas", LinkedTreeMap.class);

            for (Map.Entry<String, Map<String, Object>> schema : schemas.entrySet()) {
                schema.getValue().put("x-schemaName", schema.getKey());
            }
        }
    }

    /**
     * Normalizes the 'required' attribute in request/response bodies. In fact, within schemas the 'required' field
     * is a list instead of a Boolean. Hence, we propagate the required values downwards in the structure
     */
    private void unfoldRequiredAttributes() {
        Map<String, Object> paths = (Map<String, Object>) openAPIMap.get("paths");

        // Iterate through path items and look for 'parameters'
        for (Map.Entry<String, Object> pathItemMap : paths.entrySet()) {

            if (pathItemMap.getKey().startsWith("x-")) {
                continue;
            }

            Map<String, Object> pathItem = (Map<String, Object>) pathItemMap.getValue();

            for (Map.Entry<String, Object> operation : pathItem.entrySet()) {

                if (operation.getKey().startsWith("x-")) {
                    continue;
                }

                Map<String, Object> operationMap = (Map<String, Object>) operation.getValue();

                // Check every item in 'parameters' list
                List<Map<String, Object>> parameters = safeGet(operationMap, "parameters", ArrayList.class);
                parameters.forEach(parameter -> {
                    if (parameter.containsKey("schema")) {
                        recursiveUnfoldRequired((Map<String, Object>) parameter.get("schema"));
                    }
                });

                // Check for body parameters
                Map<String, Object> requestBody = safeGet(operationMap, "requestBody", LinkedTreeMap.class);
                Map<String, Object> content = safeGet(requestBody, "content", LinkedTreeMap.class);

                // At the moment we only support JSON and x-www-form-urlencoded
                Map<String, Object> jsonContent = safeGet(content, "application/json", LinkedTreeMap.class);

                // In case no JSON content is provided, we try to use x-www-form-urlencoded content
                if (jsonContent.isEmpty()) {
                    jsonContent = safeGet(content, "application/x-www-form-urlencoded", LinkedTreeMap.class);
                }

                if (jsonContent.isEmpty()) {
                    jsonContent = safeGet(content, "*/*", LinkedTreeMap.class);
                }

                Map<String, Object> schema = safeGet(jsonContent, "schema", LinkedTreeMap.class);

                if (!schema.isEmpty()) {
                    recursiveUnfoldRequired(schema);
                    // Required value is stored since it could be null. In this case, it should be considered 'false'
                    Boolean isRequired = (Boolean) requestBody.get("required");
                    requestBody.put("required", isRequired != null ? isRequired : false);
                }

                // Check for output parameters (response body)
                Map<String, Object> responses = safeGet(operationMap, "responses", LinkedTreeMap.class);

                for (Map.Entry<String, Object> responseMap : responses.entrySet()) {
                    Map<String, Object> response = (Map<String, Object>) responseMap.getValue();
                    content = safeGet(response, "content", LinkedTreeMap.class);

                    // At the moment we only support JSON and x-www-form-urlencoded
                    jsonContent = safeGet(content, "application/json", LinkedTreeMap.class);

                    // In case no JSON content is provided, we try to use x-www-form-urlencoded content
                    if (jsonContent.isEmpty()) {
                        jsonContent = safeGet(content, "application/x-www-form-urlencoded", LinkedTreeMap.class);
                    }

                    if (jsonContent.isEmpty()) {
                        jsonContent = safeGet(content, "*/*", LinkedTreeMap.class);
                    }

                    schema = safeGet(jsonContent, "schema", LinkedTreeMap.class);

                    if (!schema.isEmpty()) {
                        recursiveUnfoldRequired(schema);
                        schema.put("required", true);
                    }
                }

            }

        }

    }

    /**
     * This function recursively scans JSON Parameters to normalize the required attribute. In object parameters, the
     * required field is provided as a list of required properties. The goal of the function is to bring these values
     * inside the parameters to reflect the structure of path/query/header parameters with a boolean required field.
     * @param map json schema of a parameter
     */
    private void recursiveUnfoldRequired(Map<String, Object> map) {
        // Avoid crash for undefined schemas
        if (map == null) {
            return;
        }

        String type = safeGet(map, "type", String.class);

        // Induction step 1: objects contain required as a list. Arrays can contain objects as elements
        if (type.equals("object")) {
            List<String> required = safeGet(map, "required", ArrayList.class);
            map.remove("required");

            Map<String, Map<String, Object>> properties = safeGet(map, "properties", LinkedTreeMap.class);

            for (Map.Entry<String, Map<String, Object>> property : properties.entrySet()) {
                // Call the recursion first, avoiding to overwriting the children required field
                recursiveUnfoldRequired(property.getValue());
                if (required.contains(property.getKey())) {
                    property.getValue().put("required", true);
                }
            }

        } else if (type.equals("array")) {
            recursiveUnfoldRequired((Map<String, Object>) map.get("items"));
        }

        // Induction step 2: if the parameter contains combined schemas, check for unsolved required
        List<Map<String, Object>> allOf = OpenApiParser.safeGet(map, "allOf", ArrayList.class);
        allOf.forEach(this::recursiveUnfoldRequired);
        List<Map<String, Object>> anyOf = OpenApiParser.safeGet(map, "anyOf", ArrayList.class);
        anyOf.forEach(this::recursiveUnfoldRequired);
        List<Map<String, Object>> oneOf = OpenApiParser.safeGet(map, "oneOf", ArrayList.class);
        oneOf.forEach(this::recursiveUnfoldRequired);

        // Base step: simple parameter, so required can only be a boolean field

    }

    /**
     * This function normalizes the location of common parameters. In fact, parameters that are common to every
     * path item defined within a path can be defined globally at path level. This function moves such values into each
     * path item.
     */
    // TODO: rename, I do not like it
    private void normalizeCommonParameters() {
        Map<String, Object> paths = (Map<String, Object>) openAPIMap.get("paths");

        // iterate through path items and look for 'parameters'
        for (Map.Entry<String, Object> pathItemMap : paths.entrySet()) {
            Map<String, Object> pathItem = (Map<String, Object>) pathItemMap.getValue();

            // retrieve parameters if present
            List<Map<String, Object>> parameters = (List<Map<String, Object>>) pathItem.get("parameters");
            pathItem.remove("parameters");

            if (parameters == null) {
                continue;
            }

            Map<Pair<String, String>, Map<String, Object>> parametersMap = new HashMap<>();
            parameters.forEach(parameter ->
                    parametersMap.put(new Pair<>((String) parameter.get("name"), (String) parameter.get("in")), parameter)
            );

            // copy all the parameters in the 'parameters' list (if exists) inside each operation
            // DO NOT OVERRIDE ALREADY DEFINED PARAMETERS!

            for (Map.Entry<String, Object> operationMap : pathItem.entrySet()) {
                // Parse iff the key is a supported HTTP method
                try {
                    HttpMethod.getMethod(operationMap.getKey());
                } catch (IllegalArgumentException e) {
                    continue;
                }

                Map<String, Object> operation = (Map<String, Object>) operationMap.getValue();
                List<Map<String, Object>> operationParameters = safeGet(operation, "parameters", ArrayList.class);

                if (operationParameters.isEmpty()) {
                    operation.put("parameters", operationParameters);
                }

                Set<Pair<String, String>> operationParameterSet = new HashSet<>();
                operationParameters.forEach(parameter ->
                        operationParameterSet.add(new Pair<>((String) parameter.get("name"), (String) parameter.get("in")))
                );

                // For each parameter, if not duplicate, add to operation parameters
                for (Map.Entry<Pair<String, String>, Map<String, Object>> parameter : parametersMap.entrySet()) {
                    if (!operationParameterSet.contains(parameter.getKey())) {
                        operationParameters.add(parameter.getValue());
                    }
                }

            }

        }

    }

    /**
     * This function normalizes the specification w.r.t. the '$ref' values, replacing references with their actual
     * value.
     */
    public void solveOpenAPIrefs() {

        LinkedList<Pair<ArrayList<String>, Map<String, Object>>> queue = new LinkedList<>();

        // Resolve first components since they are the targets of the refs
        if (openAPIMap.containsKey("components")) {
            Map<String, Object> components = (Map<String, Object>) openAPIMap.get("components");

            logger.info("Solving components/schemas refs..");
            Map<String, Map<String, Object>> schemas = safeGet(components, "schemas", LinkedTreeMap.class);

            resolveSchemaRef(schemas);

            // Once resolved parameters schema refs, resolve schema items refs
            schemas.values().forEach(this::resolvePropertyItemRef);

            // Parse responses
            logger.info("Solving components/responses refs..");
            Map<String, Object> responses = safeGet(components, "responses", LinkedTreeMap.class);
            responses.values().forEach(response -> replaceContentRef((Map<String, Object>) response));

            // Parse parameters
            logger.info("Solving components/parameters refs..");
            Map<String, Object> parameters = safeGet(components, "parameters", LinkedTreeMap.class);
            parameters.values().forEach(parameter -> replaceSchemaRef((Map<String, Object>) parameter));

            // Parse requestBodies
            logger.info("Solving components/requestBodies refs..");
            Map<String, Object> bodies = safeGet(components, "requestBodies", LinkedTreeMap.class);
            bodies.values().forEach(body -> replaceContentRef((Map<String, Object>) body));

        }

        // Once components are solved, solve all refs inside pathItems
        recursiveReplaceRef((Map<String, Object>) openAPIMap.get("paths"));

        logger.info("Specification references solved.");
    }

    /**
     * Resolves schema refs using a queue, since every schema ref can potentially contain many other schema refs.
     * @param schemas Map with the schemas defined in 'components/schemas'
     */
    private void resolveSchemaRef(Map<String, Map<String, Object>> schemas) {

        // Queue of the schemas to be solved.
        LinkedList<Pair<
                List<String>, // List of all the schemas that have already been solved along the path
                Map<String, Object> // Map containing the actual schema
                >> queue = new LinkedList<>();

        // Iterate through each schema and them to the queue to resolve refs
        for (Map<String, Object> schema : schemas.values()) {
            ArrayList<String> pathSolvedSchemas = new ArrayList<>(1);
            pathSolvedSchemas.add((String) schema.get("x-schemaName")); // set as solved the root schema along the part starting from itself
            queue.addLast(new Pair<>(pathSolvedSchemas, schema)); // add to queue the path and its schema
        }

        // Resolve components refs
        while (!queue.isEmpty()) {
            Pair<List<String>, Map<String, Object>> top = queue.pollFirst();
            List<String> pathSolvedSchemas = top.getFirst();
            Map<String, Object> schema = top.getSecond();

            // Check whether schema is only a ref
            if (schema.containsKey("$ref")) {
                Map<String, Object> solvedRef = getElementByRef((String) schema.get("$ref"));
                String refSchemaName = (String) solvedRef.get("x-schemaName");
                schema.remove("$ref"); // Remove ref field from specification

                // Check if the referred schema has already been solved along this path. If so, skip it (recursive ref)
                if (!pathSolvedSchemas.contains(refSchemaName)) {
                    schema.putAll(solvedRef); // Update the actual schema with the values in the referenced schema
                    ArrayList<String> extendedPathSolvedSchemas = new ArrayList<>(pathSolvedSchemas);
                    extendedPathSolvedSchemas.add(refSchemaName);
                    // Add the actual, resolved schema to the queue again, since it could contain other references in its subfields
                    queue.addLast(new Pair<>(extendedPathSolvedSchemas, schema));
                } else {
                    logger.warn("Recursive references found: " + pathSolvedSchemas + ", " + refSchemaName);
                }
            } else {
                // if combined schemas are present, solve them too
                List<Map<String, Object>> allOf = safeGet(schema, "allOf", ArrayList.class);
                allOf.forEach(element -> queue.addLast(new Pair<>(pathSolvedSchemas, element)));
                List<Map<String, Object>> oneOf = safeGet(schema, "oneOf", ArrayList.class);
                oneOf.forEach(element -> queue.addLast(new Pair<>(pathSolvedSchemas, element)));
                List<Map<String, Object>> anyOf = safeGet(schema, "anyOf", ArrayList.class);
                anyOf.forEach(element -> queue.addLast(new Pair<>(pathSolvedSchemas, element)));
                Map<String, Object> not = safeGet(schema, "not", LinkedTreeMap.class);
                if (!not.isEmpty()) {
                    queue.addLast(new Pair<>(pathSolvedSchemas, not));
                }

                // Check if the schema describes something structured (i.e. arrays or objects)
                if ("object".equals(schema.get("type")) || schema.containsKey("properties")) {
                    Map<String, Map<String, Object>> properties = safeGet(schema, "properties", LinkedTreeMap.class);
                    properties.forEach((key, value) -> queue.addLast(new Pair<>(pathSolvedSchemas, value)));
                } else if ("array".equals(schema.get("type")) || schema.containsKey("items")) {
                    Map<String, Object> items = safeGet(schema, "items", LinkedTreeMap.class);
                    queue.addLast(new Pair<>(pathSolvedSchemas, items));
                }
            }

            // If no ref is present and the schema describes something unstructured, then there can be no other refs in this path

        }
    }

    /**
     * Resolve refs for structured parameters (i.e. objects and arrays) recursively.
     * @param propertiesMap Map containing the description of the parameter
     */
    private void resolvePropertyItemRef(Map<String, Object> propertiesMap) {

        // Induction step 1: properties can contain referred items
        if (propertiesMap.containsKey("properties")) {
            ((Map<String, Object>) propertiesMap.get("properties")).values().forEach(
                    property -> resolvePropertyItemRef((Map<String, Object>) property)
            );
        }

        else if (propertiesMap.containsKey("items")) {
            Map<String, Object> itemValue = (Map<String, Object>) propertiesMap.get("items");
            if (itemValue.containsKey("$ref")) {
                propertiesMap.put("items", getElementByRef((String) itemValue.get("$ref")));
                // Induction step 2: items can contain referred items
                resolvePropertyItemRef((Map<String, Object>) propertiesMap.get("items"));
            }
            // Base step
            // items field do not contain refs
        }
    }

    /**
     * Replaces the reference to a schema with its actual value
     * @param map Map that could potentially contain a schema reference
     */
    private void replaceSchemaRef(Map<String, Object> map) {
        if (map.containsKey("schema")) {
            Map<String, Object> schema = (Map<String, Object>) map.get("schema");

            if (schema.containsKey("$ref")) {
                Map<String, Object> solvedRef = getElementByRef((String) schema.get("$ref"));
                map.put("schema", solvedRef);
            }
        }
    }

    /**
     * Replaces the reference to a content schema with its actual value
     * @param map Content map that could potentially contain a schema reference
     */
    private void replaceContentRef(Map<String, Object> map) {
        Map<String, Object> content = safeGet(map, "content", LinkedTreeMap.class);

        for (Map.Entry<String, Object> mediaType : content.entrySet()) {
            Map<String, Object> mediaTypeMap = (Map<String, Object>) mediaType.getValue();

            replaceSchemaRef(mediaTypeMap);
        }
    }

    /**
     * Recursively replace references in a given map searching for map/list children.
     * @param map Map that could contain references to replace
     */
    private void recursiveReplaceRef(Map<String, Object> map) {

        // Scan each child looking for a ref to solve
        for (Map.Entry<String, Object> child : map.entrySet()) {

            // base step 3: the value is null
            if (child.getValue() == null) {
                return;
            }

            // Induction steps
            if (Map.class.isAssignableFrom(child.getValue().getClass())) {
                recursiveReplaceRef((Map<String, Object>) child.getValue());
            }
            else if (List.class.isAssignableFrom(child.getValue().getClass())) {
                recursiveReplaceRef((List<Object>) child.getValue());
            }

            // base step 1: ref found
            else if (child.getKey().equals("$ref")) {
                Map<String, Object> referencedElement = getElementByRef((String) child.getValue());
                map.clear();
                map.putAll(referencedElement);
                return; // When a ref is present, no other field should be used
            }

            // base step 2: a simple value instead of a map/list is found. No action needed
        }
    }

    /**
     * Recursively replace references in a given list searching for map/list children.
     * @param list List that could contain references to replace
     */
    private void recursiveReplaceRef(List<Object> list) {

        // Scan each child looking for a ref to solve
        for (Object child : list) {

            if (Map.class.isAssignableFrom(child.getClass())) {
                recursiveReplaceRef((Map<String, Object>) child);
            }
            else if (List.class.isAssignableFrom(child.getClass())) {
                recursiveReplaceRef((List<Object>) child);
            }
        }
    }

    /**
     * The function returns a deep clone of the component referenced by the input string.
     * The choice of the deep clone is to avoid unintentional modifications on the original object
     * @param ref path of the referenced resource
     * @return Deep clone of the referenced object
     */
    private Map<String, Object> getElementByRef(String ref) {
        Map<String, Object> map = this.openAPIMap;
        String[] componentPath = ref.split("/");

        // Skip the first element, since it is a '#'
        for (int i = 1; i < componentPath.length; ++i) {
            map = (Map<String, Object>) map.get(componentPath[i]);
            if (map == null) {
                throw new InvalidOpenApiException("Reference to '" + ref + "' cannot be resolved since the element" +
                        "is missing in the specification.");
            }
        }

        // Copy object instead of put it directly to avoid unexpected behaviors
        // Used to avoid unintentional modifications to the map passed as parameter
        return ObjectHelper.deepCloneObject(map);
    }

    /**
     * This function returns the name of the object referenced by the string passed as parameter
     * @param ref path of the referenced resource
     * @return Name of the referenced resource
     */
    private String getObjectNameByRef(String ref) {
        String[] componentPath = ref.split("/");
        return componentPath[componentPath.length -1];
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
