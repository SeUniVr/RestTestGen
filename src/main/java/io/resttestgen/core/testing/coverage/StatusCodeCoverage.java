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

    private HashMap<Operation, Set<HttpStatusCode>> statusCodeToTest = new HashMap<>();
    private HashMap<Operation, Set<HttpStatusCode>> statusCodeDocumentedTested = new HashMap<>();
    private HashMap<Operation, Set<HttpStatusCode>> statusCodeNotDocumentedTested = new HashMap<>();

    public StatusCodeCoverage(){
        for(Operation operation : Environment.getInstance().getOpenAPI().getOperations()){
            Set<HttpStatusCode> codes = new HashSet<>();
            for(String code : operation.getOutputParameters().keySet()){
                try {
                    codes.add(new HttpStatusCode(Integer.parseInt(code)));
                } catch (NumberFormatException ignored) {}
            }
            statusCodeToTest.put(operation,codes);
        }
    }

    @Override
    public void updateCoverage(TestInteraction testInteraction) {
        Operation op = testInteraction.getFuzzedOperation();
        if(statusCodeToTest.containsKey(op)){
            if(statusCodeToTest.get(op).contains(testInteraction.getResponseStatusCode())){
                insertStatusCodeToSet(statusCodeDocumentedTested,op,testInteraction.getResponseStatusCode());
            }else{
                insertStatusCodeToSet(statusCodeNotDocumentedTested,op,testInteraction.getResponseStatusCode());
            }
        }else{
            insertStatusCodeToSet(statusCodeNotDocumentedTested,op,testInteraction.getResponseStatusCode());
        }
    }

    private void insertStatusCodeToSet(HashMap<Operation, Set<HttpStatusCode>> operationsMap, Operation operation, HttpStatusCode code){
        if(operationsMap.containsKey(operation)){
            operationsMap.get(operation).add(code);
        }else{
            Set<HttpStatusCode> codes = new HashSet<>();
            codes.add(code);
            operationsMap.put(operation, codes);
        }
    }

    @Override
    public int getToTest() {
        int sum = 0;
        for(Operation key: statusCodeToTest.keySet()){
            sum += statusCodeToTest.get(key).size();
        }
        return sum;
    }

    @Override
    public int getNumOfTestedDocumented(){
        int sum = 0;
        for(Operation key: statusCodeDocumentedTested.keySet()){
            sum += statusCodeDocumentedTested.get(key).size();
        }
        return sum;
    }

    @Override
    public int getNumOfTestedNotDocumented(){
        int sum = 0;
        for(Operation key: statusCodeNotDocumentedTested.keySet()){
            sum += statusCodeNotDocumentedTested.get(key).size();
        }
        return sum;
    }

    @Override
    public JsonObject getReportAsJsonObject() {
        JsonObject report = new JsonObject();
        report.add("documented", createJsonObject(statusCodeToTest));
        report.add("documentedTested", createJsonObject(statusCodeDocumentedTested));
        report.add("notDocumentedTested", createJsonObject(statusCodeNotDocumentedTested));
        report.add("notTested", createJsonObject(createNotTested()));
        return report;
    }

    private JsonObject createJsonObject(HashMap<Operation, Set<HttpStatusCode>> operationMap){
        JsonObject jsonObject = new JsonObject();
        for(Operation op : operationMap.keySet()) {
            JsonArray codes = new JsonArray();
            for (HttpStatusCode code : operationMap.get(op)) {
                codes.add(code.toString());
            }
            jsonObject.add(op.toString(),codes);
        }
        return jsonObject;
    }

    private HashMap<Operation, Set<HttpStatusCode>> createNotTested(){
        HashMap<Operation, Set<HttpStatusCode>> notTested = new HashMap<>();
        for(Operation op : statusCodeToTest.keySet()){
            boolean containsOperation = statusCodeDocumentedTested.containsKey(op);
            for(HttpStatusCode code : statusCodeToTest.get(op)){
                if(containsOperation){
                    if(!statusCodeDocumentedTested.get(op).contains(code)){
                        insertStatusCodeToSet(notTested,op,code);
                    }
                }else{
                    insertStatusCodeToSet(notTested,op,code);
                }
            }
        }
        return notTested;
    }
}
