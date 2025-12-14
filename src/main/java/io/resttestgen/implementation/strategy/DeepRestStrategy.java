package io.resttestgen.implementation.strategy;

import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.HttpStatusCode;
import io.resttestgen.core.helper.DeepReinforcementLearningProxy;
import io.resttestgen.core.openapi.Operation;
import io.resttestgen.core.testing.*;
import io.resttestgen.core.testing.operationsorter.OperationsSorter;
import io.resttestgen.implementation.fuzzer.ExperienceFuzzer;
import io.resttestgen.implementation.fuzzer.IntensificationFuzzer;
import io.resttestgen.implementation.fuzzer.NominalFuzzer;
import io.resttestgen.implementation.operationssorter.DeepReinforcementLearningOperationsSorter;
import io.resttestgen.implementation.operationssorter.RandomOperationsSorter;
import io.resttestgen.implementation.oracle.StatusCodeOracle;
import io.resttestgen.implementation.strategy.configuration.DeepReinforcementLearningStrategyConfiguration;
import io.resttestgen.implementation.writer.CoverageReportWriter;
import io.resttestgen.implementation.writer.HtmlReportWriter;
import io.resttestgen.implementation.writer.ReportWriter;
import io.resttestgen.implementation.writer.RestAssuredWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@SuppressWarnings("unused")
public class DeepRestStrategy extends Strategy {

    private static final Logger logger = LogManager.getLogger(DeepRestStrategy.class);

    private final DeepReinforcementLearningStrategyConfiguration config =
            StrategyConfiguration.loadConfiguration(DeepReinforcementLearningStrategyConfiguration.class);

    HashSet<Operation> intensificatedOperations = new HashSet<>();

    StatusCodeOracle statusCodeOracle = new StatusCodeOracle();

    @Override
    public void start() {

        HtmlReportWriter htmlReportWriter = initializeHtmlReportWriter();
        List<TestSequence> testSequencesToReport = new ArrayList<>();

        // Request test budget (max of requests sendable by the tool), now set to 50 * (no. of API operations)^2
        final long REQUEST_BUDGET = 50L * Environment.getInstance().getOpenAPI().getOperations().size() * Environment.getInstance().getOpenAPI().getOperations().size();

        DeepReinforcementLearningProxy.initializeDeepReinforcementLearning(config.getNamedPipesPath(),
                Environment.getInstance().getOpenAPI().getOperations().size());

        OperationsSorter sorter = new DeepReinforcementLearningOperationsSorter();

        // If DRL is disabled, use random sorter
        if (config.isDisableDrl()) {
            sorter = new RandomOperationsSorter();
        }

        while (!sorter.isEmpty()) {

            Operation operationToTest = sorter.getFirst();

            logger.debug("Testing operation {}", operationToTest);
            TestSequence nominalSequence;
            if (config.getFuzzer().equals("experience")) {
                ExperienceFuzzer experienceFuzzer = new ExperienceFuzzer(operationToTest);
                nominalSequence = experienceFuzzer.generateTestSequences(1).get(0);
            } else {
                NominalFuzzer nominalFuzzer = new NominalFuzzer(operationToTest);
                nominalSequence = nominalFuzzer.generateTestSequences(1).get(0);
            }

            TestRunner.getInstance().run(nominalSequence);
            HttpStatusCode statusCode = nominalSequence.get(0).getResponseStatusCode();
            if (statusCode == null) {
                //logger.warn("Found NULL status code");
                // Fallback
                statusCode = new HttpStatusCode(400);
            }
            DeepReinforcementLearningProxy.sendResult(statusCode);
            testSequencesToReport.add(nominalSequence);
            writeTestSequenceReport(nominalSequence);

            // In case of successful interaction, invoke intensification testing, but only the first time, and then with
            // a low probability. Of course, only if intensification is enabled by configuration
            if (statusCode.isSuccessful() && config.isIntensification()) {
                if (!intensificatedOperations.contains(operationToTest) || (Environment.getInstance().getRandom().nextInt(0, 100) < config.getIntensificationProbability())) {

                    logger.info("Performed successful interaction. Starting intensification for {}.", operationToTest);

                    IntensificationFuzzer intensificationFuzzer = new IntensificationFuzzer(nominalSequence);
                    List<TestSequence> intensificatedSequences = intensificationFuzzer.generateTestSequences(0); // Input to this method is currently ignored

                    intensificatedOperations.add(operationToTest);

                    testSequencesToReport.addAll(intensificatedSequences);
                    intensificatedSequences.forEach(this::writeTestSequenceReport);

                    logger.info("Intensification completed. Continuing with testing.");
                }
            }

            sorter.removeFirst();

            if (nominalSequence.getId() > REQUEST_BUDGET) {
                logger.info("Test budget of {} requests is over. Exiting.", REQUEST_BUDGET);
                break;
            }
        }

        writeCoverageReport(htmlReportWriter);

        htmlReportWriter.populateCoverageCollection(TestRunner.getInstance().getCoverage());
        injectTestSequenceData(htmlReportWriter, testSequencesToReport);
    }

    private void writeTestSequenceReport(TestSequence testSequence) {
        try {
            statusCodeOracle.assertTestSequence(testSequence);
            ReportWriter reportWriter = new ReportWriter(testSequence);
            reportWriter.write();
            RestAssuredWriter restAssuredWriter = new RestAssuredWriter(testSequence);
            restAssuredWriter.write();
        } catch (IOException e) {
            logger.warn("Could not write test sequence report to file.", e);
        }
    }

    private HtmlReportWriter initializeHtmlReportWriter() {
        HtmlReportWriter htmlReportWriter = new HtmlReportWriter(Environment.getInstance().getConfiguration());
        try {
            htmlReportWriter.createReportResourcesFiles();
        } catch (IOException e) {
            logger.warn("Could not copy folders from template-resources", e);
        }
        return htmlReportWriter;
    }

    private void writeCoverageReport(HtmlReportWriter htmlReportWriter) {
        try {
            CoverageReportWriter coverageReportWriter = new CoverageReportWriter(TestRunner.getInstance().getCoverage());
            coverageReportWriter.write();
        } catch (IOException e) {
            logger.warn("Could not write test coverage report to file.", e);
        }
    }

    private void injectTestSequenceData(HtmlReportWriter htmlReportWriter, List<TestSequence> testSequencesToReport) {
        logger.debug("Injecting test sequences into JS constants file.");
        try {
            htmlReportWriter.injectTestSequenceData(testSequencesToReport);
        } catch (IOException e) {
            logger.warn("Could not write test sequence data to JavaScript constants file.", e);
        }
        logger.debug("Done injecting test sequences into JS constants file.");
    }
}
