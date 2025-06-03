package io.resttestgen.core.helper.jsonserializer;

import com.google.gson.*;
import io.resttestgen.core.datatype.parameter.ParameterUtils;
import io.resttestgen.core.datatype.parameter.attributes.ParameterLocation;
import io.resttestgen.core.datatype.parameter.leaves.*;
import io.resttestgen.core.datatype.parameter.structured.ArrayParameter;
import io.resttestgen.core.datatype.parameter.structured.ObjectParameter;

import java.lang.reflect.Type;

public class BooleanParameterSerializer implements JsonSerializer<BooleanParameter> {

    @Override
    public JsonElement serialize(BooleanParameter src, Type typeOfSrc, JsonSerializationContext context) {
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

        if (src.getLocation() == ParameterLocation.REQUEST_BODY || src.getLocation() == ParameterLocation.RESPONSE_BODY) {

            // Add description, if not empty
            if (!src.getDescription().trim().isEmpty()) {
                result.addProperty("description", src.getDescription());
            }

            // Add type
            result.addProperty("type", src.getType().toString().toLowerCase());

            // Add required, if true
            result.addProperty("required", true);

            // Add default, if default value is provided
            if (src.getDefaultValue() != null) {
                result.add("default", gson.toJsonTree(src.getDefaultValue()));
            }

            // Add enum, if enum values are provided
            if (!src.getEnumValues().isEmpty()) {
                result.add("enum", gson.toJsonTree(src.getEnumValues()));
            }

            // Add examples, if examples are provided
            // FIXME: check if the example export format is correct
            if (src.getExamples().stream().findFirst().isPresent()) {
                result.add("example", gson.toJsonTree(src.getExamples().stream().findFirst().get()));
            }
        } else {

            // Add parameter name, if parameter is not a reference element
            if (!ParameterUtils.isReferenceElement(src)) {
                result.addProperty("name", src.getName().toString());

                // Add required
                if (src.isRequired() || src.getLocation() == ParameterLocation.PATH) {
                    result.addProperty("required", true);
                }
            }

            // Add description, if not empty
            if (!src.getDescription().trim().isEmpty()) {
                result.addProperty("description", src.getDescription());
            }

            // Add location, if parameter is root (no parent)
            if (src.getParent() == null) {
                result.addProperty("in", src.getLocation().toString().toLowerCase());
            }

            JsonObject schema = new JsonObject();

            // Type and constraints are places differently if parameter is a reference element
            if (ParameterUtils.isReferenceElement(src)) {
                schema = result;
            }

            // Add type
            schema.addProperty("type", src.getType().toString().toLowerCase());

            // Add default, if default value is provided
            if (src.getDefaultValue() != null) {
                schema.add("default", gson.toJsonTree(src.getDefaultValue()));
            }

            // Add enum, if enum values are provided
            if (!src.getEnumValues().isEmpty()) {
                schema.add("enum", gson.toJsonTree(src.getEnumValues()));
            }

            // Add examples, if examples are provided
            // FIXME: check if export format is correct
            if (!src.getExamples().isEmpty()) {
                result.add("example", gson.toJsonTree(src.getExamples()));
            }

            // Type and constraints are places differently if parameter is a reference element
            if (!ParameterUtils.isReferenceElement(src)) {
                result.add("schema", schema);
            }
        }

        return result;
    }
}
