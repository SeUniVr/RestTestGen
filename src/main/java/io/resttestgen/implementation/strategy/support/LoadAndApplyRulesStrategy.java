package io.resttestgen.implementation.strategy.support;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import io.resttestgen.core.datatype.rule.Rule;
import io.resttestgen.core.helper.jsonserializer.RuleDeserializer;
import io.resttestgen.core.testing.Strategy;

import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class LoadAndApplyRulesStrategy extends Strategy {

    @Override
    public void start() {

        String filename = "/home/davide/Workspace/Research/REST API testing/RestTestGen/RestTestGen-v2-Development/RestTestGen-Development/output/nlp-dev/nlpRules.json";

        List<Rule> rules = new LinkedList<>();

        try {
            Gson gson = new GsonBuilder().registerTypeAdapter(Rule.class, new RuleDeserializer()).create();
            JsonReader reader = new JsonReader(new FileReader(filename));
            JsonObject nlpRules = gson.fromJson(reader, JsonElement.class);
            for (Map.Entry<String, JsonElement> entry : nlpRules.entrySet()) {
                System.out.println(entry.getKey());
                JsonArray array = entry.getValue().getAsJsonArray();
                for (int i = 0; i < array.size(); i++) {
                    Rule rule = gson.fromJson(array.get(i), Rule.class);
                    rules.add(rule);
                }
            }
            System.out.println(rules);
        } catch (IOException ignored) {}
    }
}
