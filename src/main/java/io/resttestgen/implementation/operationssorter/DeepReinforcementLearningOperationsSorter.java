package io.resttestgen.implementation.operationssorter;

import io.resttestgen.core.Environment;
import io.resttestgen.core.helper.DeepReinforcementLearningProxy;
import io.resttestgen.core.openapi.Operation;
import io.resttestgen.core.testing.operationsorter.DynamicOperationsSorter;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DeepReinforcementLearningOperationsSorter extends DynamicOperationsSorter {

    Map<Integer, Operation> operationIds = new HashMap<>();

    public DeepReinforcementLearningOperationsSorter() {
        super();

        // Fill IDs of operations
        int id = 0;
        Set<Operation> operations = Environment.getInstance().getOpenAPI().getOperations();
        for (Operation o : operations) {
            operationIds.put(id, o);
            id++;
        }
    }

    @Override
    public void refresh() {

        // Insert new item only if queue is empty
        if (queue.isEmpty()) {
            queue.add(operationIds.get(DeepReinforcementLearningProxy.getAction()));
        }
    }
}
