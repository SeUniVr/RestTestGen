package io.resttestgen.core.testing.coverage;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.resttestgen.core.Environment;
import io.resttestgen.core.openapi.Operation;
import io.resttestgen.core.testing.Coverage;
import io.resttestgen.core.testing.TestInteraction;

import java.util.HashSet;
import java.util.Set;

public class OperationCoverage extends Coverage {

    private HashSet<Operation> operationsDocumentedTested= new HashSet<>();
    private HashSet<Operation> operationsNotDocumentedTested = new HashSet<>();
    private Set<Operation> operationsToTest;

    public OperationCoverage(){
        this.operationsToTest = Environment.getInstance().getOpenAPI().getOperations();
    }
    @Override
    public void updateCoverage(TestInteraction testInteraction) {
        if(operationsToTest.contains(testInteraction.getFuzzedOperation())){
            operationsDocumentedTested.add(testInteraction.getFuzzedOperation());
        }else{
            operationsNotDocumentedTested.add(testInteraction.getFuzzedOperation());
        }
    }

    @Override
    public int getNumOfTestedDocumented(){
        return this.operationsDocumentedTested.size();
    }

    @Override
    public int getNumOfTestedNotDocumented(){
        return this.operationsNotDocumentedTested.size();
    }
    @Override
    public int getToTest() {
        return this.operationsToTest.size();
    }

    @Override
    public JsonObject getReportAsJsonObject() {
        JsonObject report = new JsonObject();
        JsonArray documented = new JsonArray(getToTest());
        JsonArray documentedTested = new JsonArray(getNumOfTestedDocumented());
        JsonArray notDocumentedTested = new JsonArray(getNumOfTestedNotDocumented());
        JsonArray notTested = new JsonArray(getToTest()-getNumOfTestedDocumented());

        for(Operation operation : operationsToTest){
            documented.add(operation.toString());
            if(operationsDocumentedTested.contains(operation)){
                documentedTested.add(operation.toString());
            }else{
                notTested.add(operation.toString());
            }
        }
        for(Operation operation: operationsNotDocumentedTested){
            notDocumentedTested.add(operation.toString());
        }

        report.add("documented",documented);
        report.add("documentedTested", documentedTested);
        report.add("notDocumentedTested", notDocumentedTested);
        report.add("notTested", notTested);
        return report;
    }


}
