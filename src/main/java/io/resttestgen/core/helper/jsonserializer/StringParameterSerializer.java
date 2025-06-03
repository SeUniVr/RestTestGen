package io.resttestgen.core.helper.jsonserializer;

import com.google.gson.*;
import io.resttestgen.core.datatype.parameter.ParameterUtils;
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
            if (!src.getDescription().trim().isEmpty()) {
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
            if (!src.getEnumValues().isEmpty()) {
                result.add("enum", gson.toJsonTree(src.getEnumValues()));
            }

            // Add examples, if examples are provided
            // FIXME: check if export format is correct
            if (!src.getExamples().isEmpty()) {
                result.add("example", gson.toJsonTree(src.getExamples()));
            }
        } else {

            // Add parameter name, if not a reference element
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

            // Add location, if root element
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
            if (!src.getEnumValues().isEmpty()) {
                schema.add("enum", gson.toJsonTree(src.getEnumValues()));
            }

            // Add examples, if examples are provided
            // FIXME: check if export format is correct
            if (src.getExamples().stream().findFirst().isPresent()) {
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
