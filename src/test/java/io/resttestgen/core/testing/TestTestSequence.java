package io.resttestgen.core.testing;

import io.resttestgen.boot.ApiUnderTest;
import io.resttestgen.boot.Starter;
import io.resttestgen.core.Environment;
import io.resttestgen.core.openapi.Operation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class TestTestSequence {

    private static Environment environment;
    static TestSequence testSequence;

    @BeforeAll
    public static void setUp() throws IOException, InterruptedException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        environment = Starter.initEnvironment(ApiUnderTest.loadApiFromFile("petstore"));
        testSequence = new TestSequence();

        for (int i = 0; i < 10; i++) {
            Operation randomOperation = environment.getRandom().nextElement(environment.getOpenAPI().getOperations()).get();
            TestInteraction interaction = new TestInteraction(randomOperation);
            interaction.addTag("count=" + i);
            testSequence.add(interaction);
        }
    }

    @Test
    public void testFor() {
        int index = 0;
        for (TestInteraction testInteraction : testSequence) {
            Assertions.assertEquals(testInteraction, testSequence.get(index));
            index++;
        }
    }

    @Test
    public void testContains() {
        for (TestInteraction testInteraction : testSequence) {
            Assertions.assertTrue(testSequence.contains(testInteraction));
        }
    }
}
