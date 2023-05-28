package io.resttestgen.core.helper.jsonserializer;

import com.google.gson.*;
import io.resttestgen.core.datatype.parameter.attributes.ParameterLocation;
import io.resttestgen.core.datatype.parameter.leaves.*;
import io.resttestgen.core.datatype.parameter.structured.ArrayParameter;
import io.resttestgen.core.datatype.parameter.structured.ObjectParameter;

import java.lang.reflect.Type;

public class ParameterArraySerializer implements JsonSerializer<ArrayParameter> {

    @Override
    public JsonElement serialize(ArrayParameter src, Type typeOfSrc, JsonSerializationContext context) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(ObjectParameter.class, new ParameterObjectSerializer())
                .registerTypeAdapter(ArrayParameter.class, new ParameterArraySerializer())
                .registerTypeAdapter(StringParameter.class, new StringParameterSerializer())
                .registerTypeAdapter(NumberParameter.class, new NumberParameterSerializer())
                .registerTypeAdapter(BooleanParameter.class, new BooleanParameterSerializer())
                .registerTypeAdapter(NullParameter.class, new NullParameterSerializer())
                .registerTypeAdapter(GenericParameter.class, new GenericParameterSerializer())
                .setPrettyPrinting()
                .create();

        JsonObject result = new JsonObject();

        // Format differently based on location (body, or elsewhere)
        if (src.getLocation() == ParameterLocation.REQUEST_BODY || src.getLocation() == ParameterLocation.RESPONSE_BODY) {

            // Add type
            result.addProperty("type", "array");

            // Add reference element
            result.add("items", gson.toJsonTree(src.getReferenceElement()));

            // Add minItems, if defined
            if (src.getMinItems() != null && src.getMinItems() > 0) {
                result.addProperty("minItems", src.getMinItems());
            }

            // Add maxItems, if defined
            if (src.getMaxItems() != null && src.getMaxItems() > 0) {
                result.addProperty("maxItems", src.getMaxItems());
            }
        } else {

            JsonObject schema = new JsonObject();

            // Add type
            schema.addProperty("type", "array");

            // Add reference element
            schema.add("items", gson.toJsonTree(src.getReferenceElement()));

            // Add minItems, if defined
            if (src.getMinItems() != null && src.getMinItems() > 0) {
                schema.addProperty("minItems", src.getMinItems());
            }

            // Add maxItems, if defined
            if (src.getMaxItems() != null && src.getMaxItems() > 0) {
                schema.addProperty("maxItems", src.getMaxItems());
            }

            result.add("schema", schema);

            // Add required, if true
            if (src.isRequired() || src.getLocation() == ParameterLocation.PATH) {
                result.addProperty("required", true);
            }
        }

        return result;
    }
}
