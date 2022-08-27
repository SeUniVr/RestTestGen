package io.resttestgen.core.testing;

import io.resttestgen.core.Configuration;
import io.resttestgen.core.Environment;
import io.resttestgen.core.openapi.CannotParseOpenAPIException;
import io.resttestgen.core.openapi.Operation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class TestTestSequence {

    private static final Environment e = Environment.getInstance();
    static TestSequence testSequence;

    @BeforeAll
    public static void setUp() throws CannotParseOpenAPIException, IOException, InterruptedException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {

        Configuration configuration = new Configuration(true);
        e.setUp(configuration);

        testSequence = new TestSequence();

        for (int i = 0; i < 10; i++) {
            Operation randomOperation = e.getRandom().nextElement(e.getOpenAPI().getOperations()).get();
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
