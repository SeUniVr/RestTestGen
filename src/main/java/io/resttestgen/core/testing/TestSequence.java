package io.resttestgen.core.testing;

import io.resttestgen.core.Environment;
import io.resttestgen.core.helper.ExtendedRandom;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Represents an ordered sequence of test interactions. Although the current implementations of fuzzers do not require
 * ordered sequences, the order is anyway managed by this class in order to support further extensions.
 */
public class TestSequence {

    private String generator = "UserInstantiated";
    private String name = generateRandomTestSequenceName();
    private String tag;
    private List<TestInteraction> testInteractions = new LinkedList<>();

    // Time information
    private final Timestamp generatedAt;

    // Outcome
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private Map<Oracle, TestResult> testResults = new HashMap<>();

    public TestSequence() {
        generatedAt = new Timestamp(System.currentTimeMillis());
    }

    public TestSequence(Fuzzer generator, TestInteraction testInteraction) {
        generatedAt = new Timestamp(System.currentTimeMillis());
        setGenerator(generator);
        append(testInteraction);
    }

    public TestSequence(Fuzzer generator, List<TestInteraction> testInteractions) {
        generatedAt = new Timestamp(System.currentTimeMillis());
        setGenerator(generator);
        this.testInteractions = testInteractions;
    }

    public List<TestInteraction> getTestInteractions() {
        return testInteractions;
    }

    public void setTestInteractions(List<TestInteraction> testInteractions) {
        this.testInteractions = testInteractions;
    }

    /**
     * Returns the number of test interactions inside the test sequence.
     * @return the number of test interactions inside the test sequence.
     */
    public int size() {
        return testInteractions.size();
    }

    public TestInteraction getFirst() {
        return testInteractions.get(0);
    }

    public TestInteraction get(int index) {
        if (index > testInteractions.size() || index < 0) {
            throw new IndexOutOfBoundsException();
        }
        return testInteractions.get(index);
    }

    public TestInteraction getLast() {
        return testInteractions.get(testInteractions.size() - 1);
    }

    public void append(TestInteraction testInteraction) {
        testInteractions.add(testInteraction);
    }

    /**
     * Appends to the current test sequence, the interaction contained into another test sequence
     * @param testSequence the test sequence to append to the current one
     */
    public void append(TestSequence testSequence) {
        if (testInteractions.size() == 0) {
            this.generator = testSequence.generator;
        } else if (!this.generator.endsWith(testSequence.generator)){
            this.generator = this.generator + "+" + testSequence.generator;
        }
        testInteractions.addAll(testSequence.getTestInteractions());
    }

    /**
     * Appends to the current test sequence, a list of test sequences
     * @param testSequenceList the list of test sequences to append
     */
    public void append(List<TestSequence> testSequenceList) {
        testSequenceList.forEach(this::append);
    }

    /**
     * Removes from the test sequence operations that not have a successful status code (200-299)
     */
    public void filterBySuccessfulStatusCode() {
        testInteractions = testInteractions.stream()
                .filter(testInteraction -> testInteraction.getResponseStatusCode().isSuccessful())
                .collect(Collectors.toList());
    }

    public TestSequence getSubSequence(int i, int i1) {
        TestSequence subSequence = new TestSequence();
        subSequence.setTestInteractions(testInteractions.subList(i, i1));
        return subSequence;
    }

    public void setName(String name) {
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("The name for a test sequence must not be null or an empty string.");
        }
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setGenerator(Fuzzer generator) {
        this.generator = generator.getClass().getSimpleName();
    }

    public String getGenerator() {
        return generator;
    }

    public Timestamp getGeneratedAt() {
        return generatedAt;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public void addTestResult(Oracle oracle, TestResult testResult) {
        testResults.put(oracle, testResult);
    }

    /**
     * Checks if all the test interactions composing a sequence have been executed
     * @return true if all interaction in the sequence have been executed.
     */
    public boolean isExecuted() {
        for (TestInteraction testInteraction : testInteractions) {
            if (testInteraction.getTestStatus() != TestStatus.EXECUTED) {
                return false;
            }
        }
        return true;
    }

    public void reset() {
        testInteractions.forEach(TestInteraction::reset);
        testResults = new HashMap<>();
    }

    public TestSequence deepClone() {
        TestSequence target = new TestSequence();
        this.testInteractions.forEach(testInteraction -> target.append(testInteraction.deepClone()));
        return target;
    }

    private String generateRandomTestSequenceName() {
        ExtendedRandom random = Environment.getInstance().getRandom();
        return random.nextWord() + "-" + random.nextWord();
    }

    public void appendGeneratedAtTimestampToSequenceName() {
        SimpleDateFormat dformat = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        this.name = this.name + "-" + dformat.format(getGeneratedAt());
    }
}