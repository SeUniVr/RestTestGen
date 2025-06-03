package io.resttestgen.core.datatype.parameter;

import io.resttestgen.boot.ApiUnderTest;
import io.resttestgen.boot.Starter;
import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.HttpMethod;
import io.resttestgen.core.datatype.parameter.leaves.LeafParameter;
import io.resttestgen.core.openapi.Operation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.stream.Collectors;

public class TestRemove {

    private static Environment environment;

    @BeforeAll
    public static void setUp() throws IOException, InterruptedException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        environment = Starter.initEnvironment(ApiUnderTest.loadApiFromFile("petstore"));
    }

    @Test
    public void testRemove1() {
        Operation operation = environment.getOpenAPI().getOperations().stream().filter(o -> o.getMethod() == HttpMethod.PUT).collect(Collectors.toList()).get(0).deepClone();
        int inizialSize = operation.getAllRequestParameters().size();
        System.out.println(operation + ": " + operation.getAllRequestParameters().size() + " parameters.");
        for (LeafParameter leaf : operation.getLeaves()) {
            leaf.remove();
            break;
        }
        System.out.println(operation + ": " + operation.getAllRequestParameters().size() + " parameters.");
        Assertions.assertEquals(inizialSize - 1, operation.getAllRequestParameters().size());
    }

    @Test
    public void testRemove2() {
        Operation operation = environment.getOpenAPI().getOperations().stream().filter(o -> o.getMethod() == HttpMethod.PUT).collect(Collectors.toList()).get(0).deepClone();
        System.out.println(operation + ": " + operation.getAllRequestParameters().size() + " parameters.");
        operation.getRequestBody().remove();
        System.out.println(operation + ": " + operation.getAllRequestParameters().size() + " parameters.");
        Assertions.assertEquals(0, operation.getAllRequestParameters().size());
    }
}
