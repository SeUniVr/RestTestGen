package io.resttestgen.implementation.strategy.support;

import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.parameter.Parameter;
import io.resttestgen.core.openapi.Operation;
import io.resttestgen.core.testing.Strategy;

public class RqOneAndTwoExtractionStrategy extends Strategy {

    StringBuilder csv = new StringBuilder();

    private String openapiName = Environment.getInstance().getConfiguration().getTestingSessionName()
            .substring(0, Environment.getInstance().getConfiguration().getTestingSessionName().length() - 18);

    @Override
    public void start() {

        // For each operation in the specification
        for (Operation operation : Environment.getInstance().getOpenAPI().getOperations()) {

            // If operation has description, add it
            if (operation.getDescription() != null && operation.getDescription().length() > 3) {
                addDescriptionToMap(operation.toString(), "#operationdescription", operation.getDescription());
            }

            if (operation.getRequestBodyDescription() != null && operation.getRequestBodyDescription().length() > 3) {
                addDescriptionToMap(operation.toString(), "#requestdescription", operation.getRequestBodyDescription());
            }

            // For each parameter in the specification, if it has description, add it
            for (Parameter parameter : operation.getAllRequestParameters()) {
                if (parameter.getDescription() != null && parameter.getDescription().length() > 3) {
                    addDescriptionToMap(operation.toString(), parameter.getName().toString(), parameter.getDescription());
                }
            }
        }

        System.out.println(csv);

    }

    private void addDescriptionToMap(String operation, String parameterName, String description) {
        description = description.replace("\"", "'").replace("\n", "\\n");
        csv.append("\"").append(openapiName).append("\",\"\",\"\",\"").append(operation).append("\",\"").append(parameterName).append("\",\"").append(description).append("\"\n");
    }
}
