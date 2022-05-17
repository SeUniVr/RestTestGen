package io.resttestgen.implementation.operationssorter;

import com.google.common.base.Function;
import com.google.common.collect.Ordering;
import io.resttestgen.core.Environment;
import io.resttestgen.core.helper.ExtendedRandom;
import io.resttestgen.core.openapi.Operation;
import io.resttestgen.core.operationdependencygraph.OperationDependencyGraph;
import io.resttestgen.core.operationdependencygraph.OperationNode;
import io.resttestgen.core.testing.operationsorter.DynamicOperationsSorter;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class GraphBasedOperationsSorter extends DynamicOperationsSorter {

    private static final int MAX_ATTEMPTS = 10;

    private final OperationDependencyGraph graph = Environment.getInstance().getOperationDependencyGraph();
    private final ExtendedRandom random = Environment.getInstance().getRandom();

    public GraphBasedOperationsSorter() {
        refresh();
    }

    @Override
    public Operation removeFirst() {
        Operation removedOperation = super.removeFirst();
        graph.increaseOperationTestingAttempts(removedOperation);
        return removedOperation;
    }

    @Override
    public void refresh() {
        emptyCurrentQueue();

        // Get the nodes in the graph
        List<OperationNode> notTestedNodes = graph.getGraph().vertexSet().stream()

                // Keep only nodes that are not tested and whose testing has been attempted less that MAX_ATTEMPTS times
                .filter(n -> !n.isTested() && n.getTestingAttempts() < MAX_ATTEMPTS)

                // Sort nodes by number of unsatisfied edges
                .sorted(Ordering.natural().onResultOf((Function<OperationNode, Integer>) node ->
                        (int) graph.getGraph().outgoingEdgesOf(node).stream()
                                .filter(dependencyEdge -> !dependencyEdge.isSatisfied()).count()))

                // Sort nodes by number of testing attempts
                .sorted(Comparator.comparing(OperationNode::getTestingAttempts))

                // Sort noted by number of input parameters
                .sorted(Comparator.comparing(n -> n.getOperation().getInputParametersSet().size()))

                .collect(Collectors.toList());

        notTestedNodes.forEach(n -> queue.add(n.getOperation()));
    }

    /**
     * Removes all elements in the queue
     */
    private void emptyCurrentQueue() {
        while (!queue.isEmpty()) {
            queue.removeFirst();
        }
    }
}
