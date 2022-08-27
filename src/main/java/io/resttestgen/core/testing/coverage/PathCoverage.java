package io.resttestgen.core.testing.coverage;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.resttestgen.core.Environment;
import io.resttestgen.core.openapi.Operation;
import io.resttestgen.core.testing.Coverage;
import io.resttestgen.core.testing.TestInteraction;

import java.util.HashSet;
import java.util.Set;

public class PathCoverage extends Coverage {

    private Set<String> testedPaths = new HashSet<>();
    private Set<String> pathsToBeTested= new HashSet<>();

    public PathCoverage(){
        for(Operation operation : Environment.getInstance().getOpenAPI().getOperations()){
            pathsToBeTested.add(operation.getEndpoint());
        }
    }
    @Override
    public void updateCoverage(TestInteraction testInteraction) {
        testedPaths.add(testInteraction.getOperation().getEndpoint());
    }

    public double computeCoverage(){
        return ((double)getTested()/(double)getToTest());
    }


    @Override
    public int getTested() {
        return testedPaths.size();
    }

    @Override
    public int getToTest() {
        return pathsToBeTested.size();
    }

    @Override
    public JsonObject getReportAsJsonObject() {
        JsonObject report = new JsonObject();
        JsonArray documented = new JsonArray(getToTest());
        JsonArray tested = new JsonArray(getTested());
        JsonArray notTested = new JsonArray(getToTest()-getTested());
        for(String path : pathsToBeTested){
            documented.add(path);
            if(testedPaths.contains(path)){
                tested.add(path);
            }else{
                notTested.add(path);
            }
        }
        report.add("documented",documented);
        report.add("tested", tested);
        report.add("notTested", notTested);
        return report;
    }


}
