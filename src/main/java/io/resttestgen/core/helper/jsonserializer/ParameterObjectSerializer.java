package io.resttestgen.core.helper.jsonserializer;

import com.google.gson.*;
import io.resttestgen.core.datatype.parameter.*;
import io.resttestgen.core.openapi.OpenAPI;
import io.resttestgen.core.openapi.Operation;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class ParameterObjectSerializer implements JsonSerializer<ParameterObject> {

    @Override
    public JsonElement serialize(ParameterObject src, Type typeOfSrc, JsonSerializationContext context) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(OpenAPI.class, new OpenApiSerializer())
                .registerTypeAdapter(Operation.class, new OperationSerializer())
                .registerTypeAdapter(ParameterObject.class, new ParameterObjectSerializer())
                .registerTypeAdapter(ParameterArray.class, new ParameterArraySerializer())
                .registerTypeAdapter(StringParameter.class, new StringParameterSerializer())
                .registerTypeAdapter(NumberParameter.class, new NumberParameterSerializer())
                .registerTypeAdapter(BooleanParameter.class, new BooleanParameterSerializer())
                .registerTypeAdapter(NullParameter.class, new NullParameterSerializer())
                .registerTypeAdapter(GenericParameter.class, new GenericParameterSerializer())
                .setPrettyPrinting()
                .create();

        JsonObject result = new JsonObject();

        // Only process objects if they are in request or response body. They are not allowed elsewhere
        if (src.getLocation() == ParameterLocation.REQUEST_BODY || src.getLocation() == ParameterLocation.RESPONSE_BODY) {
            Map<String, ParameterElement> properties = new HashMap<>();
            for (ParameterElement parameter : src.getProperties()) {
                properties.put(parameter.getName().toString(), parameter);
            }
            result.addProperty("type", "object");
            result.add("properties", gson.toJsonTree(properties));
        }
        return result;
    }
}
