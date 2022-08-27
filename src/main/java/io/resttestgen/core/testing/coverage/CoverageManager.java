package io.resttestgen.core.testing.coverage;

import io.resttestgen.core.testing.Coverage;
import io.resttestgen.core.testing.TestInteraction;

import java.util.LinkedList;
import java.util.List;

public class CoverageManager{

    private final List<Coverage> listCoverages = new LinkedList<>();
    public CoverageManager(){
        OperationCoverage operationCoverage = new OperationCoverage();
        PathCoverage pathCoverage = new PathCoverage();
        ParameterCoverage parameterCoverage = new ParameterCoverage();
        StatusCodeCoverage statusCodeCoverage = new StatusCodeCoverage();
        ParameterValueCoverage parameterValueCoverage = new ParameterValueCoverage();
        this.listCoverages.add(parameterValueCoverage);
        this.listCoverages.add(statusCodeCoverage);
        this.listCoverages.add(operationCoverage);
        this.listCoverages.add(pathCoverage);
        this.listCoverages.add(parameterCoverage);
    }


    public void updateCoverage(TestInteraction testInteraction) {
        for(Coverage coverage: listCoverages){
            coverage.updateCoverage(testInteraction);
        }
    }


    public List<Coverage> getCoverages(){
        return this.listCoverages;
    }
}
