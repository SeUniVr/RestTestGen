package io.resttestgen.implementation.writer;

import com.google.gson.GsonBuilder;
import io.resttestgen.core.testing.TestSequence;
import io.resttestgen.core.helper.jsonserializer.TestSequenceSerializer;
import io.resttestgen.core.testing.Writer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ReportWriter extends Writer {

    public ReportWriter(TestSequence testSequence) {
        super(testSequence);
    }

    @Override
    public String getOutputFormatName() {
        return "Report";
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public void write() throws IOException {

        File file = new File(getOutputPath());

        file.mkdirs();

        FileWriter writer = new FileWriter(getOutputPath() + getSuggestedFileName(".json"));
        // Convert map to JSON File
        //new GsonBuilder().setPrettyPrinting().create().toJson(testSequence, writer);
        new GsonBuilder().registerTypeAdapter(TestSequence.class, new TestSequenceSerializer()).setPrettyPrinting().create().toJson(testSequence,writer);

        // Close the writer
        writer.close();
    }
}
