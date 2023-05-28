package io.resttestgen.core.helper.jsonserializer;

import com.google.gson.*;
import io.resttestgen.core.datatype.parameter.attributes.ParameterLocation;
import io.resttestgen.core.datatype.parameter.attributes.ParameterTypeFormat;
import io.resttestgen.core.datatype.parameter.leaves.*;
import io.resttestgen.core.datatype.parameter.structured.ObjectParameter;
import io.resttestgen.core.datatype.parameter.structured.ArrayParameter;

import java.lang.reflect.Type;

public class StringParameterSerializer implements JsonSerializer<StringParameter> {

    @Override
    public JsonElement serialize(StringParameter src, Type typeOfSrc, JsonSerializationContext context) {
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

            // Add description, if defined
            if (!src.getDescription().trim().equals("")) {
                result.addProperty("description", src.getDescription());
            }

            // Add type
            result.addProperty("type", src.getType().toString().toLowerCase());

            // Add format, if defined
            if (src.getFormat() != ParameterTypeFormat.MISSING && src.getFormat() != ParameterTypeFormat.UNKNOWN) {
                result.addProperty("format", src.getFormat().toString().toLowerCase().replaceAll("_", "-"));
            }

            // Add required, if true
            if (src.isRequired()) {
                result.addProperty("required", true);
            }

            // Add minLength, if greater than 0
            if (src.getMinLength() != null && src.getMinLength() > 0) {
                result.addProperty("minLength", src.getMinLength());
            }

            // Add maxLength, if defined
            if (src.getMaxLength() != null) {
                result.addProperty("maxLength", src.getMaxLength());
            }

            // Add default, if default value is provided
            if (src.getDefaultValue() != null) {
                result.add("default", gson.toJsonTree(src.getDefaultValue()));
            }

            // Add enum, if enum values are provided
            if (src.getEnumValues().size() > 0) {
                result.add("enum", gson.toJsonTree(src.getEnumValues()));
            }

            // Add examples, if examples are provided
            // FIXME: check if export format is correct
            if (src.getExamples().size() > 0) {
                result.add("example", gson.toJsonTree(src.getExamples()));
            }
        } else {

            // Add parameter name
            result.addProperty("name", src.getName().toString());

            // Add description, if not empty
            if (!src.getDescription().trim().equals("")) {
                result.addProperty("description", src.getDescription());
            }

            // Add location
            result.addProperty("in", src.getLocation().toString().toLowerCase());

            JsonObject schema = new JsonObject();

            // Add type
            schema.addProperty("type", src.getType().toString().toLowerCase());
            // Add format, if not empty
            if (src.getFormat() != ParameterTypeFormat.MISSING && src.getFormat() != ParameterTypeFormat.UNKNOWN) {
                schema.addProperty("format", src.getFormat().toString().toLowerCase().replaceAll("_", "-"));
            }

            // Add minLength, if greater than 0
            if (src.getMinLength() != null && src.getMinLength() > 0) {
                schema.addProperty("minLength", src.getMinLength());
            }

            // Add maxLength, if defined
            if (src.getMaxLength() != null) {
                schema.addProperty("maxLength", src.getMaxLength());
            }

            // Add default, if default value is provided
            if (src.getDefaultValue() != null) {
                schema.add("default", gson.toJsonTree(src.getDefaultValue()));
            }

            // Add enum, if enum values are provided
            if (src.getEnumValues().size() > 0) {
                schema.add("enum", gson.toJsonTree(src.getEnumValues()));
            }

            // Add examples, if examples are provided
            // FIXME: export all examples, not just the first one
            if (src.getExamples().stream().findFirst().isPresent()) {
                result.add("example", gson.toJsonTree(src.getExamples().stream().findFirst().get()));
            }

            result.add("schema", schema);

            if (src.isRequired() || src.getLocation() == ParameterLocation.PATH) {
                result.addProperty("required", true);
            }
        }

        return result;
    }
}
