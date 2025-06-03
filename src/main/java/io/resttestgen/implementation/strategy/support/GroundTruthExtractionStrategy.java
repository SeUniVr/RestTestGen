package io.resttestgen.implementation.strategy.support;

import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.parameter.Parameter;
import io.resttestgen.core.openapi.Operation;
import io.resttestgen.core.testing.Strategy;

import java.util.HashMap;

@SuppressWarnings("unused")
public class GroundTruthExtractionStrategy extends Strategy {

    private final String openapiName = Environment.getInstance().getConfiguration().getTestingSessionName()
            .substring(0, Environment.getInstance().getConfiguration().getTestingSessionName().length() - 18);

    private final HashMap<String, Integer> collapseCountMap = new HashMap<>();

    @Override
    public void start() {

        // For each operation in the specification
        for (Operation operation : Environment.getInstance().getOpenAPI().getOperations()) {

            // If the operation has a description, add it
            if (operation.getDescription() != null && operation.getDescription().length() > 3) {
                addDescriptionToMap("#operationdescription", operation.getDescription());
            }

            if (operation.getRequestBodyDescription() != null && operation.getRequestBodyDescription().length() > 3) {
                addDescriptionToMap("#requestbodydescription", operation.getRequestBodyDescription());
            }

            // For each parameter in the specification, if it has a description, add it
            for (Parameter parameter : operation.getAllRequestParameters()) {
                if (parameter.getDescription() != null && parameter.getDescription().length() > 3) {
                    addDescriptionToMap(parameter.getName().toString(), parameter.getDescription());
                }
            }
        }

        StringBuilder csv = new StringBuilder();
        for (String description : collapseCountMap.keySet()) {
            csv.append("\"").append(openapiName).append("\",\"\",\"\",\"").append(collapseCountMap.get(description))
                    .append("\",\"").append(description).append("\"\n");
        }

        System.out.println(csv);

    }

    private void addDescriptionToMap(String parameterName, String description) {
        String key = parameterName + "\",\"" + description.replace("\"", "'").replace("\n", "\\n");
        collapseCountMap.merge(key, 1, Integer::sum);
    }
}
