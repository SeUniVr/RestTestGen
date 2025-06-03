package io.resttestgen.core.datatype.parameter;


import io.resttestgen.boot.ApiUnderTest;
import io.resttestgen.boot.Starter;
import io.resttestgen.core.Environment;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

/*
To use this test, set the parameter "specificationFileName" with the openApi "testOpenApi.json" in the json-path test api.
Run Prism with the following command : "prism mock https://Path/To/OpenapiTest/testOpenApi.json -p 4010".
Prism requires NodeJS for the installation: npm install -g @stoplight/prism-cli
 */

public class TestJsonPath {
    private static Environment environment;


    @BeforeAll
    public static void setUp() throws IOException, InterruptedException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        environment = Starter.initEnvironment(ApiUnderTest.loadApiFromFile("petstore"));
    }

    /*@Test
    public void testJsonPaths() {

        for (Operation operation : environment.getOpenAPI().getOperations()) {

            NominalFuzzer nominalFuzzer = new NominalFuzzer(operation);
            List<TestSequence> nominalSequences = nominalFuzzer.generateTestSequences(1);

            for (TestSequence testSequence : nominalSequences) {

                // Run test sequence
                TestRunner testRunner = TestRunner.getInstance();
                testRunner.run(testSequence);

                TestInteraction testInteraction = testSequence.getFirst();
                System.out.println("\n OPERATION: "+testInteraction.getOperation().getOperationId() +"\tBODY_RESPONSE: "+testInteraction.getResponseBody());
                for(ParameterLeaf parameter : testInteraction.getOperation().getResponseBody().getLeaves()){
                    String path = parameter.getJsonPath();


                    Object result = JsonPath.read(testInteraction.getResponseBody(),path);
                    System.out.println("\tPARAMETER: "+parameter +"\tJSON_PATH: "+path +"\tVALUE: "+ result);
                    assertNotNull(result);

                    System.out.println("\n PROVA: "+testInteraction.getOperation().getResponseBody().getElementByJsonPath(path));

                }
            }
        }
    }*/

    @Test
    public void testGetElementByJsonPath() {
        /*for (Operation operation : environment.getOpenAPI().getOperations()) {
            System.out.println("OPERATION: " + operation);
            Parameter requestBody = operation.getRequestBody();
            Parameter responseBody = operation.getOutputParameters().get("200");

            System.out.println("REQUEST:");
            if (requestBody != null) {
                for (LeafParameter leaf : getReferenceLeaves(requestBody)) {
                    String jsonPath = leaf.getJsonPath();
                    System.out.println(jsonPath);
                    Parameter parameter = requestBody.getParameterFromJsonPath(jsonPath);
                    Assertions.assertEquals(leaf, parameter);
                }
            }

            System.out.println("RESPONSE:");
            if (responseBody != null) {
                for (LeafParameter leaf : getReferenceLeaves(responseBody)) {
                    String jsonPath = leaf.getJsonPath();
                    System.out.println(jsonPath);
                    Parameter parameter = responseBody.getParameterFromJsonPath(jsonPath);
                    Assertions.assertEquals(leaf, parameter);
                }
            }
        }*/
    }
}
