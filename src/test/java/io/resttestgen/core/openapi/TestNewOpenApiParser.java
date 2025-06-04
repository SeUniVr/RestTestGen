package io.resttestgen.core.openapi;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.resttestgen.boot.ApiUnderTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestNewOpenApiParser {

    private static final Logger logger = LoggerFactory.getLogger(TestNewOpenApiParser.class);

    @Disabled
    @Test
    public void testOldVsNewParser() throws IOException, NoSuchFieldException, IllegalAccessException {

        LinkedList<String> wildcards = new LinkedList<>();

        /*File currentDir = new File(System.getProperty("user.dir") + "/apis/.test-apis");
        File[] filesList = currentDir.listFiles();
        if (filesList != null) {
            for (File file : filesList) {
                if (file.isDirectory()) {
                    wildcards.add(file.getName());
                }
            }
        }

        // Remove invalid specs
        wildcards.remove("not-a-json");
        wildcards.remove("no-valid-server");
        wildcards.remove("not-a-spec");

        // Remove specs that are different for a valid reason
        wildcards.remove("apis-guru"); // additionalProperties
        wildcards.remove("justeat-uk"); // additionalProperties
        wildcards.remove("json-path"); // Old parser failed to expand one reference
        wildcards.remove("google-calendar"); // additionalProperties
        wildcards.remove("docker-engine"); // additionalProperties
        wildcards.remove("kubernetes"); // Old parser misses a cycle
        wildcards.remove("exavault"); // Required=false added for no reason
        wildcards.remove("type-inference"); // Required=false added for no reason
        wildcards.remove("bigred"); // Required=false added for no reason

        // Temporarily remove failing specs
        wildcards.remove("jira"); // Test ignored
        wildcards.remove("github"); // Test ignored
        wildcards.remove("bbc-nitro"); // Too slow
        wildcards.remove("bitbucket"); // Test ignored

        */

        wildcards.add("justeat-uk");

        for (String wildcard : wildcards) {
            System.out.println("PARSING " + wildcard);
            ApiUnderTest apiUnderTest = ApiUnderTest.loadTestApiFromFile(wildcard);

            OpenApi newOpenApi = OpenApiParser.parse(apiUnderTest);
            Map<String, Object> newOpenApiMap = (Map<String, Object>) OpenApiParser.getOpenApiMap().get("paths");

            OpenApi oldOpenApi = OpenApiParserOldAmedeo.parse(apiUnderTest);
            Map<String, Object> oldOpenApiMap = (Map<String, Object>) OpenApiParserOldAmedeo.getOpenApiMap().get("paths");

            //System.out.println("Old parser map: " + oldOpenApiMap);
            //System.out.println("New parser map: " + newOpenApiMap);

            Gson gson = new GsonBuilder().setPrettyPrinting().create();

            // Write the map to a file as JSON
            try (FileWriter writer = new FileWriter("old.json")) {
                gson.toJson(oldOpenApiMap, writer);
                System.out.println("JSON map written to file successfully.");
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Write the map to a file as JSON
            try (FileWriter writer = new FileWriter("new.json")) {
                gson.toJson(newOpenApiMap, writer);
                System.out.println("JSON map written to file successfully.");
            } catch (IOException e) {
                e.printStackTrace();
            }


            assertEquals(gson.toJson(oldOpenApiMap), gson.toJson(newOpenApiMap));
        }
    }
}
