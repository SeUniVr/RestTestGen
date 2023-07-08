package io.resttestgen.core.testing.coverage;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.parameter.Parameter;
import io.resttestgen.core.datatype.parameter.leaves.LeafParameter;
import io.resttestgen.core.datatype.parameter.attributes.ParameterType;
import io.resttestgen.core.openapi.Operation;
import io.resttestgen.core.testing.Coverage;
import io.resttestgen.core.testing.TestInteraction;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class ParameterValueCoverage extends Coverage {

    private HashMap<Operation, HashMap<ParameterElementWrapper, Set<Object>>> valuesToTest = new HashMap<>();
    private HashMap<Operation, HashMap<ParameterElementWrapper, Set<Object>>> valuesDocumentedTested= new HashMap<>();
    private HashMap<Operation, HashMap<ParameterElementWrapper, Set<Object>>> valuesNotDocumentedTested= new HashMap<>();

    public ParameterValueCoverage(){
        for(Operation operation : Environment.getInstance().getOpenAPI().getOperations()){
            HashMap<ParameterElementWrapper,Set<Object>> newMapParameters = new HashMap<>();
            for(Parameter parameter : operation.getAllRequestParameters()){
                if(parameter instanceof LeafParameter){
                    ParameterElementWrapper parameterWrapper = new ParameterElementWrapper(parameter);
                    Set<Object> values = new HashSet<>();
                    if(parameter.getType() == ParameterType.BOOLEAN){
                        values.add(true);
                        values.add(false);
                    }
                    if(parameter.isEnum()){
                        values.addAll(parameter.getEnumValues());
                    }
                    if(!values.isEmpty()){
                        newMapParameters.put(parameterWrapper,values);
                        valuesToTest.put(operation,newMapParameters);
                    }
                }
            }
        }
    }
    @Override
    public void updateCoverage(TestInteraction testInteraction) {
        Operation operation = testInteraction.getFuzzedOperation();
        boolean containsOperation = valuesToTest.containsKey(operation);
        for(Parameter parameter : operation.getAllRequestParameters()) {
            if(parameter instanceof LeafParameter){
                ParameterElementWrapper parameterWrapper = new ParameterElementWrapper(parameter);
                if(parameter.getType()== ParameterType.BOOLEAN || parameter.isEnum()){
                    if(containsOperation){
                        if(valuesToTest.get(operation).containsKey(parameterWrapper)){
                            if(valuesToTest.get(operation).get(parameterWrapper).contains(((LeafParameter)parameterWrapper.getParameter()).getConcreteValue())) {
                                insertParameterValueToSet(valuesDocumentedTested, operation, parameterWrapper, ((LeafParameter)parameter).getConcreteValue());
                            }else{
                                insertParameterValueToSet(valuesNotDocumentedTested,operation, parameterWrapper, ((LeafParameter)parameter).getConcreteValue());
                            }
                        }else{
                            insertParameterValueToSet(valuesNotDocumentedTested,operation, parameterWrapper, ((LeafParameter)parameter).getConcreteValue());
                        }
                    }else{
                        insertParameterValueToSet(valuesNotDocumentedTested, operation, parameterWrapper, ((LeafParameter)parameter).getConcreteValue());
                    }
                }
            }
        }
    }

    public void insertParameterValueToSet(HashMap<Operation, HashMap<ParameterElementWrapper, Set<Object>>> operationMap,Operation operation, ParameterElementWrapper parameter, Object value){
        if(operationMap.containsKey(operation)){
            if(operationMap.get(operation).containsKey(parameter)){
                operationMap.get(operation).get(parameter).add(value);
            }else{
                Set<Object> values = new HashSet<>();
                values.add(value);
                operationMap.get(operation).put(parameter,values);
            }
        }else{
            HashMap<ParameterElementWrapper, Set<Object>> parametersMap = new HashMap<>();
            Set<Object> values = new HashSet<>();
            values.add(value);
            parametersMap.put(parameter, values);
            operationMap.put(operation, parametersMap);
        }
    }

    @Override
    public int getNumOfTestedDocumented(){
        int sum = 0;
        for(Operation key: valuesDocumentedTested.keySet()){
            for(ParameterElementWrapper subKey: valuesDocumentedTested.get(key).keySet()){
                sum += valuesDocumentedTested.get(key).get(subKey).size();
            }
        }
        return sum;
    }

    @Override
    public int getNumOfTestedNotDocumented(){
        int sum = 0;
        for(Operation key: valuesNotDocumentedTested.keySet()){
            for(ParameterElementWrapper subKey: valuesNotDocumentedTested.get(key).keySet()){
                sum += valuesNotDocumentedTested.get(key).get(subKey).size();
            }
        }
        return sum;
    }

    @Override
    public int getToTest() {
        int sum = 0;
        for(Operation key: valuesToTest.keySet()){
            for(ParameterElementWrapper subKey: valuesToTest.get(key).keySet()){
                sum += valuesToTest.get(key).get(subKey).size();
            }
        }
        return sum;
    }

    @Override
    public JsonObject getReportAsJsonObject() {
        JsonObject report = new JsonObject();
        report.add("documented", createJsonObject(valuesToTest));
        report.add("documentedTested", createJsonObject(valuesDocumentedTested));
        report.add("notDocumentedTested", createJsonObject(valuesNotDocumentedTested));
        report.add("notTested", createJsonObject(createNotTested()));
        return report;
    }

    private JsonObject createJsonObject(HashMap<Operation, HashMap<ParameterElementWrapper, Set<Object>>> operationsMap){
        JsonObject jsonObject = new JsonObject();
        for(Operation op : operationsMap.keySet()) {
            JsonObject jsonObjectOperation = new JsonObject();
            for(ParameterElementWrapper parameter : operationsMap.get(op).keySet()){
                JsonArray values = new JsonArray();
                for(Object value : operationsMap.get(op).get(parameter)){
                    if(value != null){
                        values.add(value.toString());
                    }else{
                        values.add("null");
                    }
                }
                jsonObjectOperation.add(parameter.toString(),values);
            }
            jsonObject.add(op.toString(),jsonObjectOperation);
        }
        return jsonObject;
    }

    private HashMap<Operation, HashMap<ParameterElementWrapper, Set<Object>>> createNotTested(){
        HashMap<Operation, HashMap<ParameterElementWrapper, Set<Object>>> notTested = new HashMap<>();
        boolean containsOperation;
        boolean containsParameter = false;
        for(Operation op : valuesToTest.keySet()){
            containsOperation = valuesDocumentedTested.containsKey(op);
            for(ParameterElementWrapper parameter : valuesToTest.get(op).keySet()){
                if(containsOperation){
                    containsParameter = valuesDocumentedTested.get(op).containsKey(parameter);
                }
                for(Object value : valuesToTest.get(op).get(parameter)){
                    if(containsOperation && containsParameter){
                        if(!valuesDocumentedTested.get(op).get(parameter).contains(value)){
                            insertParameterValueToSet(notTested,op,parameter,value);
                        }
                    }else{
                        insertParameterValueToSet(notTested,op,parameter,value);
                    }
                }
            }
        }
        return notTested;
    }

}
