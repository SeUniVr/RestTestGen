package io.resttestgen.implementation.writer;

import com.google.gson.GsonBuilder;
import io.resttestgen.core.Environment;
import io.resttestgen.core.testing.TestSequence;
import io.resttestgen.core.testing.Writer;

import java.io.File;
import java.io.FileWriter;

public class ReportWriter extends Writer {

    public ReportWriter(Environment environment, TestSequence testSequence) {
        super(environment, testSequence);
    }

    @Override
    public String getOutputFormatName() {
        return "Report";
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public void write() {
        try {
            File file = new File(getOutputPath());

            file.mkdirs();

            FileWriter writer = new FileWriter(getOutputPath() +
                    testSequence.getName().replaceAll("[^a-zA-Z0-9\\.\\-]", "_") + ".json");

            // Convert map to JSON File
            new GsonBuilder().setPrettyPrinting().create().toJson(testSequence, writer);

            // Close the writer
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
