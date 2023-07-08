package io.resttestgen.core.operationdependencygraph;

import io.resttestgen.boot.Configuration;
import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.NormalizedParameterName;
import io.resttestgen.core.datatype.parameter.Parameter;
import io.resttestgen.core.openapi.OpenApi;
import io.resttestgen.core.openapi.Operation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DirectedMultigraph;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.DefaultAttribute;
import org.jgrapht.nio.dot.DOTExporter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A graph where nodes represent the operations described in the OpenAPI specification, and edges represent some kind
 * of dependency among these operations. E.g. input/output data dependencies, CRUD dependencies, etc.
 */
public class OperationDependencyGraph {

    private static final Logger logger = LogManager.getLogger(OperationDependencyGraph.class);
    private final Configuration configuration = Environment.getInstance().getConfiguration();

    private final Graph<OperationNode, DependencyEdge> graph = new DirectedMultigraph<>(DependencyEdge.class);

    /**
     * Constructor that builds the Operation Dependency Graph from the provided parsed OpenAPI specification
     * @param openAPI the parsed, valid OpenAPI specification
     */
    public OperationDependencyGraph(OpenApi openAPI) {

        // Get operations from specification and add them to the graph as vertices
        logger.debug("Collecting operations from OpenAPI specification.");
        Set<OperationNode> nodes = new HashSet<>();
        openAPI.getOperations().forEach(operation -> nodes.add(new OperationNode(operation)));
        Graphs.addAllVertices(graph, nodes);

        // Add edges based on the data dependencies described in the specifications
        logger.debug("Extracting data dependencies from OpenAPI specification.");
        extractDataDependencies(openAPI);

        // Save graph to file
        try {
            saveToFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Graph<OperationNode, DependencyEdge> getGraph() {
        return graph;
    }

    public void setOperationAsTested(Operation operation) {
        getNodeFromOperation(operation).setAsTested();
    }

    public void increaseOperationTestingAttempts(Operation operation) {
        getNodeFromOperation(operation).increaseTestingAttempts();
    }


    private void extractDataDependencies(OpenApi openAPI) {

        // Get all the documented operations that have output parameters
        Set<Operation> sourceOperations = openAPI.getOperations().stream()
                .filter(operation -> operation.getOutputParametersSet().size() > 0)
                .collect(Collectors.toSet());

        // For each documented operation
        for (Operation sourceOperation : sourceOperations) {

            // Get all the operations with input parameters, excluding the current source operation
            Set<Operation> targetOperations = openAPI.getOperations().stream()
                    .filter(operation -> operation.getReferenceLeaves().size() > 0).collect(Collectors.toSet());
            targetOperations.remove(sourceOperation);

            // For each combination of "source -> target" operations
            for (Operation targetOperation : targetOperations) {

                // Set of common parameters name for logging and debugging purposes
                Set<NormalizedParameterName> commonParametersNames = new HashSet<>();

                // For each output parameter of source operation
                for (Parameter outputParameter : sourceOperation.getOutputParametersSet()) {
                    if (outputParameter.getNormalizedName() != null) {

                        // For each input parameter of target operation
                        for (Parameter inputParameter : targetOperation.getReferenceLeaves()) {

                            // If input and output parameters have the same normalized name
                            if (outputParameter.getNormalizedName().equals(inputParameter.getNormalizedName())) {

                                DependencyEdge edge = new DependencyEdge(outputParameter, inputParameter);

                                // If the input parameter is not required, this edge can be marked as satisfied
                                /*if (!inputParameter.isRequired()) {
                                    edge.setAsSatisfied();
                                }*/

                                graph.addEdge(getNodeFromOperation(targetOperation), getNodeFromOperation(sourceOperation), edge);
                                commonParametersNames.add(outputParameter.getNormalizedName());
                            }
                        }
                    }
                }

                logger.debug("Dependencies for " + sourceOperation + " -> " + targetOperation + ": " + commonParametersNames);
            }
        }
    }

    /**
     * Get the node in the graph containing the given operation.
     * @param operation the reference operation
     * @return the node in the graph containing the reference operation
     */
    private OperationNode getNodeFromOperation(Operation operation) {
        return graph.vertexSet().stream().filter(operationNode ->
                (operationNode.getOperation().getMethod() == operation.getMethod() &&
                        operationNode.getOperation().getEndpoint().equals(operation.getEndpoint()))).findFirst().get();
    }

    /**
     * Saves the graph to file (only labels, not actual objects).
     * @throws IOException when writing of the file fails.
     */
    public void saveToFile() throws IOException {
        DOTExporter<OperationNode, DependencyEdge> exporter = new DOTExporter<>();
        exporter.setVertexAttributeProvider((v) -> {
            Map<String, Attribute> map = new LinkedHashMap<>();
            map.put("label", DefaultAttribute.createAttribute(v.toString()));
            return map;
        });
        exporter.setEdgeAttributeProvider((e) -> {
            Map<String, Attribute> map = new LinkedHashMap<>();
            map.put("label", DefaultAttribute.createAttribute(e.toString()));
            return map;
        });

        File file = new File(configuration.getOutputPath() + configuration.getTestingSessionName() + "/");
        file.mkdirs();

        Writer writer = new FileWriter(configuration.getOutputPath() + configuration.getTestingSessionName() + "/" +
                configuration.getOdgFileName());
        exporter.exportGraph(this.graph, writer);
        writer.flush();
        writer.close();
    }
}
