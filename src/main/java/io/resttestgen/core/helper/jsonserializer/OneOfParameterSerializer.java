package io.resttestgen.core.helper.jsonserializer;

import com.google.gson.*;
import io.resttestgen.core.datatype.parameter.combined.OneOfParameter;
import io.resttestgen.core.datatype.parameter.leaves.*;
import io.resttestgen.core.datatype.parameter.structured.ObjectParameter;
import io.resttestgen.core.datatype.parameter.structured.ArrayParameter;
import io.resttestgen.core.openapi.OpenApi;
import io.resttestgen.core.openapi.Operation;

import java.lang.reflect.Type;

public class OneOfParameterSerializer implements JsonSerializer<OneOfParameter> {
    @Override
    public JsonElement serialize(OneOfParameter src, Type typeOfSrc, JsonSerializationContext context) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(OpenApi.class, new OpenApiSerializer())
                .registerTypeAdapter(Operation.class, new OperationSerializer())
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

        JsonObject result = new JsonObject();
        result.add("oneOf", gson.toJsonTree(src.getParametersSchemas()));
        return result;
    }
}