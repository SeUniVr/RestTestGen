package io.resttestgen.core.openapi;

import com.google.gson.Gson;
import io.resttestgen.core.datatype.NormalizedParameterName;
import io.resttestgen.core.datatype.parameter.ParameterElement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class TestOpenAPIParser {

    private static final Logger logger = LogManager.getLogger(TestOpenAPIParser.class);

    @BeforeAll
    public static void setNormalizer() {
        Helper.setNormalizer();
    }

    @Test
    public void testPetstoreNormalizer() throws InvalidOpenAPIException, CannotParseOpenAPIException {
        logger.info("Test petstore normalizer");
        File specification = new File("build/resources/test/specifications/petstore_vuln.json");
        OpenAPI openAPI = new OpenAPIParser(Paths.get(specification.getAbsolutePath())).parse();

        Set<ParameterElement> params = new HashSet<>();
        for (Operation op : openAPI.getOperations()) {
            params.addAll(op.getReferenceLeaves());
            params.addAll(op.getOutputParametersSet());
        }

        Set<NormalizedParameterName> norm = new HashSet<>();
        for (ParameterElement p : params) {
            norm.add(p.getNormalizedName());
        }

        logger.debug(norm);

        assertTrue(norm.contains(new NormalizedParameterName("PetId")));
        assertTrue(norm.contains(new NormalizedParameterName("PetName")));
        assertTrue(norm.contains(new NormalizedParameterName("TagId")));
        assertTrue(norm.contains(new NormalizedParameterName("TagName")));
        assertTrue(norm.contains(new NormalizedParameterName("CategoriId")));
        assertTrue(norm.contains(new NormalizedParameterName("CategoriName")));
        assertTrue(norm.contains(new NormalizedParameterName("PhotoUrl")));
        assertTrue(norm.contains(new NormalizedParameterName("Statu")));
    }

    @Test
    public void testParameterAttributesNormalization() throws IOException, NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, CannotParseOpenAPIException {
        logger.info("Test normalization of parameters declared at path item level");

        File specification = new File("build/resources/test/specifications/commonParameters.json");
        OpenAPIParser openAPIParser = new OpenAPIParser(Paths.get(specification.getAbsolutePath()));

        Helper.invokeParserMethod(openAPIParser, "addSchemasNames");
        Helper.invokeParserMethod(openAPIParser, "solveOpenAPIrefs");
        Helper.invokeParserMethod(openAPIParser, "normalizeCommonParameters");

        File solvedSpecification = new File("build/resources/test/specifications/solved/commonParameters_normalized.json");

        Map<String, Object> parsed = Helper.getParserMap(openAPIParser);
        Gson gson = new Gson();
        Reader reader = Files.newBufferedReader(Paths.get(solvedSpecification.getAbsolutePath()));
        Map<String, Object> rightMap = gson.fromJson(reader, Map.class);

        assertEquals(parsed, rightMap);

    }

    @Test
    public void testParameterRequiredNormalization() throws IOException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, CannotParseOpenAPIException, NoSuchFieldException {
        logger.info("Test normalization of required attribute declared as list of names in objects");

        File specification = new File("build/resources/test/specifications/required.json");
        OpenAPIParser openAPIParser = new OpenAPIParser(Paths.get(specification.getAbsolutePath()));

        Helper.invokeParserMethod(openAPIParser, "addSchemasNames");
        Helper.invokeParserMethod(openAPIParser, "solveOpenAPIrefs");
        Helper.invokeParserMethod(openAPIParser, "unfoldRequiredAttributes");

        File solvedSpecification = new File("build/resources/test/specifications/solved/required_solved.json");

        Map<String, Object> parsed = Helper.getParserMap (openAPIParser);
        Gson gson = new Gson();
        Reader reader = Files.newBufferedReader(Paths.get(solvedSpecification.getAbsolutePath()));
        Map<String, Object> rightMap = gson.fromJson(reader, Map.class);

        assertEquals(rightMap, parsed);
    }

    @Test
    public void testParsingErrors() throws CannotParseOpenAPIException, NoSuchMethodException {
        logger.info("Test right exceptions are thrown by parser.");

        assertThrows(CannotParseOpenAPIException.class, () -> new OpenAPIParser(Paths.get("/not/a/valid/path.json")));
        assertThrows(CannotParseOpenAPIException.class, () -> new OpenAPIParser(null));

        File notAJSON = new File("build/resources/test/specifications/not_valid/not_a_json.txt");
        assertThrows(CannotParseOpenAPIException.class, () ->
                new OpenAPIParser(Paths.get(notAJSON.getAbsolutePath()))
        );

        File notASpec = new File("build/resources/test/specifications/not_valid/not_a_spec.txt");
        assertThrows(CannotParseOpenAPIException.class, () ->
                new OpenAPIParser(Paths.get(notASpec.getAbsolutePath()))
        );

        File noValidServer = new File("build/resources/test/specifications/not_valid/no_valid_server.json");
        assertThrows(InvalidOpenAPIException.class, () ->
                new OpenAPIParser(Paths.get(noValidServer.getAbsolutePath())).parse()
        );

        assertDoesNotThrow(() -> new OpenAPIParser(Paths.get(noValidServer.getAbsolutePath())));
        OpenAPIParser openAPIParser = new OpenAPIParser(Paths.get(noValidServer.getAbsolutePath()));
        Method getElementByRef = OpenAPIParser.class.getDeclaredMethod("getElementByRef", String.class);
        getElementByRef.setAccessible(true);
        assertDoesNotThrow(() -> getElementByRef.invoke(openAPIParser, "#/components/schemas/ASchema"));
        // Need to check for InvocationTargetException since it blocks InvalidOpenAPIException
        assertThrows(InvocationTargetException.class,
                () -> getElementByRef.invoke(openAPIParser, "#/components/schemas/BSchema"));
    }

    @Test
    public void testTypeInference() throws IOException, NoSuchFieldException, IllegalAccessException, CannotParseOpenAPIException {
        logger.info("Test specification type inference");
        File specification = new File("build/resources/test/specifications/type_inference.json");
        OpenAPIParser openAPIParser = new OpenAPIParser(Paths.get(specification.getAbsolutePath()));
        openAPIParser.parse();

        File solvedSpecification = new File("build/resources/test/specifications/solved/type_inference_solved.json");
        Map<String, Object> parsed = Helper.getParserMap (openAPIParser);
        Gson gson = new Gson();
        Reader reader = Files.newBufferedReader(Paths.get(solvedSpecification.getAbsolutePath()));
        Map<String, Object> rightMap = gson.fromJson(reader, Map.class);

        assertEquals(rightMap, parsed);
    }

    @Test
    public void test20Spec() throws InvalidOpenAPIException {
        List<String> specNames = new LinkedList<>(Arrays.asList(new String[]{
                /*"realworld",
                "atmosphere",
                "apisguru",
                "zoom",
                "gitlab_3_trimmed",
                "whois",
                "powertools",
                "jira",
                "bbc_nitro",
                //"bigred", // recursion found -> causes a NullPointerException
                "billingo",
                "bitbucket", // recursion found
                "ibmcontainers",
                "circleci",
                "configcat",
                "dockerengine",
                "exavault",
                //"gambitcomm", // response as json, but with type string (line 88)
                "github",
                "postman",
                "googlecalendar",
                "kubernetes",
                "justeatuk",*/
        }));

        for (String name : specNames) {
            logger.info("Testing " + name);
            try {
                File specification = new File("testResults/test20/" + name + ".json");
                OpenAPIParser openAPIParser = new OpenAPIParser(Paths.get(specification.getAbsolutePath()));
                openAPIParser.parse();

                File outSpec = new File("testResults/test20/solved/" + name + "_solved.json"); // The folders have to exist
                //Helper.writeJSON(outSpec.getAbsolutePath(), Helper.getParserMap(openAPIParser));

                Map<String, Object> parsed = Helper.getParserMap(openAPIParser);
                Gson gson = new Gson();
                Reader reader = Files.newBufferedReader(Paths.get(outSpec.getAbsolutePath()));
                Map<String, Object> rightMap = gson.fromJson(reader, Map.class);

                assertEquals(rightMap, parsed);

            } catch (Error | CannotParseOpenAPIException | NoSuchFieldException | IllegalAccessException | IOException e) {
                logger.error("Specification " + name + " not found.");
            }

        }
    }

}