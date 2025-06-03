package io.resttestgen.core.openapi;

import com.google.gson.Gson;
import io.resttestgen.boot.ApiUnderTest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestRefSolver {

    private static final Logger logger = LogManager.getLogger(TestRefSolver.class);

    @BeforeAll
    public static void setNormalizer() {
        Helper.setNormalizer();
    }

    @Disabled("Enable after parser is implemented")
    @Test
    public void testRefSolverRecursion() throws IOException, NoSuchFieldException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        /*logger.info("Test specification ref resolution for recursive schema.");
        OpenApiParser openAPIParser = new OpenApiParser(ApiUnderTest.loadTestApiFromFile("recursive-ref"));
        Helper.invokeParserMethod(openAPIParser, "addSchemasNames");
        openAPIParser.expandRefs(new LinkedList<>(), Helper.getParserMap(openAPIParser));

        File solvedSpecification = new File("apis/.test-apis/recursive-ref/specifications/solved.json");

        Map<String, Object> parsed = Helper.getParserMap(openAPIParser);
        Map<String, Object> paths = (Map<String, Object>) parsed.get("paths");
        Gson gson = new Gson();
        Reader reader = Files.newBufferedReader(Paths.get(solvedSpecification.getAbsolutePath()));
        Map<String, Object> rightMap = gson.fromJson(reader, Map.class);
        Map<String, Object> rightPaths = (Map<String, Object>) rightMap.get("paths");

        assertEquals(rightPaths, paths);*/
    }

    @Disabled("Enable after parser is implemented")
    @Test
    public void testRefSolverSafe() throws IOException, NoSuchFieldException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        /*logger.info("Test specification ref resolution (easy).");
        File specification = new File("build/resources/test/specifications/safeRef.json");
        OpenApiParser openAPIParser = new OpenApiParser(Paths.get(specification.getAbsolutePath()));
        Helper.invokeParserMethod(openAPIParser, "addSchemasNames");
        openAPIParser.expandOpenApiRefs();

        File solvedSpecification = new File("build/resources/test/specifications/solved/safeRef_solved.json");

        Map<String, Object> parsed = Helper.getParserMap (openAPIParser);
        //Helper.writeJSON("testResults/specifications/solved/ref_solved.json", parsed);
        Gson gson = new Gson();
        Reader reader = Files.newBufferedReader(Paths.get(solvedSpecification.getAbsolutePath()));
        Map<String, Object> rightMap = gson.fromJson(reader, Map.class);

        assertEquals(parsed, rightMap);*/
    }

    @Disabled("Enable after parser is implemented")
    @Test
    public void testRefSolverArray() throws IOException, NoSuchFieldException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        /*logger.info("Test specification ref resolution schemas defining arrays");
        File specification = new File("build/resources/test/specifications/nested_array_ref.json");
        OpenApiParser openAPIParser = new OpenApiParser(Paths.get(specification.getAbsolutePath()));
        Helper.invokeParserMethod(openAPIParser, "addSchemasNames");
        openAPIParser.expandOpenApiRefs();


        File solvedSpecification = new File("build/resources/test/specifications/solved/nested_array_ref_solved.json");
        Map<String, Object> parsed = Helper.getParserMap (openAPIParser);
        Gson gson = new Gson();
        Reader reader = Files.newBufferedReader(Paths.get(solvedSpecification.getAbsolutePath()));
        Map<String, Object> rightMap = gson.fromJson(reader, Map.class);

        assertEquals(rightMap, parsed);*/
    }

    @Disabled("Enable after parser is implemented")
    @Test
    public void testRefSolverItems() throws IOException, NoSuchFieldException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        /*logger.info("Test specification ref resolution for items");
        File specification = new File("build/resources/test/specifications/itemsRef.json");
        OpenApiParser openAPIParser = new OpenApiParser(Paths.get(specification.getAbsolutePath()));
        Helper.invokeParserMethod(openAPIParser, "addSchemasNames");
        openAPIParser.expandOpenApiRefs();

        File solvedSpecification = new File("build/resources/test/specifications/solved/itemsRef_solved.json");

        Map<String, Object> parsed = Helper.getParserMap (openAPIParser);
        Gson gson = new Gson();
        Reader reader = Files.newBufferedReader(Paths.get(solvedSpecification.getAbsolutePath()));
        Map<String, Object> rightMap = gson.fromJson(reader, Map.class);

        assertEquals(rightMap, parsed);*/
    }

    @Disabled("Enable after parser is implemented")
    @Test
    public void testRefSolverRequestBodies() throws IOException, NoSuchFieldException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        /*logger.info("Test specification ref resolution for request bodies");
        File specification = new File("build/resources/test/specifications/requestBodiesRef.json");
        OpenApiParser openAPIParser = new OpenApiParser(Paths.get(specification.getAbsolutePath()));
        Helper.invokeParserMethod(openAPIParser, "addSchemasNames");
        openAPIParser.expandOpenApiRefs();

        File solvedSpecification = new File("build/resources/test/specifications/solved/requestBodiesRef_solved.json");

        Map<String, Object> parsed = Helper.getParserMap (openAPIParser);
        Gson gson = new Gson();
        Reader reader = Files.newBufferedReader(Paths.get(solvedSpecification.getAbsolutePath()));
        Map<String, Object> rightMap = gson.fromJson(reader, Map.class);

        assertEquals(rightMap, parsed);*/
    }

    @Disabled("Enable after parser is implemented")
    @Test
    public void testRefSolverParameters() throws IOException, NoSuchFieldException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        /*logger.info("Test specification ref resolution for parameters");
        File specification = new File("build/resources/test/specifications/parametersRef.json");
        OpenApiParser openAPIParser = new OpenApiParser(Paths.get(specification.getAbsolutePath()));
        Helper.invokeParserMethod(openAPIParser, "addSchemasNames");
        openAPIParser.expandOpenApiRefs();

        File solvedSpecification = new File("build/resources/test/specifications/solved/parametersRef_solved.json");

        Map<String, Object> parsed = Helper.getParserMap (openAPIParser);
        Gson gson = new Gson();
        Reader reader = Files.newBufferedReader(Paths.get(solvedSpecification.getAbsolutePath()));
        Map<String, Object> rightMap = gson.fromJson(reader, Map.class);

        assertEquals(parsed, rightMap);*/
    }

    @Disabled("Enable after parser is implemented")
    @Test
    public void testRefSolverResponse() throws IOException, NoSuchFieldException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        /*logger.info("Test specification ref resolution for responses");
        File specification = new File("build/resources/test/specifications/responseRef.json");
        OpenApiParser openAPIParser = new OpenApiParser(Paths.get(specification.getAbsolutePath()));
        Helper.invokeParserMethod(openAPIParser, "addSchemasNames");
        openAPIParser.expandOpenApiRefs();

        File solvedSpecification = new File("build/resources/test/specifications/solved/responseRef_solved.json");

        Map<String, Object> parsed = Helper.getParserMap (openAPIParser);
        Gson gson = new Gson();
        Reader reader = Files.newBufferedReader(Paths.get(solvedSpecification.getAbsolutePath()));
        Map<String, Object> rightMap = gson.fromJson(reader, Map.class);

        assertEquals(parsed, rightMap);*/
    }

    @Disabled("Enable after parser is implemented")
    @Test
    public void testRefSolverNested() throws IOException, NoSuchFieldException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        /*logger.info("Test specification ref resolution for responses");
        File specification = new File("build/resources/test/specifications/nestedObjectsAndItems.json");
        OpenApiParser openAPIParser = new OpenApiParser(Paths.get(specification.getAbsolutePath()));
        Helper.invokeParserMethod(openAPIParser, "addSchemasNames");
        openAPIParser.expandOpenApiRefs();

        File solvedSpecification = new File("build/resources/test/specifications/solved/nestedObjectsAndItems_solved.json");

        Map<String, Object> parsed = Helper.getParserMap (openAPIParser);
        Gson gson = new Gson();
        Reader reader = Files.newBufferedReader(Paths.get(solvedSpecification.getAbsolutePath()));
        Map<String, Object> rightMap = gson.fromJson(reader, Map.class);

        assertEquals(parsed, rightMap);*/
    }

    @Disabled("Enable after parser is implemented")
    @Test
    public void testRefSolverCombined() throws IOException, NoSuchFieldException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        /*logger.info("Test specification ref resolution for combined schemas (oneOf, anyOf, allOf, not)");
        File specification = new File("build/resources/test/specifications/combined_schemas_ref.json");
        OpenApiParser openAPIParser = new OpenApiParser(Paths.get(specification.getAbsolutePath()));
        Helper.invokeParserMethod(openAPIParser, "addSchemasNames");
        openAPIParser.expandOpenApiRefs();


        File solvedSpecification = new File("build/resources/test/specifications/solved/combined_schemas_ref_solved.json");
        Map<String, Object> parsed = Helper.getParserMap (openAPIParser);
        Gson gson = new Gson();
        Reader reader = Files.newBufferedReader(Paths.get(solvedSpecification.getAbsolutePath()));
        Map<String, Object> rightMap = gson.fromJson(reader, Map.class);

        assertEquals(parsed, rightMap);*/
    }

    @Disabled("Enable after parser is implemented")
    @Test
    public void testRefSolverDeep() throws IOException, NoSuchFieldException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        /*logger.info("Test specification ref resolution for combined schemas (oneOf, anyOf, allOf, not)");
        File specification = new File("build/resources/test/specifications/deep_ref.json");
        OpenApiParser openAPIParser = new OpenApiParser(Paths.get(specification.getAbsolutePath()));
        Helper.invokeParserMethod(openAPIParser, "addSchemasNames");
        openAPIParser.expandOpenApiRefs();


        File solvedSpecification = new File("build/resources/test/specifications/solved/deep_ref_solved.json");
        Map<String, Object> parsed = Helper.getParserMap (openAPIParser);
        Gson gson = new Gson();
        Reader reader = Files.newBufferedReader(Paths.get(solvedSpecification.getAbsolutePath()));
        Map<String, Object> rightMap = gson.fromJson(reader, Map.class);

        assertEquals(parsed, rightMap);*/
    }

    @Disabled("Enable after parser is implemented")
    @Test
    public void testRefSolverPet() throws IOException, NoSuchFieldException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        /*logger.info("Test specification ref resolution for petstore");
        File specification = new File("build/resources/test/specifications/petstore_vuln.json");
        OpenApiParser openAPIParser = new OpenApiParser(Paths.get(specification.getAbsolutePath()));
        Helper.invokeParserMethod(openAPIParser, "addSchemasNames");
        openAPIParser.expandOpenApiRefs();


        File solvedSpecification = new File("build/resources/test/specifications/solved/petstore_vuln_solved.json");

        Map<String, Object> parsed = Helper.getParserMap (openAPIParser);
        Gson gson = new Gson();
        Reader reader = Files.newBufferedReader(Paths.get(solvedSpecification.getAbsolutePath()));
        Map<String, Object> rightMap = gson.fromJson(reader, Map.class);

        assertEquals(parsed, rightMap);*/
    }

    @Disabled("Enable after parser is implemented")
    @Test
    public void testRefSolverAuthentiq() throws IOException, NoSuchFieldException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        /*logger.info("Test specification ref resolution for autentiq");
        File specification = new File("build/resources/test/specifications/authentiq.json");
        OpenApiParser openAPIParser = new OpenApiParser(Paths.get(specification.getAbsolutePath()));
        Helper.invokeParserMethod(openAPIParser, "addSchemasNames");
        openAPIParser.expandOpenApiRefs();

        File solvedSpecification = new File("build/resources/test/specifications/solved/authentiq_solved.json");

        Map<String, Object> parsed = Helper.getParserMap (openAPIParser);
        Gson gson = new Gson();
        Reader reader = Files.newBufferedReader(Paths.get(solvedSpecification.getAbsolutePath()));
        Map<String, Object> rightMap = gson.fromJson(reader, Map.class);

        assertEquals(parsed, rightMap);*/
    }

    @Disabled("Enable after parser is implemented")
    @Test
    public void testRefSolverAEM() throws IOException, NoSuchFieldException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        /*logger.info("Test specification ref resolution for AEM");
        File specification = new File("build/resources/test/specifications/aem.json");
        OpenApiParser openAPIParser = new OpenApiParser(Paths.get(specification.getAbsolutePath()));
        Helper.invokeParserMethod(openAPIParser, "addSchemasNames");
        openAPIParser.expandOpenApiRefs();

        File solvedSpecification = new File("build/resources/test/specifications/solved/aem_solved.json");

        Map<String, Object> parsed = Helper.getParserMap (openAPIParser);
        Gson gson = new Gson();
        Reader reader = Files.newBufferedReader(Paths.get(solvedSpecification.getAbsolutePath()));
        Map<String, Object> rightMap = gson.fromJson(reader, Map.class);

        assertEquals(parsed, rightMap);*/
    }

    @Disabled("Enable after parser is implemented")
    @Test
    public void testRefSolverAWSMigrationHub() throws IOException, NoSuchFieldException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        /*logger.info("Test specification ref resolution for AWS Migration Hub");
        File specification = new File("build/resources/test/specifications/awsMigrationHub.json");
        OpenApiParser openAPIParser = new OpenApiParser(Paths.get(specification.getAbsolutePath()));
        Helper.invokeParserMethod(openAPIParser, "addSchemasNames");
        openAPIParser.expandOpenApiRefs();

        File solvedSpecification = new File("build/resources/test/specifications/solved/awsMigrationHub_solved.json");

        Map<String, Object> parsed = Helper.getParserMap (openAPIParser);
        //Helper.writeJSON("testResults/specifications/solved/ref_solved.json", parsed);
        Gson gson = new Gson();
        Reader reader = Files.newBufferedReader(Paths.get(solvedSpecification.getAbsolutePath()));
        Map<String, Object> rightMap = gson.fromJson(reader, Map.class);

        assertEquals(parsed, rightMap);*/
    }
}
