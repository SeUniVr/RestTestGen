package io.resttestgen.core.openapi;

import io.resttestgen.boot.ApiUnderTest;
import io.resttestgen.boot.Configuration;
import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.parameter.Parameter;
import io.resttestgen.core.datatype.parameter.structured.StructuredParameter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestRestPath {

    private static Environment e;

    @BeforeAll
    public static void setUp() {
        try {
            Configuration configuration = Configuration.fromFile();
            e = Environment.getInstance();
            e.setUp(configuration, new ApiUnderTest("petstore"));
        } catch (Exception ignored) {}
    }

    @Test
    public void testRestPathForRequestParameters() {
        for (Operation operation: e.getOpenAPI().getOperations()) {
            for (Parameter parameter : operation.getAllRequestParameters()) {
                String restPath = parameter.getRestPath();
                System.out.println(restPath);
                Parameter retrievedParameter = operation.getParameterByRestPath(restPath);
                Assertions.assertEquals(parameter, retrievedParameter);
            }
        }
    }

    @Test
    public void testRestPathForResponseSchemas() {
        for (Operation operation: e.getOpenAPI().getOperations()) {
            for (StructuredParameter schema : operation.getOutputParameters().values()) {
                for (Parameter parameter : schema.getAllParameters()) {
                    String restPath = parameter.getRestPath();
                    System.out.println(restPath);
                    Parameter retrievedParameter = operation.getParameterByRestPath(restPath);
                    Assertions.assertEquals(parameter, retrievedParameter);
                }
            }
        }
    }
}
