package io.resttestgen.core.testing.coverage;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.parameter.ParameterElement;
import io.resttestgen.core.datatype.parameter.ParameterType;
import io.resttestgen.core.openapi.Operation;
import io.resttestgen.core.testing.Coverage;
import io.resttestgen.core.testing.TestInteraction;
import org.apache.commons.lang3.ObjectUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class ParameterValueCoverage extends Coverage {

    private HashMap<Operation, HashMap<ParameterElement, Set<Object>>> valuesToBeTested = new HashMap<>();
    private HashMap<Operation, HashMap<ParameterElement, Set<Object>>> testedValues= new HashMap<>();

    public ParameterValueCoverage(){
        for(Operation operation : Environment.getInstance().getOpenAPI().getOperations()){
            HashMap<ParameterElement,Set<Object>> newMapParameter = new HashMap<>();
            for(ParameterElement parameter : operation.getLeaves()){
                Set<Object> values = new HashSet<>();
                if(parameter.getType()== ParameterType.BOOLEAN){
                    values.add("True");
                    values.add("False");
                }
                if(!parameter.getEnumValues().isEmpty()){
                    values.add(parameter.getEnumValues());
                }
                if(parameter.isRequired()){
                    values.add("null");
                    values.add("value");
                }
                if(!values.isEmpty()){
                    newMapParameter.put(parameter,values);
                    valuesToBeTested.put(operation,newMapParameter);
                }
            }
        }
        System.out.println(valuesToBeTested);
    }
    @Override
    public void updateCoverage(TestInteraction testInteraction) {
        Operation operation = testInteraction.getOperation();
        if(testedValues.containsKey(operation)){
            for(ParameterElement parameter : operation.getLeaves()) {
                if(testedValues.get(operation).containsKey(parameter)){
                    if(parameter.getType()== ParameterType.BOOLEAN || !parameter.getEnumValues().isEmpty()){
                        testedValues.get(operation).get(parameter).add(parameter.getValue());
                    }
                    if(parameter.isRequired()) {
                        if (parameter.hasValue()) {
                            testedValues.get(operation).get(parameter).add("value");
                        } else {
                            testedValues.get(operation).get(parameter).add("null");
                        }
                    }
                }else {
                    if(parameter.getType()== ParameterType.BOOLEAN || !parameter.getEnumValues().isEmpty()){
                        Set<Object> newSet = new HashSet<>();
                        newSet.add(parameter.getValue());
                        testedValues.get(operation).put(parameter,newSet);
                    }
                    if(parameter.isRequired()) {
                        if (parameter.hasValue()) {
                            Set<Object> newSet = new HashSet<>();
                            newSet.add("value");
                            testedValues.get(operation).put(parameter,newSet);
                        } else {
                            Set<Object> newSet = new HashSet<>();
                            newSet.add("null");
                            testedValues.get(operation).put(parameter,newSet);
                        }
                    }
                }
            }
        }else {
            HashMap<ParameterElement, Set<Object>> newMap = new HashMap<>();
            for(ParameterElement parameter : operation.getLeaves()) {
                if(newMap.containsKey(parameter)){
                    if(parameter.getType()== ParameterType.BOOLEAN || !parameter.getEnumValues().isEmpty()){
                        newMap.get(parameter).add(parameter.getValue());
                    }
                    if(parameter.isRequired()) {
                        if (parameter.hasValue()) {
                            newMap.get(parameter).add("value");
                        } else {
                            newMap.get(parameter).add("null");
                        }
                    }
                }else {
                    if(parameter.getType()== ParameterType.BOOLEAN || !parameter.getEnumValues().isEmpty()){
                        Set<Object> newSet = new HashSet<>();
                        newSet.add(parameter.getValue());
                        newMap.put(parameter,newSet);
                    }
                    if(parameter.isRequired()) {
                        if (parameter.hasValue()) {
                            Set<Object> newSet = new HashSet<>();
                            newSet.add("value");
                            newMap.put(parameter,newSet);
                        } else {
                            Set<Object> newSet = new HashSet<>();
                            newSet.add("null");
                            newMap.put(parameter,newSet);
                        }
                    }
                }
            }
            testedValues.put(operation,newMap);
        }
    }

    @Override
    public int getTested() {
        int sum = 0;
        for(Operation key: testedValues.keySet()){
            for(ParameterElement subKey: testedValues.get(key).keySet()){
                sum += testedValues.get(key).get(subKey).size();
            }
        }
        return sum;
    }

    @Override
    public int getToTest() {
        int sum = 0;
        for(Operation key: valuesToBeTested.keySet()){
            for(ParameterElement subKey: valuesToBeTested.get(key).keySet()){
                sum += valuesToBeTested.get(key).get(subKey).size();
            }
        }
        return sum;
    }

    @Override
    public JsonObject getReportAsJsonObject() {
        JsonObject report = new JsonObject();
        JsonObject documented = new JsonObject();
        JsonObject tested = new JsonObject();
        JsonObject notTested = new JsonObject();
        for(Operation operation: valuesToBeTested.keySet()){
            JsonObject jsonOperationDocumented = new JsonObject();
            JsonObject jsonOperationTested = new JsonObject();
            JsonObject jsonOperationNotTested = new JsonObject();
            for(ParameterElement parameter: valuesToBeTested.get(operation).keySet()){
                JsonArray jsonArrayValuesDocumented = new JsonArray();
                JsonArray jsonArrayValuesTested = new JsonArray();
                JsonArray jsonArrayValuesNotTested = new JsonArray();
                for(Object value: valuesToBeTested.get(operation).get(parameter)){
                   jsonArrayValuesDocumented.add(value.toString());
                   if(testedValues.get(operation).get(parameter).contains(value)){
                       jsonArrayValuesTested.add(value.toString());
                   }else{
                       jsonArrayValuesNotTested.add(value.toString());
                   }
                }
                jsonOperationDocumented.add(parameter.toString(),jsonArrayValuesDocumented);
                jsonOperationTested.add(parameter.toString(),jsonArrayValuesTested);
                jsonOperationNotTested.add(parameter.toString(),jsonArrayValuesNotTested);
            }
            documented.add(operation.toString(),jsonOperationDocumented);
            tested.add(operation.toString(),jsonOperationTested);
            notTested.add(operation.toString(),jsonOperationNotTested);
        }
        report.add("documented",documented);
        report.add("tested", tested);
        report.add("notTested", notTested);
        return report;
    }

}
