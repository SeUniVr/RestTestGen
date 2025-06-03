package io.resttestgen.core.operationdependencygraph;

import io.resttestgen.boot.ApiUnderTest;
import io.resttestgen.boot.Starter;
import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.NormalizedParameterName;
import io.resttestgen.core.datatype.parameter.Parameter;
import io.resttestgen.core.openapi.InvalidOpenApiException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class  OperationDependencyGraphTest {

    private static Environment environment;

    @BeforeAll
    public static void setUp() throws InvalidOpenApiException, IOException, InterruptedException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        environment = Starter.initEnvironment(ApiUnderTest.loadApiFromFile("petstore"));
    }

    @Test
    // The number of the nodes should be equal to the number of the operations
    public void testNodes() {
        assertEquals(environment.getOperationDependencyGraph().getGraph().vertexSet().size(),
                environment.getOpenAPI().getOperations().size());
    }

    @Test
    // The name of parameters on edges should be in the parameters of operations they link
    public void testEdges() {
        for (DependencyEdge edge : environment.getOperationDependencyGraph().getGraph().edgeSet()) {

            Set<NormalizedParameterName> parametersNormalizedNames = new HashSet<>();
            for (Parameter parameter : environment.getOperationDependencyGraph().getGraph().getEdgeSource(edge).getOperation().getReferenceLeaves()) {
                if (parameter.getNormalizedName() != null) {
                    parametersNormalizedNames.add(parameter.getNormalizedName());
                }
            }
            assertTrue(parametersNormalizedNames.contains(edge.getNormalizedName()));

            parametersNormalizedNames = new HashSet<>();
            for (Parameter parameter : environment.getOperationDependencyGraph().getGraph().getEdgeTarget(edge).getOperation().getOutputParametersSet()) {
                if (parameter.getNormalizedName() != null) {
                    parametersNormalizedNames.add(parameter.getNormalizedName());
                }
            }
            assertTrue(parametersNormalizedNames.contains(edge.getNormalizedName()));
        }
    }
}
