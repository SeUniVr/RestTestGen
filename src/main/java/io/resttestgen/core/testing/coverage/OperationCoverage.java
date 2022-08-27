package io.resttestgen.core.testing.coverage;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.resttestgen.core.Environment;
import io.resttestgen.core.openapi.Operation;
import io.resttestgen.core.testing.Coverage;
import io.resttestgen.core.testing.TestInteraction;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class OperationCoverage extends Coverage {

    private HashSet<Operation> testedOperations = new HashSet<>();
    private Set<Operation> operationsToBeTested;

    public OperationCoverage(){
        this.operationsToBeTested = Environment.getInstance().getOpenAPI().getOperations();
    }
    @Override
    public void updateCoverage(TestInteraction testInteraction) {
        testedOperations.add(testInteraction.getOperation());
    }

    public double computeCoverage(){
        return ((double)getTested()/(double)getToTest());
    }

    @Override
    public int getTested() {
        return this.testedOperations.size();
    }

    @Override
    public int getToTest() {
        return this.operationsToBeTested.size();
    }

    @Override
    public JsonObject getReportAsJsonObject() {
        JsonObject report = new JsonObject();
        JsonArray documented = new JsonArray(getToTest());
        JsonArray tested = new JsonArray(getTested());
        JsonArray notTested = new JsonArray(getToTest()-getTested());
        for(Operation operation : operationsToBeTested){
            documented.add(operation.toString());
            if(testedOperations.contains(operation)){
                tested.add(operation.toString());
            }else{
                notTested.add(operation.toString());
            }
        }
        report.add("documented",documented);
        report.add("tested", tested);
        report.add("notTested", notTested);
        return report;
    }


}
