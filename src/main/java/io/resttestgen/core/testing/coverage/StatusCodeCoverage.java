package io.resttestgen.core.testing.coverage;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.HttpStatusCode;
import io.resttestgen.core.openapi.Operation;
import io.resttestgen.core.testing.Coverage;
import io.resttestgen.core.testing.TestInteraction;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;


public class StatusCodeCoverage  extends Coverage {

    private HashMap<Operation, Set<HttpStatusCode>> responseCodeToBeTested = new HashMap<>();
    private HashMap<Operation, Set<HttpStatusCode>> testedResponseCode= new HashMap<>();

    public StatusCodeCoverage(){
        for(Operation operation : Environment.getInstance().getOpenAPI().getOperations()){
            Set<HttpStatusCode> paths = new HashSet<>();
            for(String responseCode : operation.getOutputParameters().keySet()){
                paths.add(new HttpStatusCode(Integer.parseInt(responseCode)));
            }
            responseCodeToBeTested.put(operation,paths);
        }
    }
    @Override
    public void updateCoverage(TestInteraction testInteraction) {
        Operation operation = testInteraction.getOperation();
        if(testedResponseCode.containsKey(operation)){
            testedResponseCode.get(operation).add(testInteraction.getResponseStatusCode());
        }else{
            Set<HttpStatusCode> newSet = new HashSet<>();
            newSet.add(testInteraction.getResponseStatusCode());
            testedResponseCode.put(operation,newSet);
        }
    }

    @Override
    public int getTested() {
        int sum = 0;
        for(Operation key: testedResponseCode.keySet()){
            sum += testedResponseCode.get(key).size();
        }
        return sum;
    }

    @Override
    public int getToTest() {
        int sum = 0;
        for(Operation key: responseCodeToBeTested.keySet()){
            sum += responseCodeToBeTested.get(key).size();
        }
        return sum;
    }

    @Override
    public JsonObject getReportAsJsonObject() {
        JsonObject report = new JsonObject();
        JsonObject documented = new JsonObject();
        JsonObject tested = new JsonObject();
        JsonObject notTested = new JsonObject();
        for(Operation op : responseCodeToBeTested.keySet()){

            JsonArray operationDocumented = new JsonArray();
            JsonArray operationTested = new JsonArray();
            JsonArray operationNotTested = new JsonArray();
            for(HttpStatusCode responseCode: responseCodeToBeTested.get(op)){
                operationDocumented.add(responseCode.toString());
                if(testedResponseCode.get(op).contains(responseCode)){
                    operationTested.add(responseCode.toString());
                }else{
                    operationNotTested.add(responseCode.toString());
                }
            }
            documented.add(op.toString(),operationDocumented);
            tested.add(op.toString(),operationTested);
            notTested.add(op.toString(),operationNotTested);

        }
        report.add("documented",documented);
        report.add("tested", tested);
        report.add("notTested", notTested);
        return report;
    }
}
