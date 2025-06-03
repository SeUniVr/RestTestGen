package io.resttestgen.core.openapi;

import com.google.gson.Gson;

import io.resttestgen.boot.ApiUnderTest;
import io.resttestgen.core.datatype.NormalizedParameterName;
import io.resttestgen.core.datatype.parameter.Parameter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class TestOpenApiParser {

    private static final Logger logger = LogManager.getLogger(TestOpenApiParser.class);

    @BeforeAll
    public static void setNormalizer() {
        Helper.setNormalizer();
    }

    @Test
    public void testPetstoreNormalizer() throws InvalidOpenApiException, IOException {
        logger.info("Testing PetStore parameter name normalizer.");
        OpenApi openAPI = OpenApiParser.parse(ApiUnderTest.loadApiFromFile("petstore-vuln"));

        Set<Parameter> params = new HashSet<>();
        for (Operation op : openAPI.getOperations()) {
            params.addAll(op.getReferenceLeaves());
            params.addAll(op.getOutputParametersSet());
        }

        Set<NormalizedParameterName> norm = new HashSet<>();
        for (Parameter p : params) {
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

    @Disabled("Enable after parser is implemented")
    @Test
    public void testParameterAttributesNormalization() throws IOException, NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        /*logger.info("Test normalization of parameters declared at path item level");

        OpenApiParser openAPIParser = new OpenApiParser(ApiUnderTest.loadTestApiFromFile("common-parameters"));

        Helper.invokeParserMethod(openAPIParser, "addSchemasNames");
        Helper.invokeParserMethod(openAPIParser, "expandOpenApiRefs");
        Helper.invokeParserMethod(openAPIParser, "unfoldPathSharedParameters");

        File solvedSpecification = new File("apis/.test-apis/common-parameters/specifications/normalized.json");

        Map<String, Object> parsed = Helper.getParserMap(openAPIParser);
        Gson gson = new Gson();
        Reader reader = Files.newBufferedReader(Paths.get(solvedSpecification.getAbsolutePath()));
        Map<String, Object> rightMap = gson.fromJson(reader, Map.class);

        assertEquals(parsed, rightMap);*/
    }

    @Disabled("Enable after parser is implemented")
    @Test
    public void testParameterRequiredNormalization() throws IOException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, NoSuchFieldException {
        /*logger.info("Test normalization of required attribute declared as list of names in objects");

        OpenApiParser openAPIParser = new OpenApiParser(ApiUnderTest.loadTestApiFromFile("required"));

        Helper.invokeParserMethod(openAPIParser, "addSchemasNames");
        Helper.invokeParserMethod(openAPIParser, "expandOpenApiRefs");
        Helper.invokeParserMethod(openAPIParser, "unfoldRequiredAttributes");

        File solvedSpecification = new File("apis/.test-apis/required/specifications/solved.json");

        Map<String, Object> parsed = Helper.getParserMap (openAPIParser);
        Gson gson = new Gson();
        Reader reader = Files.newBufferedReader(Paths.get(solvedSpecification.getAbsolutePath()));
        Map<String, Object> rightMap = gson.fromJson(reader, Map.class);

        assertEquals(rightMap, parsed);*/
    }

    @Disabled("Enable after parser is implemented")
    @Test
    public void testParsingErrors() throws NoSuchMethodException, IOException {
        /*logger.info("Test right exceptions are thrown by parser.");

        assertThrows(NullPointerException.class, () -> new OpenApiParser((ApiUnderTest) null));

        assertThrows(CannotParseOpenApiException.class, () ->
                new OpenApiParser(ApiUnderTest.loadTestApiFromFile("not-a-json"))
        );

        assertThrows(InvalidOpenApiException.class, () ->
                new OpenApiParser(ApiUnderTest.loadTestApiFromFile("not-a-spec")).parse()
        );

        assertThrows(InvalidOpenApiException.class, () ->
                new OpenApiParser(ApiUnderTest.loadTestApiFromFile("no-valid-server")).parse()
        );

        assertDoesNotThrow(() -> new OpenApiParser(ApiUnderTest.loadTestApiFromFile("no-valid-server")));
        OpenApiParser openAPIParser = new OpenApiParser(ApiUnderTest.loadTestApiFromFile("no-valid-server"));
        Method getElementByRef = OpenApiParser.class.getDeclaredMethod("getElementCloneByRef", String.class);
        getElementByRef.setAccessible(true);
        assertDoesNotThrow(() -> getElementByRef.invoke(openAPIParser, "#/components/schemas/ASchema"));
        // Need to check for InvocationTargetException since it blocks InvalidOpenAPIException
        assertThrows(InvocationTargetException.class,
                () -> getElementByRef.invoke(openAPIParser, "#/components/schemas/BSchema"));*/
    }

    @Disabled("Enable after parser is implemented")
    @Test
    public void testTypeInference() throws IOException, NoSuchFieldException, IllegalAccessException {
        /*logger.info("Test specification type inference");
        OpenApiParser openApiParser = new OpenApiParser(ApiUnderTest.loadTestApiFromFile("type-inference"));
        openApiParser.parse();

        File solvedSpecification = new File("apis/.test-apis/type-inference/specifications/solved.json");
        Map<String, Object> parsed = Helper.getParserMap(openApiParser);
        Gson gson = new Gson();
        Reader reader = Files.newBufferedReader(Paths.get(solvedSpecification.getAbsolutePath()));
        Map<String, Object> rightMap = gson.fromJson(reader, Map.class);

        assertEquals(rightMap, parsed);*/
    }

    @Disabled("Enable after parser is implemented")
    @Test
    public void test20Spec() throws InvalidOpenApiException, IOException {
        /*List<String> testApisWildcards = new LinkedList<>(Arrays.asList(
                "realworld",
                "atmosphere",
                "apis-guru",
                //"zoom",
                //"gitlab-3-trimmed",
                //"whois",
                //"powertools",
                //"jira",
                //"bbc-nitro", // too long processing time
                //"bigred", // recursion found -> causes a NullPointerException
                "billingo",
                //"bitbucket", // recursion found
                //"ibm-containers",
                "circleci",
                "configcat",
                "docker-engine",
                "exavault",
                //"gambitcomm", // response as json, but with type string (line 88)
                //"github",
                "postman",
                "google-calendar",
                "kubernetes",
                "justeat-uk"));

        for (String wildcard : testApisWildcards) {

            ApiUnderTest apiUnderTest = ApiUnderTest.loadTestApiFromFile(wildcard);

            logger.info("Testing {}.", apiUnderTest.getName());

            try {
                OpenApiParser openApiParser = new OpenApiParser(apiUnderTest);
                openApiParser.parseOld();

                File outSpec = new File("./apis/.test-apis/" + wildcard + "/specifications/solved.json"); // The folders have to exist
                //Helper.writeJSON(outSpec.getAbsolutePath(), Helper.getParserMap(openApiParser));

                Map<String, Object> parsed = Helper.getParserMap(openApiParser);
                Gson gson = new Gson();
                Reader reader = Files.newBufferedReader(Paths.get(outSpec.getAbsolutePath()));
                Map<String, Object> rightMap = gson.fromJson(reader, Map.class);

                assertEquals(rightMap, parsed);

            } catch (NoSuchFieldException | IllegalAccessException | IOException e) {
                logger.error("Specification {} not usable. {}.", wildcard, e.getMessage().substring(0, 100) );
            }

        }*/
    }

}