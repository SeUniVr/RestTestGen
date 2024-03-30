package io.resttestgen.implementation.interactionprocessor;

import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.parameter.leaves.LeafParameter;
import io.resttestgen.core.datatype.parameter.structured.StructuredParameter;
import io.resttestgen.core.operationdependencygraph.DependencyEdge;
import io.resttestgen.core.operationdependencygraph.OperationDependencyGraph;
import io.resttestgen.core.testing.InteractionProcessor;
import io.resttestgen.core.testing.TestInteraction;
import io.resttestgen.core.testing.TestStatus;

import java.util.List;
import java.util.stream.Collectors;

import static io.resttestgen.core.datatype.parameter.ParameterUtils.getLeaves;

public class GraphInteractionProcessor extends InteractionProcessor {

    final OperationDependencyGraph operationDependencyGraph = Environment.getInstance().getOperationDependencyGraph();

    @Override
    public boolean canProcess(TestInteraction testInteraction) {
        return testInteraction.getTestStatus() == TestStatus.EXECUTED && testInteraction.getResponseStatusCode().isSuccessful();
    }

    @Override
    public void process(TestInteraction testInteraction) {

        operationDependencyGraph.setOperationAsTested(testInteraction.getFuzzedOperation());

        StructuredParameter responseBody = testInteraction.getFuzzedOperation().getResponseBody();

        // If the response body is still null, terminate the processing of the response
        if (responseBody == null) {
            return;
        }

        // Iterate on leaves to update the graph
        for (LeafParameter leaf : getLeaves(responseBody)) {
            List<DependencyEdge> satisfiedEdges = operationDependencyGraph.getGraph().edgeSet().stream()
                    .filter(edge -> edge.getNormalizedName().equals(leaf.getNormalizedName())).collect(Collectors.toList());
            for (DependencyEdge satisfiedEdge : satisfiedEdges) {
                satisfiedEdge.setAsSatisfied();
            }
        }
    }
}
