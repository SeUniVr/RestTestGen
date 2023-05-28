package io.resttestgen.core.helper.jsonserializer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import io.resttestgen.core.datatype.parameter.leaves.NullParameter;

import java.lang.reflect.Type;

public class NullParameterSerializer implements JsonSerializer<NullParameter> {

    @Override
    public JsonElement serialize(NullParameter src, Type typeOfSrc, JsonSerializationContext context) {
        // NullParameters are not describable by the OpenAPI specification grammar, so we skip them
        return new JsonObject();
    }
}
