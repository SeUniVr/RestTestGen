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

    private Set<String> pathsToTest= new HashSet<>();
    private Set<String> pathsDocumentedTested= new HashSet<>();
    private Set<String> pathsNotDocumentedTested = new HashSet<>();

    public PathCoverage(){
        for(Operation operation : Environment.getInstance().getOpenAPI().getOperations()){
            pathsToTest.add(operation.getEndpoint());
        }
    }
    @Override
    public void updateCoverage(TestInteraction testInteraction) {
        if(pathsToTest.contains(testInteraction.getFuzzedOperation().getEndpoint())){
            pathsDocumentedTested.add(testInteraction.getFuzzedOperation().getEndpoint());
        }else{
            pathsNotDocumentedTested.add(testInteraction.getFuzzedOperation().getEndpoint());
        }
    }
    @Override
    public int getNumOfTestedDocumented(){
        return pathsDocumentedTested.size();
    }

    @Override
    public int getNumOfTestedNotDocumented(){
        return pathsNotDocumentedTested.size();
    }

    @Override
    public int getToTest() {
        return pathsToTest.size();
    }

    @Override
    public JsonObject getReportAsJsonObject() {
        JsonObject report = new JsonObject();
        JsonArray documented = new JsonArray(getToTest());
        JsonArray testedDocumented = new JsonArray(getNumOfTestedDocumented());
        JsonArray testedNotDocumented = new JsonArray(getNumOfTestedNotDocumented());
        JsonArray notTested = new JsonArray(getToTest()-getNumOfTestedDocumented());
        for(String path : pathsToTest){
            documented.add(path);
            if(pathsDocumentedTested.contains(path)){
                testedDocumented.add(path);
            }else{
                notTested.add(path);
            }
        }
        for(String path : pathsNotDocumentedTested){
            testedNotDocumented.add(path);
        }
        report.add("documented",documented);
        report.add("documentedTested", testedDocumented);
        report.add("notDocumentedTested", testedNotDocumented);
        report.add("notTested", notTested);
        return report;
    }


}
