package io.resttestgen.core.testing;

import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.parameter.Parameter;
import io.resttestgen.core.datatype.parameter.leaves.LeafParameter;
import io.resttestgen.core.helper.ExtendedRandom;
import io.resttestgen.core.helper.Taggable;
import org.jetbrains.annotations.NotNull;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static io.resttestgen.core.datatype.parameter.ParameterUtils.getLeaves;

/**
 * Represents an ordered sequence of test interactions. Although the current implementations of fuzzers do not require
 * ordered sequences, the order is anyway managed by this class in order to support further extensions.
 */
public class TestSequence extends Taggable implements List<TestInteraction> {

    public String readOnlyParameter = "";

    private String generator = "UserInstantiated";
    private String name = generateRandomTestSequenceName();
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

    protected void setTestInteractions(List<TestInteraction> testInteractions) {
        this.testInteractions = testInteractions;
    }

    /**
     * Returns the number of test interactions inside the test sequence.
     * @return the number of test interactions inside the test sequence.
     */
    public int size() {
        return testInteractions.size();
    }

    @Override
    public boolean isEmpty() {
        return testInteractions.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return testInteractions.contains(o);
    }

    @NotNull
    @Override
    public Iterator<TestInteraction> iterator() {
        return testInteractions.iterator();
    }

    @NotNull
    @Override
    public Object[] toArray() {
        return testInteractions.toArray();
    }

    @NotNull
    @Override
    public <T> T[] toArray(@NotNull T[] ts) {
        return testInteractions.toArray(ts);
    }

    @Override
    public boolean add(TestInteraction testInteraction) {
        return testInteractions.add(testInteraction);
    }

    @Override
    public boolean remove(Object o) {
        return testInteractions.remove(o);
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> collection) {
        return testInteractions.containsAll(collection);
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends TestInteraction> collection) {
        return testInteractions.addAll(collection);
    }

    @Override
    public boolean addAll(int i, @NotNull Collection<? extends TestInteraction> collection) {
        return testInteractions.addAll(i, collection);
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> collection) {
        return testInteractions.removeAll(collection);
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> collection) {
        return testInteractions.retainAll(collection);
    }

    @Override
    public void clear() {

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

    @Override
    public TestInteraction set(int i, TestInteraction testInteraction) {
        return testInteractions.set(i, testInteraction);
    }

    @Override
    public void add(int i, TestInteraction testInteraction) {
        testInteractions.add(i, testInteraction);
    }

    @Override
    public TestInteraction remove(int i) {
        return testInteractions.remove(i);
    }

    @Override
    public int indexOf(Object o) {
        return testInteractions.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return testInteractions.lastIndexOf(o);
    }

    @NotNull
    @Override
    public ListIterator<TestInteraction> listIterator() {
        return testInteractions.listIterator();
    }

    @NotNull
    @Override
    public ListIterator<TestInteraction> listIterator(int i) {
        return testInteractions.listIterator(i);
    }

    @NotNull
    @Override
    public List<TestInteraction> subList(int i, int i1) {
        return testInteractions.subList(i, i1);
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
        testInteractions.addAll(testSequence);
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
                .filter(testInteraction -> testInteraction.getTestStatus() == TestStatus.EXECUTED &&
                        testInteraction.getResponseStatusCode().isSuccessful())
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

    /**
     * Replaces concrete values in interactions with a pointer to an output value, in case previous interactions have
     * returned a value used later on as input in the sequence.
     */
    public void inferVariablesFromConcreteValues() {
        for (int i = testInteractions.size() - 1; i > 0; i--) {
            for (LeafParameter responseLeaf : testInteractions.get(i).getFuzzedOperation().getLeaves()) {

                boolean found = false;

                for (int j = i - 1; j >= 0 && !found; j--) {

                    Parameter responseBody = testInteractions.get(j).getFuzzedOperation().getResponseBody();
                    if (responseBody != null) {
                        for (LeafParameter requestLeaf : getLeaves(responseBody)) {
                            if (requestLeaf.getName().equals(responseLeaf.getName()) &&
                                    requestLeaf.getValue().toString().equals(responseLeaf.getValue().toString())) {
                                requestLeaf.setValue(responseLeaf);
                                found = true;
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    public TestSequence reset() {
        testInteractions.forEach(TestInteraction::reset);
        testResults = new HashMap<>();
        return this;
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

    public Map<Oracle, TestResult> getTestResults() {
        return testResults;
    }

    @Override
    public String toString() {
        return testInteractions.toString();
    }
}