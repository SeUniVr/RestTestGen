package io.resttestgen.core.datatype.parameter;

import io.resttestgen.core.TestingOperationGenerator;
import io.resttestgen.core.openapi.Operation;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.Random;

public class TestGenerators {

    public void generateString() {

        Random random = new Random();
        Integer minLength = 5;
        Integer maxLength = 10;


        // Characters that are common in strings
        String commonChars = "1234567890qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM";

        // Characters that are special, but not rare
        String specialChars = ".:,;-_#+*'\"?=/\\!$@&%€£<>";

        // Characters that are rare
        String rareChars = "àèìòùéç§°^|";

        // StringBuilder to which random characters are appended
        StringBuilder generatedString = new StringBuilder();

        // Pick a random length for the string from a gaussian distribution. Abs to avoid negative lengths.
        int length = (int) (Math.abs(random.nextGaussian() * 10.0));


        // If only minlength is provided (correctly)
        if (minLength != null && minLength >= 0) {
            length += minLength;
        }

        // If the parameter has bounded lengths defined properly, we adjust the randomly generated length to match
        if (maxLength != null && minLength != null && maxLength >= minLength && minLength >= 0) {
            int range = maxLength - minLength;
            length = (length % range) + minLength;
        }

        String sourceString = commonChars;
        for (int i = 0; i < length; i++) {
            int p = random.nextInt(1000);
            if (p < 955) {
                sourceString = commonChars;
            } else if (p < 992) {
                sourceString = specialChars;
            } else {
                sourceString = rareChars;
            }
            int index = random.nextInt(sourceString.length());
            generatedString.append(sourceString.charAt(index));
        }

        System.out.println("Generated string value for parameter: " + generatedString);
    }


    @Test
    public void generateStrings() {
        for (int i = 0; i < 20; i++) {
            generateString();
        }
    }


    @Test
    public void testGenerators() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        Operation operation = TestingOperationGenerator.getTestingOperation();
        System.out.println(operation.getReferenceLeaves());
    }
}
