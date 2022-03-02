package io.resttestgen.core.testing;

import java.sql.Timestamp;
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

    private String generator;
    private String name;
    private List<TestInteraction> testInteractions = new LinkedList<>();

    // Time information
    private Timestamp generatedAt;

    // Outcome
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

    public void addTestResult(Oracle oracle, TestResult testResult) {
        testResults.put(oracle, testResult);
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
}