package io.resttestgen.core.helper.jsonserializer;

import com.google.gson.*;
import io.resttestgen.core.datatype.parameter.*;
import io.resttestgen.core.openapi.Operation;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class OperationSerializer implements JsonSerializer<Operation> {

    Map<String, Response> responses = new HashMap<>();

    @Override
    public JsonElement serialize(Operation src, Type typeOfSrc, JsonSerializationContext context) {

        // Instantiate Gson with all the relevant serializers
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(ParameterObject.class, new ParameterObjectSerializer())
                .registerTypeAdapter(ParameterArray.class, new ParameterArraySerializer())
                .registerTypeAdapter(StringParameter.class, new StringParameterSerializer())
                .registerTypeAdapter(NumberParameter.class, new NumberParameterSerializer())
                .registerTypeAdapter(BooleanParameter.class, new BooleanParameterSerializer())
                .registerTypeAdapter(NullParameter.class, new NullParameterSerializer())
                .registerTypeAdapter(GenericParameter.class, new GenericParameterSerializer())
                .setPrettyPrinting()
                .create();

        // Compute responses map
        for (String responseKey : src.getOutputParameters().keySet()) {
            responses.put(responseKey, new Response(src.getOutputParameters().get(responseKey)));
        }

        // Build up components of OpenAPI operation object
        JsonObject result = new JsonObject();
        result.add("tags", null);
        result.add("summary", null);
        result.add("description", gson.toJsonTree(src.getDescription().equals("") ? null : src.getDescription()));
        result.add("externalDocs", null);
        result.add("operationId", gson.toJsonTree(src.getOperationId().equals("") ? null : src.getOperationId()));
        result.add("parameters", gson.toJsonTree(src.getFirstLevelRequestParametersNotInBody()));
        if (src.getRequestBody() != null) {
            result.add("requestBody", gson.toJsonTree(new RequestBody(src)));
        }
        result.add("responses", gson.toJsonTree(responses));
        result.add("callbacks", null);
        result.add("deprecated", null);
        result.add("security", null);
        result.add("servers", null);
        return result;
    }

    private static class RequestBody {

        private final String description;
        private final Map<String, Map<String, StructuredParameterElement>> content = new HashMap<>();
        private final Boolean required;

        public RequestBody(Operation operation) {
            this.description = operation.getRequestBodyDescription().equals("") ? null : operation.getRequestBodyDescription();
            Map<String, StructuredParameterElement> schema = new HashMap<>();
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
        private Map<String, Map<String, StructuredParameterElement>> content = new HashMap<>();

        public Response(StructuredParameterElement responseBody) {
            Map<String, StructuredParameterElement> schema = new HashMap<>();
            schema.put("schema", responseBody);
            content.put("application/json", schema);
        }
    }
}
