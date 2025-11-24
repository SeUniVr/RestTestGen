package io.resttestgen.core.testing.coverage;

import io.resttestgen.core.testing.TestInteraction;
import io.resttestgen.implementation.coveragemetric.*;

import java.util.LinkedList;
import java.util.List;

public class CoverageManager {

    private final List<Coverage> coverageMetrics = new LinkedList<>();

    public CoverageManager(){
        coverageMetrics.add(new OperationCoverage());
        coverageMetrics.add(new PathCoverage());
        coverageMetrics.add(new ParameterCoverage());
        coverageMetrics.add(new StatusCodeCoverage());
        coverageMetrics.add(new ParameterValueCoverage());
        coverageMetrics.add(new SuccessfulOperationCoverage());
    }

    public void updateAllCoverage(TestInteraction testInteraction) {
        coverageMetrics.forEach(cm -> cm.updateCoverage(testInteraction));

    }

    public List<Coverage> getCoverageMetrics(){
        return this.coverageMetrics;
    }
}
