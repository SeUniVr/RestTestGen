package io.resttestgen.implementation.coveragemetric;

import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.resttestgen.core.Environment;
import io.resttestgen.core.openapi.Operation;
import io.resttestgen.core.testing.TestInteraction;
import io.resttestgen.core.testing.coverage.Coverage;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class SuccessfulOperationCoverage extends Coverage {

    Set<Operation> documentedOperations;
    Set<Operation> successfullyTestedOperations;

    public SuccessfulOperationCoverage(){
        documentedOperations = Environment.getInstance().getOpenAPI().getOperations();
        successfullyTestedOperations = new HashSet<>();
    }

    @Override
    public void updateCoverage(TestInteraction testInteraction) {
        if (testInteraction.getResponseStatusCode() != null && testInteraction.getResponseStatusCode().isSuccessful()) {
            successfullyTestedOperations.add(testInteraction.getFuzzedOperation());
        }
    }

    @Override
    public int getNumOfTestedDocumented() {
        return 0;
    }

    @Override
    public int getNumOfTestedNotDocumented() {
        return 0;
    }

    @Override
    public int getToTest() {
        return 0;
    }

    @Override
    public JsonObject getReportAsJsonObject() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonObject report = new JsonObject();
        JsonArray successfulOperations = gson.toJsonTree(
                successfullyTestedOperations.stream().map(Operation::toString).collect(Collectors.toSet())
        ).getAsJsonArray();
        report.add("successfulOperations", successfulOperations);
        JsonArray unsuccessfulOperations = gson.toJsonTree(
                Sets.difference(documentedOperations, successfullyTestedOperations)
                        .stream().map(Operation::toString).collect(Collectors.toSet())
        ).getAsJsonArray();
        report.add("unsuccessfulOperations", unsuccessfulOperations);
        return report;
    }
}
