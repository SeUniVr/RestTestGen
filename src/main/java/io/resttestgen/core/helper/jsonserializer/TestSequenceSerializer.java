package io.resttestgen.core.helper.jsonserializer;

import com.google.gson.*;
import io.resttestgen.core.testing.TestSequence;

import java.lang.reflect.Type;

/**
 * Used by Gson to write out the test sequence report.
 */
public class TestSequenceSerializer implements JsonSerializer<TestSequence> {

    @Override
    public JsonElement serialize(TestSequence src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject result = new JsonObject();
        Gson gson =  new Gson();
        result.add("generator",gson.toJsonTree(src.getGenerator()));
        result.add("name", gson.toJsonTree(src.getName()));
        result.add("testInteractions", gson.toJsonTree(src));
        result.add("generatedAt", gson.toJsonTree(src.getGeneratedAt()));
        result.add("testResults", gson.toJsonTree(src.getTestResults()));
        result.add("tags", gson.toJsonTree(src.getTags()));
        return result;
    }
}
