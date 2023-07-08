package io.resttestgen.implementation.parametervalueprovider.single;

import com.google.gson.Gson;
import io.resttestgen.boot.ApiUnderTest;
import io.resttestgen.boot.Starter;
import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.parameter.leaves.NumberParameter;
import io.resttestgen.core.datatype.parameter.leaves.StringParameter;
import io.resttestgen.core.openapi.CannotParseOpenApiException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

public class RandomParameterValueProviderTest {

    private static Environment environment = Environment.getInstance();
    private static RandomParameterValueProvider randomParameterValueProvider;
    private static final int NUM_TESTS = 1000;
    private static final boolean printGeneratedValues = false;

    private static final String longParameterJSON = "{\"name\":\"bookId\",\"description\":\"The unique identifier of the book in the system.\",\"in\":\"path\",\"required\":true,\"schema\":{\"type\":\"integer\",\"format\":\"int64\"}}";
    private static Map<String, Object> longParameterMap;

    private static final String intParameterJSON = "{\"name\":\"bookId\",\"description\":\"The unique identifier of the book in the system.\",\"in\":\"path\",\"required\":true,\"schema\":{\"type\":\"integer\"}}";
    private static Map<String, Object> intParameterMap;

    private static final String doubleParameterJSON = "{\"name\":\"bookId\",\"description\":\"The unique identifier of the book in the system.\",\"in\":\"path\",\"required\":true,\"schema\":{\"type\":\"number\",\"format\":\"double\"}}";
    private static Map<String, Object> doubleParameterMap;

    private static final String floatParameterJSON = "{\"name\":\"bookId\",\"description\":\"The unique identifier of the book in the system.\",\"in\":\"path\",\"required\":true,\"schema\":{\"type\":\"number\",\"format\":\"float\"}}";
    private static Map<String, Object> floatParameterMap;

    private static final String stringParameterJSON = "{\"name\":\"title\",\"description\":\"The title of the book.\",\"in\":\"query\",\"schema\":{\"type\":\"string\"}}";
    private static Map<String, Object> stringParameterMap;

    private static final String booleanParameterJSON = "{\"name\":\"title\",\"description\":\"The title of the book.\",\"in\":\"query\",\"schema\":{\"type\":\"boolean\"}}";
    private static Map<String, Object> booleanParameterMap;


    @BeforeAll
    public static void setUp() throws CannotParseOpenApiException, IOException, InterruptedException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        Starter.initEnvironment(ApiUnderTest.loadApiFromFile("petstore"));
        randomParameterValueProvider = new RandomParameterValueProvider();
        Gson gson = new Gson();
        longParameterMap = gson.fromJson(longParameterJSON, Map.class);
        intParameterMap = gson.fromJson(intParameterJSON, Map.class);
        doubleParameterMap = gson.fromJson(doubleParameterJSON, Map.class);
        floatParameterMap = gson.fromJson(floatParameterJSON, Map.class);
        stringParameterMap = gson.fromJson(stringParameterJSON, Map.class);
        booleanParameterMap = gson.fromJson(booleanParameterJSON, Map.class);
    }

    @Test
    public void testLongNumberParameter() {
        NumberParameter numberParameter = new NumberParameter(longParameterMap, null);
        for (int i = 0; i < NUM_TESTS; i++) {
            Object generated = randomParameterValueProvider.provideValueFor(numberParameter);
            if (printGeneratedValues) {
                System.out.println("Generated long: " + generated);
            }
            Assertions.assertInstanceOf(Long.class, generated);
        }
    }

    @Test
    public void testLongNumberParameterWithConstraints() {
        NumberParameter numberParameter = new NumberParameter(longParameterMap, null);
        double MIN = -50.;
        double MAX = 150.;
        numberParameter.setMinimum(MIN);
        numberParameter.setMaximum(MAX);
        for (int i = 0; i < NUM_TESTS; i++) {
            Object generated = randomParameterValueProvider.provideValueFor(numberParameter);
            if (printGeneratedValues) {
                System.out.println("Generated long: " + generated);
            }
            Assertions.assertInstanceOf(Long.class, generated);
            Assertions.assertTrue((long) generated >= (long) MIN && (long) generated <= MAX);
        }
    }

    @Test
    public void testIntegerNumberParameter() {
        NumberParameter numberParameter = new NumberParameter(intParameterMap, null);
        for (int i = 0; i < NUM_TESTS; i++) {
            Object generated = randomParameterValueProvider.provideValueFor(numberParameter);
            if (printGeneratedValues) {
                System.out.println("Generated integer: " + generated);
            }
            Assertions.assertInstanceOf(Integer.class, generated);
        }
    }

    @Test
    public void testIntegerNumberParameterWithConstraints() {
        NumberParameter numberParameter = new NumberParameter(intParameterMap, null);
        double MIN = -50.;
        double MAX = 150.;
        numberParameter.setMinimum(MIN);
        numberParameter.setMaximum(MAX);
        for (int i = 0; i < NUM_TESTS; i++) {
            Object generated = randomParameterValueProvider.provideValueFor(numberParameter);
            if (printGeneratedValues) {
                System.out.println("Generated integer: " + generated);
            }
            Assertions.assertInstanceOf(Integer.class, generated);
            Assertions.assertTrue((int) generated >= MIN && (int) generated <= MAX);
        }
    }

    @Test
    public void testDoubleNumberParameter() {
        NumberParameter numberParameter = new NumberParameter(doubleParameterMap, null);
        for (int i = 0; i < NUM_TESTS; i++) {
            Object generated = randomParameterValueProvider.provideValueFor(numberParameter);
            if (printGeneratedValues) {
                System.out.println("Generated double: " + generated);
            }
            Assertions.assertInstanceOf(Double.class, generated);
        }
    }

    @Test
    public void testDoubleNumberParameterWithConstraints() {
        NumberParameter numberParameter = new NumberParameter(doubleParameterMap, null);
        double MIN = -50.;
        double MAX = 150.;
        numberParameter.setMinimum(MIN);
        numberParameter.setMaximum(MAX);
        for (int i = 0; i < NUM_TESTS; i++) {
            Object generated = randomParameterValueProvider.provideValueFor(numberParameter);
            if (printGeneratedValues) {
                System.out.println("Generated double: " + generated);
            }
            Assertions.assertInstanceOf(Double.class, generated);
            Assertions.assertTrue((double) generated >= MIN && (double) generated <= MAX);
        }
    }

    @Test
    public void testFloatNumberParameter() {
        NumberParameter numberParameter = new NumberParameter(floatParameterMap, null);
        for (int i = 0; i < NUM_TESTS; i++) {
            Object generated = randomParameterValueProvider.provideValueFor(numberParameter);
            if (printGeneratedValues) {
                System.out.println("Generated float: " + generated);
            }
            Assertions.assertInstanceOf(Float.class, generated);
        }
    }

    @Test
    public void testFloatNumberParameterWithConstraints() {
        NumberParameter numberParameter = new NumberParameter(floatParameterMap, null);
        double MIN = -50.;
        double MAX = 150.;
        numberParameter.setMinimum(MIN);
        numberParameter.setMaximum(MAX);
        for (int i = 0; i < NUM_TESTS; i++) {
            Object generated = randomParameterValueProvider.provideValueFor(numberParameter);
            if (printGeneratedValues) {
                System.out.println("Generated float: " + generated);
            }
            Assertions.assertInstanceOf(Float.class, generated);
            Assertions.assertTrue((float) generated >= MIN && (float) generated <= MAX);
        }
    }

    @Test
    public void testStringParameter() {
        StringParameter stringParameter = new StringParameter(stringParameterMap, null);
        for (int i = 0; i < NUM_TESTS; i++) {
            Object generated = randomParameterValueProvider.provideValueFor(stringParameter);
            if (printGeneratedValues) {
                System.out.println("Generated string: " + generated);
            }
            Assertions.assertInstanceOf(String.class, generated);
        }
    }

    /*
    @Test
    public void testStringParameterWithConstraint() {
        StringParameter stringParameter = new StringParameter(null, stringParameterMap, null, null);
        int MIN_LENGTH = 3;
        int MAX_LENGTH = 6;
        stringParameter.setMinLength(MIN_LENGTH);
        stringParameter.setMaxLength(MAX_LENGTH);
        for (int i = 0; i < NUM_TESTS; i++) {
            Object generated = randomParameterValueProvider.provideValueFor(stringParameter);
            if (printGeneratedValues) {
                System.out.println("Generated string: " + generated);
            }
            Assertions.assertInstanceOf(String.class, generated);
            Assertions.assertTrue(((String) generated).length() >= MIN_LENGTH &&
                    ((String) generated).length() <= MAX_LENGTH);
        }
    }
    */
}
