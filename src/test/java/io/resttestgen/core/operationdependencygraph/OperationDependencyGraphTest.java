package io.resttestgen.core.operationdependencygraph;

import io.resttestgen.core.Configuration;
import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.NormalizedParameterName;
import io.resttestgen.core.datatype.parameter.ParameterElement;
import io.resttestgen.core.openapi.CannotParseOpenAPIException;
import io.resttestgen.core.openapi.InvalidOpenAPIException;
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
    public static void setUp() throws InvalidOpenAPIException, CannotParseOpenAPIException, IOException, InterruptedException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        /*environment = TestingEnvironmentGenerator.getTestingEnvironment();
        openAPI = environment.getOpenAPI();
        operationDependencyGraph = environment.getOperationDependencyGraph();*/
        Configuration configuration = new Configuration(true);
        environment = Environment.getInstance();
        environment.setUp(configuration);
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
            for (ParameterElement parameter : environment.getOperationDependencyGraph().getGraph().getEdgeSource(edge).getOperation().getReferenceLeaves()) {
                if (parameter.getNormalizedName() != null) {
                    parametersNormalizedNames.add(parameter.getNormalizedName());
                }
            }
            assertTrue(parametersNormalizedNames.contains(edge.getNormalizedName()));

            parametersNormalizedNames = new HashSet<>();
            for (ParameterElement parameter : environment.getOperationDependencyGraph().getGraph().getEdgeTarget(edge).getOperation().getOutputParametersSet()) {
                if (parameter.getNormalizedName() != null) {
                    parametersNormalizedNames.add(parameter.getNormalizedName());
                }
            }
            assertTrue(parametersNormalizedNames.contains(edge.getNormalizedName()));
        }
    }
}
