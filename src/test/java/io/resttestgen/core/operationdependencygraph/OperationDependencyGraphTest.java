package io.resttestgen.core.operationdependencygraph;

import io.resttestgen.core.Configuration;
import io.resttestgen.core.Environment;
import io.resttestgen.core.TestingEnvironmentGenerator;
import io.resttestgen.core.openapi.CannotParseOpenAPIException;
import io.resttestgen.core.openapi.InvalidOpenAPIException;
import io.resttestgen.core.openapi.OpenAPI;
import io.resttestgen.core.datatype.NormalizedParameterName;
import io.resttestgen.core.datatype.parameter.ParameterElement;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class  OperationDependencyGraphTest {

    private static Configuration configuration;
    private static Environment environment;
    private static OpenAPI openAPI;
    private static OperationDependencyGraph operationDependencyGraph;

    @BeforeAll
    public static void setUp() throws InvalidOpenAPIException, CannotParseOpenAPIException {
        environment = TestingEnvironmentGenerator.getTestingEnvironment();
        openAPI = environment.openAPI;
        operationDependencyGraph = environment.operationDependencyGraph;
    }

    @Test
    // The number of the nodes should be equal to the number of the operations
    public void testNodes() {
        assertEquals(operationDependencyGraph.getGraph().vertexSet().size(), openAPI.getOperations().size());
    }

    @Test
    // The name of parameters on edges should be in the parameters of operations they link
    public void testEdges() {
        for (DependencyEdge edge : operationDependencyGraph.getGraph().edgeSet()) {

            Set<NormalizedParameterName> parametersNormalizedNames = new HashSet<>();
            for (ParameterElement parameter : operationDependencyGraph.getGraph().getEdgeSource(edge).getOperation().getInputParametersSet()) {
                if (parameter.getNormalizedName() != null) {
                    parametersNormalizedNames.add(parameter.getNormalizedName());
                }
            }
            assertTrue(parametersNormalizedNames.contains(edge.getNormalizedName()));

            parametersNormalizedNames = new HashSet<>();
            for (ParameterElement parameter : operationDependencyGraph.getGraph().getEdgeTarget(edge).getOperation().getOutputParametersSet()) {
                if (parameter.getNormalizedName() != null) {
                    parametersNormalizedNames.add(parameter.getNormalizedName());
                }
            }
            assertTrue(parametersNormalizedNames.contains(edge.getNormalizedName()));
        }
    }
}
