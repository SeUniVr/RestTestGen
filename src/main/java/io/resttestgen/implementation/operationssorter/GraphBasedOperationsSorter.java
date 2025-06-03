package io.resttestgen.implementation.operationssorter;

import com.google.common.collect.Sets;
import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.NormalizedParameterName;
import io.resttestgen.core.datatype.parameter.leaves.LeafParameter;
import io.resttestgen.core.helper.ExtendedRandom;
import io.resttestgen.core.openapi.Operation;
import io.resttestgen.core.operationdependencygraph.DependencyEdge;
import io.resttestgen.core.operationdependencygraph.OperationDependencyGraph;
import io.resttestgen.core.operationdependencygraph.OperationNode;
import io.resttestgen.core.testing.operationsorter.DynamicOperationsSorter;
import io.resttestgen.core.testing.parametervalueprovider.ParameterValueProviderCachedFactory;
import io.resttestgen.implementation.parametervalueprovider.ParameterValueProviderType;
import io.resttestgen.implementation.parametervalueprovider.single.ResponseDictionaryParameterValueProvider;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class GraphBasedOperationsSorter extends DynamicOperationsSorter {

    private int maximumAttempts = 10;

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
                .filter(n -> !n.isTested() && n.getTestingAttempts() < maximumAttempts)

                // Sort nodes by number of unsatisfied edges, collapsing edges with the same name
                .sorted(Comparator.comparing(this::computeNumberOfUnsatisfiedParameters))

                // Sort by operation HTTP method (CRUD semantics)
                .sorted(Comparator.comparing(n -> n.getOperation().getMethod()))

                // Sort nodes by number of testing attempts
                .sorted(Comparator.comparing(OperationNode::getTestingAttempts))

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

    /**
     * Compute the number of unsatisfied parameters by subtracting the set of satisfied parameters from the set of total
     * parameters in the operation. Moreover, removes the parameters that are not in the graph, but have a value stored
     * in the global dictionary.
     * @param node the node in the operation dependency graph.
     * @return the number of unsatisfied parameters.
     */
    private int computeNumberOfUnsatisfiedParameters(OperationNode node) {
        Set<NormalizedParameterName> satisfiedParameters = graph.getGraph().outgoingEdgesOf(node).stream()
                .filter(DependencyEdge::isSatisfied)
                .map(DependencyEdge::getNormalizedName)
                .collect(Collectors.toSet());
        Set<NormalizedParameterName> allParametersInOperation = node.getOperation().getReferenceLeaves().stream()
                .map(LeafParameter::getNormalizedName)
                .collect(Collectors.toSet());
        Set<NormalizedParameterName> unsatisfiedParameters = Sets.difference(allParametersInOperation, satisfiedParameters);

        ResponseDictionaryParameterValueProvider provider = (ResponseDictionaryParameterValueProvider) ParameterValueProviderCachedFactory.getParameterValueProvider(ParameterValueProviderType.RESPONSE_DICTIONARY);
        provider.setSameNormalizedNameValueSourceClass();

        Set<NormalizedParameterName> parametersInDictionary = new HashSet<>();
        for (NormalizedParameterName unsatisfiedParameter : unsatisfiedParameters) {
            List<LeafParameter> foundParameters = node.getOperation().searchReferenceRequestParametersByNormalizedName(unsatisfiedParameter)
                    .stream().filter(p -> p instanceof LeafParameter).map(p -> (LeafParameter) p).collect(Collectors.toList());
            if (!foundParameters.isEmpty()) {
                LeafParameter parameter = foundParameters.get(0);
                if (provider.countAvailableValuesFor(parameter) > 0) {
                    parametersInDictionary.add(unsatisfiedParameter);
                }
            }
        }
        return Sets.difference(unsatisfiedParameters, parametersInDictionary).size();
    }

    public int getMaximumAttempts() {
        return maximumAttempts;
    }

    public void setMaximumAttempts(int maximumAttempts) {
        if (maximumAttempts < 1) {
            throw new IllegalArgumentException("The number of maximum attempts must be greater or equal to 1.");
        }
        this.maximumAttempts = maximumAttempts;
    }
}
