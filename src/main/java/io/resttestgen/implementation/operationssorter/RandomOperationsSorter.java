package io.resttestgen.implementation.operationssorter;

import io.resttestgen.core.Environment;
import io.resttestgen.core.testing.StaticOperationsSorter;

import java.util.Collections;
import java.util.LinkedList;

public class RandomOperationsSorter extends StaticOperationsSorter {

    public RandomOperationsSorter(Environment environment) {
        super(environment);
        queue = new LinkedList<>(environment.openAPI.getOperations());
        Collections.shuffle(queue);
    }
}
