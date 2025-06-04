package io.resttestgen.core.openapi;

import io.resttestgen.boot.Configuration;
import io.resttestgen.core.Environment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class OpenApiIssueWriter {

    private static final Logger logger = LogManager.getLogger(OpenApiIssueWriter.class);

    private static final Configuration configuration = Environment.getInstance().getConfiguration();

    public static void writeIssue(String message) {
        try {
            File file = new File(configuration.getOutputPath() + configuration.getTestingSessionName());
            file.mkdirs();
            Files.writeString(
                    Path.of(configuration.getOutputPath() + configuration.getTestingSessionName() +
                            "/specification-issues.txt"),
                    message + System.lineSeparator(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (Exception e) {
            logger.error("Error writing specification issue to file.");
        }
    }
}
