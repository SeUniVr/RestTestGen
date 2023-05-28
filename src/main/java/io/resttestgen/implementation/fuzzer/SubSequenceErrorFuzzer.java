package io.resttestgen.implementation.fuzzer;

import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.parameter.leaves.LeafParameter;
import io.resttestgen.core.testing.*;
import io.resttestgen.implementation.mutator.ConstraintViolationMutator;
import io.resttestgen.implementation.mutator.MissingRequiredMutator;
import io.resttestgen.implementation.mutator.WrongTypeMutator;
import io.resttestgen.implementation.oracle.ErrorStatusCodeOracle;
import io.resttestgen.implementation.writer.ReportWriter;
import io.resttestgen.implementation.writer.RestAssuredWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jgrapht.alg.util.Pair;

import java.io.IOException;
import java.util.*;

public class SubSequenceErrorFuzzer extends Fuzzer {

    private static final Logger logger = LogManager.getLogger(Environment.class);

    private final TestSequence testSequenceToMutate;
    private final Set<Mutator> mutators;

    public SubSequenceErrorFuzzer(TestSequence testSequenceToMutate) {
        this.testSequenceToMutate = testSequenceToMutate;
        mutators = new HashSet<>();
        mutators.add(new MissingRequiredMutator());
        mutators.add(new WrongTypeMutator());
        mutators.add(new ConstraintViolationMutator());
    }

    public List<TestSequence> generateTestSequences(int numberOfSequences) {
        List<TestSequence> testSequences = new LinkedList<>();
        // Build up all the subsequences
        for (int i = 1; i <= testSequenceToMutate.size(); i++) {

            // For each sequence, we generate n mutants for the last interaction
            for (int j = 0; j < numberOfSequences; j++) {

                // Get clone of the subsequence
                TestSequence currentTestSequence = testSequenceToMutate.getSubSequence(0, i).deepClone();
                currentTestSequence.setGenerator(this);


                // Get last interaction in the sequence
                TestInteraction mutableInteraction = currentTestSequence.getLast();
                mutableInteraction.addTag("mutated");

                String sequenceName = mutableInteraction.getFuzzedOperation().getOperationId().length() > 0 ?
                        mutableInteraction.getFuzzedOperation().getOperationId() :
                        mutableInteraction.getFuzzedOperation().getMethod().toString() + "-" +
                                mutableInteraction.getFuzzedOperation().getEndpoint();
                currentTestSequence.setName(sequenceName);
                currentTestSequence.appendGeneratedAtTimestampToSequenceName();

                // Get set of applicable mutations to this operation
                Set<Pair<LeafParameter, Mutator>> mutableParameters = new HashSet<>();
                mutableInteraction.getFuzzedOperation().getLeaves().forEach(leaf -> mutators.forEach(mutator -> {
                        if (mutator.isParameterMutable(leaf)) {
                            mutableParameters.add(new Pair<>(leaf, mutator));
                        }
                }));

                // Choose a random mutation pair
                Optional<Pair<LeafParameter, Mutator>> mutable = Environment.getInstance().getRandom().nextElement(mutableParameters);

                if (mutable.isPresent()) {

                    // Apply mutation
                    LeafParameter mutated = mutable.get().getSecond().mutate(mutable.get().getFirst());
                    mutated.addTag("mutated");

                    // Replace original parameter with mutated one
                    if (mutable.get().getFirst().replace(mutated)) {
                        logger.debug("Mutation applied correctly.");
                    } else {
                        logger.warn("Could not apply mutation.");
                    }

                    // Execute test sequence
                    TestRunner testRunner = TestRunner.getInstance();
                    testRunner.run(currentTestSequence);

                    // Evaluate sequence with oracles
                    ErrorStatusCodeOracle errorStatusCodeOracle = new ErrorStatusCodeOracle();
                    errorStatusCodeOracle.assertTestSequence(currentTestSequence);

                    // Write report to file
                    try {
                        ReportWriter reportWriter = new ReportWriter(currentTestSequence);
                        reportWriter.write();
                        RestAssuredWriter restAssuredWriter = new RestAssuredWriter(currentTestSequence);
                        restAssuredWriter.write();
                    } catch (IOException e) {
                        logger.warn("Could not write report to file.");
                    }

                    testSequences.add(currentTestSequence);
                }
            }
        }

        // Return the list of test sequences
        return testSequences;
    }

    private void thoroughApplyMutations() {
        // Compute all combinations of mutators applied to parameters
        /*Set<Set<Pair<ParameterLeaf, Mutator>>> combinations = new HashSet<>();
        for (int j = 1; j <= mutableParameters.size(); j++) {

            // Get all combinations of a given size
            Set<Set<Pair<ParameterLeaf, Mutator>>> allCombinations = Sets.combinations(mutableParameters, j);

            // Remove combinations that apply mutations to the same parameter
            combinations.addAll(allCombinations.stream().filter(c -> {
                for (Pair<ParameterLeaf, Mutator> p1 : c) {
                    Set<Pair<ParameterLeaf, Mutator>> c2 = new HashSet<>(c);
                    c2.remove(p1);
                    for (Pair<ParameterLeaf, Mutator> p2 : c2) {
                        if (Objects.equals(p1.getFirst(), p2.getFirst())) {
                            return false;
                        }
                    }
                }
                return true;
            }).collect(Collectors.toSet()));
        }*/
    }
}
