package io.resttestgen.implementation.writer;

import com.google.gson.JsonObject;
import io.resttestgen.boot.Configuration;
import io.resttestgen.core.Environment;
import io.resttestgen.core.testing.Coverage;
import io.resttestgen.core.testing.coverage.CoverageManager;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class CoverageReportWriter {

    private final CoverageManager coverageManager;
    private final Configuration configuration = Environment.getInstance().getConfiguration();

    public CoverageReportWriter(CoverageManager coverageManager){
        this.coverageManager = coverageManager;
    }

    public String getOutputFormatName() {
        return "CoverageReports";
    }

    private String getOutputPath(){
        return configuration.getOutputPath() + configuration.getTestingSessionName() + "/" + getOutputFormatName() + "/";
    }

    public void write() throws IOException {

        File file = new File(getOutputPath());

        file.mkdirs();

        FileWriter writer = new FileWriter(getOutputPath() + "CoverageStats.json");
        JsonObject jsonRoot = new JsonObject();

        for(Coverage coverage: coverageManager.getCoverages()){
            FileWriter singleCoverageReportWriter = new FileWriter(getOutputPath() + coverage.getClass().getSimpleName() + (".json"));
            singleCoverageReportWriter.write(String.valueOf(coverage.getReportAsJsonObject()));
            singleCoverageReportWriter.close();
            JsonObject simpleCoverageReport = new JsonObject();
            JsonObject raw = new JsonObject();
            raw.addProperty("documented", coverage.getToTest());
            raw.addProperty("documentedTested", coverage.getNumOfTestedDocumented());
            raw.addProperty("notDocumentedTested", coverage.getNumOfTestedNotDocumented());
            simpleCoverageReport.add("raw",raw);
            simpleCoverageReport.addProperty("rate", coverage.getCoverage().toString());
            jsonRoot.add(coverage.getClass().getSimpleName(), simpleCoverageReport);

        }
        writer.write(jsonRoot.toString());
        // Close the writer
        writer.close();
    }
}
