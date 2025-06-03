package io.resttestgen.core.helper.jsonserializer;

import com.google.gson.*;
import io.resttestgen.core.datatype.ParameterName;
import io.resttestgen.core.datatype.parameter.Parameter;
import io.resttestgen.core.datatype.parameter.ParameterUtils;
import io.resttestgen.core.datatype.parameter.combined.OneOfParameter;
import io.resttestgen.core.datatype.parameter.leaves.*;
import io.resttestgen.core.datatype.parameter.structured.ArrayParameter;
import io.resttestgen.core.datatype.parameter.structured.ObjectParameter;
import io.resttestgen.core.datatype.parameter.structured.StructuredParameter;
import io.resttestgen.core.openapi.Operation;
import kotlin.Pair;

import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

public class OperationSerializer implements JsonSerializer<Operation> {

    Map<String, Response> responses = new HashMap<>();

    @Override
    public JsonElement serialize(Operation src, Type typeOfSrc, JsonSerializationContext context) {

        // Instantiate Gson with all the relevant serializers
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(ObjectParameter.class, new ParameterObjectSerializer())
                .registerTypeAdapter(ArrayParameter.class, new ParameterArraySerializer())
                .registerTypeAdapter(StringParameter.class, new StringParameterSerializer())
                .registerTypeAdapter(NumberParameter.class, new NumberParameterSerializer())
                .registerTypeAdapter(BooleanParameter.class, new BooleanParameterSerializer())
                .registerTypeAdapter(NullParameter.class, new NullParameterSerializer())
                .registerTypeAdapter(GenericParameter.class, new GenericParameterSerializer())
                .registerTypeAdapter(OneOfParameter.class, new OneOfParameterSerializer())
                .setPrettyPrinting()
                .create();

        // Compute responses map
        for (String responseKey : src.getOutputParameters().keySet()) {
            responses.put(responseKey, new Response(src.getOutputParameters().get(responseKey)));
        }

        // Add mandatory empty response in case no responses are not defined in the specification
        if (responses.isEmpty()) {
            responses.put("default", new Response());
        }

        // Build up components of OpenAPI operation object
        JsonObject result = new JsonObject();
        result.add("tags", null);
        result.add("summary", null);
        result.add("description", gson.toJsonTree(src.getDescription().isEmpty() ? null : src.getDescription()));
        result.add("externalDocs", null);
        result.add("operationId", gson.toJsonTree(src.getOperationId().isEmpty() ? null : src.getOperationId()));

        // Remove objects from parameters (not supported outside body at the moment)
        // FIXME: investigate how to support objects in parameters
        List<Parameter> params = src.getFirstLevelRequestParametersNotInBody().stream()
                .filter(p -> !ParameterUtils.isObject(p)).collect(Collectors.toList());

        result.add("parameters", gson.toJsonTree(params));
        if (src.getRequestBody() != null) {
            result.add("requestBody", gson.toJsonTree(new RequestBody(src)));
        }
        result.add("responses", gson.toJsonTree(responses));
        result.add("callbacks", null);
        result.add("deprecated", null);
        result.add("security", null);
        result.add("servers", null);

        // Compute IPDs
        List<String> idps = renderIPDs(src);

        // Export IDPs only if at least one exists
        if (!idps.isEmpty()) {
            result.add("x-dependencies", gson.toJsonTree(idps));
        }
        return result;
    }

    private List<String> renderIPDs(Operation operation) {
        List<String> renderedIPDs = new LinkedList<>();

        // Requires
        for (Pair<String, String> requires : operation.getRequires()) {
            renderedIPDs.add("IF " + requires.getFirst() + " THEN " + requires.getSecond() + ";");
        }

        // Or
        for (Set<ParameterName> or : operation.getOr()) {
            StringJoiner parameterNames = new StringJoiner(",");
            for (ParameterName parameterName : or) {
                parameterNames.add(parameterName.toString());
            }
            renderedIPDs.add("Or(" + parameterNames + ");");
        }

        // OnlyOne
        for (Set<ParameterName> onlyOne : operation.getOnlyOne()) {
            StringJoiner parameterNames = new StringJoiner(",");
            for (ParameterName parameterName : onlyOne) {
                parameterNames.add(parameterName.toString());
            }
            renderedIPDs.add("OnlyOne(" + parameterNames + ");");
        }

        // AllOrNone
        for (Set<ParameterName> allOrNone : operation.getAllOrNone()) {
            StringJoiner parameterNames = new StringJoiner(",");
            for (ParameterName parameterName : allOrNone) {
                parameterNames.add(parameterName.toString());
            }
            renderedIPDs.add("AllOrNone(" + parameterNames + ");");
        }

        // ZeroOrOne
        for (Set<ParameterName> zeroOrOne : operation.getZeroOrOne()) {
            StringJoiner parameterNames = new StringJoiner(",");
            for (ParameterName parameterName : zeroOrOne) {
                parameterNames.add(parameterName.toString());
            }
            renderedIPDs.add("ZeroOrOne(" + parameterNames + ");");
        }

        return renderedIPDs;
    }

    private static class RequestBody {

        private final String description;
        private final Map<String, Map<String, StructuredParameter>> content = new HashMap<>();
        private final Boolean required;

        public RequestBody(Operation operation) {
            this.description = operation.getRequestBodyDescription().isEmpty() ? null : operation.getRequestBodyDescription();
            Map<String, StructuredParameter> schema = new HashMap<>();
            schema.put("schema", operation.getRequestBody());
            this.content.put(operation.getRequestContentType(), schema);
            if (operation.getRequestBody() != null && operation.getRequestBody().isRequired()) {
                this.required = true;
            } else {
                this.required = null;
            }
        }
    }

    private static class Response {

        private String description = "Response description currently not supported by RestTestGen.";
        private Map<String, Map<String, StructuredParameter>> content = new HashMap<>();

        public Response(StructuredParameter responseBody) {
            Map<String, StructuredParameter> schema = new HashMap<>();
            schema.put("schema", responseBody);
            content.put("application/json", schema);
        }

        public Response() {
            description = "No response information provided for this operation.";
        }
    }
}
