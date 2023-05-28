package io.resttestgen.core.testing.coverage;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.parameter.Parameter;
import io.resttestgen.core.datatype.parameter.leaves.LeafParameter;
import io.resttestgen.core.openapi.Operation;
import io.resttestgen.core.testing.Coverage;
import io.resttestgen.core.testing.TestInteraction;

import java.util.*;

public class ParameterCoverage extends Coverage {

    private HashMap<Operation, Set<ParameterElementWrapper>> parametersToTest = new HashMap<>();
    private HashMap<Operation, Set<ParameterElementWrapper>> parametersDocumentedTested= new HashMap<>();
    private HashMap<Operation, Set<ParameterElementWrapper>> parametersNotDocumentedTested= new HashMap<>();

    public ParameterCoverage(){
        for(Operation operation : Environment.getInstance().getOpenAPI().getOperations()){
            Set<ParameterElementWrapper> parameters = new HashSet<>();
            for(Parameter parameter: operation.getAllRequestParameters()){
                if(parameter instanceof LeafParameter){
                    parameters.add(new ParameterElementWrapper(parameter));
                }
            }
            parametersToTest.put(operation, parameters);
        }
    }
    @Override
    public void updateCoverage(TestInteraction testInteraction) {
        Operation operation = testInteraction.getFuzzedOperation();
        for (Parameter parameter : operation.getAllRequestParameters()) {
            if (parameter instanceof LeafParameter) {
                ParameterElementWrapper elementWrapper = new ParameterElementWrapper(parameter);
                if (parametersToTest.containsKey(operation)) {
                    if (parametersToTest.get(operation).contains(elementWrapper)) {
                        insertParameterToSet(parametersDocumentedTested, elementWrapper, operation);
                    } else {
                        insertParameterToSet(parametersNotDocumentedTested, elementWrapper, operation);
                    }
                } else {
                    insertParameterToSet(parametersNotDocumentedTested, elementWrapper, operation);
                }
            }
        }
    }

    public void insertParameterToSet(HashMap<Operation,Set<ParameterElementWrapper>> map, ParameterElementWrapper parameter, Operation operation){
        if(map.containsKey(operation)){
            map.get(operation).add(parameter);
        }else{
            Set<ParameterElementWrapper> newList = new HashSet<>();
            newList.add(parameter);
            map.put(operation, newList);
        }
    }

    @Override
    public int getNumOfTestedDocumented(){
        int sum = 0;
        for(Operation key: parametersDocumentedTested.keySet()){
            sum += parametersDocumentedTested.get(key).size();
        }
        return sum;
    }

    @Override
    public int getNumOfTestedNotDocumented(){
        int sum = 0;
        for(Operation key: parametersNotDocumentedTested.keySet()){
            sum += parametersNotDocumentedTested.get(key).size();
        }
        return sum;
    }

    @Override
    public int getToTest() {
        int sum = 0;
        for(Operation key: parametersToTest.keySet()){
            sum += parametersToTest.get(key).size();
        }
        return sum;
    }

    @Override
    public JsonObject getReportAsJsonObject() {
        JsonObject report = new JsonObject();
        report.add("documented", createJsonObject(parametersToTest));
        report.add("documentedTested", createJsonObject(parametersDocumentedTested));
        report.add("notDocumentedTested", createJsonObject(parametersNotDocumentedTested));
        report.add("notTested", createJsonObject(createNotTested()));
        return report;
    }

    private JsonObject createJsonObject(HashMap<Operation, Set<ParameterElementWrapper>> map){
        JsonObject jsonObject = new JsonObject();
        for(Operation op : map.keySet()) {
            JsonArray parameters = new JsonArray();
            for (ParameterElementWrapper parameter : map.get(op)) {
                parameters.add(parameter.toString());
            }
            jsonObject.add(op.toString(),parameters);
        }
        return jsonObject;
    }

    private HashMap<Operation, Set<ParameterElementWrapper>> createNotTested(){
        HashMap<Operation, Set<ParameterElementWrapper>> notTested = new HashMap<>();
        for(Operation op : parametersToTest.keySet()){
            boolean containsOperation = parametersDocumentedTested.containsKey(op);
                for(ParameterElementWrapper parameter : parametersToTest.get(op)){
                    if(containsOperation){
                        if(!parametersDocumentedTested.get(op).contains(parameter)){//!containsParameter(parametersDocumentedTested.get(op),parameter)
                            insertParameterToSet(notTested,parameter,op);
                        }
                    }else{
                        insertParameterToSet(notTested,parameter,op);
                    }
                }
        }
        return notTested;
    }
}
