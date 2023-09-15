package io.resttestgen.core.helper;

import io.resttestgen.core.datatype.parameter.leaves.NumberParameter;
import io.resttestgen.core.testing.TestResult;
import io.resttestgen.core.testing.TestRunner;
import io.resttestgen.core.testing.TestSequence;
import io.resttestgen.implementation.oracle.StatusCodeOracle;

public class DomainExplorer {
    private final static int MAX_ITERATIONS_ON_DOMAIN = 70;
    private final static StatusCodeOracle statusCodeOracle = new StatusCodeOracle();

    private static long getMiddleValue(long lowerBound, long upperBound) {
        if ((lowerBound <= 0 && upperBound <= 0) || (lowerBound >= 0 && upperBound >= 0)) {
            return lowerBound + (upperBound - lowerBound) / 2;
        }

        return (upperBound + lowerBound) / 2;
    }

    public static Number getMaximumFromDomainExploration(NumberParameter parameter, Number startingNumber, TestSequence sequence) {
        Number newMaximum = null;
        long lowerBound = startingNumber.longValue();
        long upperBound = Long.MAX_VALUE;

        int iterations = 0;
        while (iterations < MAX_ITERATIONS_ON_DOMAIN && lowerBound < upperBound) {
            iterations++;
            long middleValue = getMiddleValue(lowerBound, upperBound);

            parameter.setValueManually(middleValue);
            if (playSequence(sequence).isPass()) {
                lowerBound = middleValue + 1;
                newMaximum = middleValue;
            } else {
                upperBound = middleValue - 1;
            }
        }

        if (iterations == MAX_ITERATIONS_ON_DOMAIN ||
                (newMaximum != null && newMaximum.longValue() == Long.MAX_VALUE)) {
            return null;
        }

        return newMaximum;
    }

    public static Number getMinimumFromDomainExploration(NumberParameter parameter, Number startingNumber, TestSequence sequence) {
        Number newMinimum = null;
        long lowerBound = Long.MIN_VALUE;
        long upperBound =  startingNumber.longValue();

        int iterations = 0;
        while (iterations < MAX_ITERATIONS_ON_DOMAIN && lowerBound < upperBound) {
            iterations++;
            long middleValue = getMiddleValue(lowerBound, upperBound);

            parameter.setValueManually(middleValue);
            if (playSequence(sequence).isPass()) {
                upperBound = middleValue - 1;
                newMinimum = middleValue;
            } else {
                lowerBound = middleValue + 1;
            }
        }

        if (iterations == MAX_ITERATIONS_ON_DOMAIN ||
                (newMinimum != null && newMinimum.longValue() == Long.MIN_VALUE)) {
            return null;
        }

        return newMinimum;
    }

    private static TestResult playSequence(TestSequence sequence) {
        TestSequence clonedSequence = sequence.deepClone().reset();
        TestRunner.getInstance().run(clonedSequence);
        return statusCodeOracle.assertTestSequence(clonedSequence);
    }


}
