package io.resttestgen.core.testing.coverage;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.parameter.ParameterElement;
import io.resttestgen.core.datatype.parameter.ParameterLeaf;
import io.resttestgen.core.openapi.Operation;
import io.resttestgen.core.testing.Coverage;
import io.resttestgen.core.testing.TestInteraction;

import java.util.*;

public class ParameterCoverage extends Coverage {

    private HashMap<Operation, Set<ParameterElement>> parametersToBeTested = new HashMap<>();
    private HashMap<Operation, Set<ParameterElement>> testedParameters= new HashMap<>();

    public ParameterCoverage(){
        for(Operation operation : Environment.getInstance().getOpenAPI().getOperations()){
            Set<ParameterElement> parameters = new HashSet<>(operation.getLeaves());
            parametersToBeTested.put(operation, parameters);
        }
    }
    @Override
    public void updateCoverage(TestInteraction testInteraction) {
        Operation operation = testInteraction.getOperation();
        for(ParameterElement parameter : operation.getReferenceLeaves()){
            if(parameter.getValue()!=null){
                if(testedParameters.containsKey(operation)){
                    testedParameters.get(operation).add(parameter);
                }else{
                    Set<ParameterElement> newList = new HashSet<>();
                    newList.add(parameter);
                    testedParameters.put(operation, newList);
                }
            }
        }
    }

    @Override
    public int getTested() {
        int sum = 0;
        for(Operation key: testedParameters.keySet()){
            sum += testedParameters.get(key).size();
        }

        return sum;
    }

    @Override
    public int getToTest() {
        int sum = 0;
        for(Operation key: parametersToBeTested.keySet()){
            sum += parametersToBeTested.get(key).size();
        }
        return sum;
    }

    @Override
    public JsonObject getReportAsJsonObject() {
        JsonObject report = new JsonObject();
        JsonObject documented = new JsonObject();
        JsonObject tested = new JsonObject();
        JsonObject notTested = new JsonObject();
        for(Operation op : parametersToBeTested.keySet()){

            JsonArray operationDocumented = new JsonArray();
            JsonArray operationTested = new JsonArray();
            JsonArray operationNotTested = new JsonArray();
            for(ParameterElement parameter: parametersToBeTested.get(op)){
                operationDocumented.add(parameter.toString());
                if(testedParameters.get(op).contains(parameter)){
                    operationTested.add(parameter.toString());
                }else{
                    operationNotTested.add(parameter.toString());
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
