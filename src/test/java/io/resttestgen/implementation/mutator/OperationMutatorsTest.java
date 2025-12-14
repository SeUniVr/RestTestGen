package io.resttestgen.implementation.mutator;

import io.resttestgen.boot.ApiUnderTest;
import io.resttestgen.boot.Starter;
import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.HttpMethod;
import io.resttestgen.core.datatype.parameter.leaves.LeafParameter;
import io.resttestgen.core.openapi.Operation;
import io.resttestgen.core.testing.parametervalueprovider.ParameterValueProviderCachedFactory;
import io.resttestgen.implementation.mutator.operation.AddParameterOperationMutator;
import io.resttestgen.implementation.mutator.operation.HttpMethodOperationMutator;
import io.resttestgen.implementation.mutator.operation.RefillValueOperationMutator;
import io.resttestgen.implementation.mutator.operation.RemoveParameterOperationMutator;
import io.resttestgen.implementation.parametervalueprovider.ParameterValueProviderType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class OperationMutatorsTest {

    private static final Logger logger = LogManager.getLogger(OperationMutatorsTest.class);

    private static Environment environment;
    private static Operation postBookOperation;

    @BeforeAll
    public static void setUp() throws IOException {
        environment = Starter.initEnvironment(ApiUnderTest.loadApiFromFile("bookstore"));
        postBookOperation = environment.getOpenAPI().getOperations().stream()
                .filter(o -> o.getMethod().equals(HttpMethod.POST))
                .findFirst().get().deepClone();
        LeafParameter parameter = postBookOperation.getLeaves().stream().findFirst().get();
        parameter.setValueWithProvider(ParameterValueProviderCachedFactory.getParameterValueProvider(ParameterValueProviderType.RANDOM));
        parameter.setRequired(false);
    }

    @Test
    public void testAddParameterMutator() {
        AddParameterOperationMutator addParameterOperationMutator = new AddParameterOperationMutator();
        Operation mutatedOperation = addParameterOperationMutator.mutate(postBookOperation);

        long originalParametersCount = postBookOperation.getLeaves().stream().filter(LeafParameter::hasValue).count();
        long mutatedParametersCount = mutatedOperation.getLeaves().stream().filter(LeafParameter::hasValue).count();

        Assertions.assertEquals(originalParametersCount + 1, mutatedParametersCount);
    }

    @Test
    public void testRemoveParameterMutator() {
        RemoveParameterOperationMutator removeParameterOperationMutator = new RemoveParameterOperationMutator();
        Operation mutatedOperation = removeParameterOperationMutator.mutate(postBookOperation);

        long originalParametersCount = postBookOperation.getLeaves().stream().filter(LeafParameter::hasValue).count();
        long mutatedParametersCount = mutatedOperation.getLeaves().stream().filter(LeafParameter::hasValue).count();

        Assertions.assertEquals(originalParametersCount - 1, mutatedParametersCount);
    }

    @Test
    public void testHttpMethodMutator() {
        HttpMethodOperationMutator httpMethodOperationMutator = new HttpMethodOperationMutator();
        Operation mutatedOperation = httpMethodOperationMutator.mutate(postBookOperation);

        HttpMethod original = postBookOperation.getMethod();
        HttpMethod mutated = mutatedOperation.getMethod();

        Assertions.assertNotEquals(original, mutated);
    }

    @Test
    public void testRefillValueMutator() {
        RefillValueOperationMutator refillValueOperationMutator = new RefillValueOperationMutator();
        Operation mutatedOperation = refillValueOperationMutator.mutate(postBookOperation);

        String originalStringValue = postBookOperation.getLeaves().stream().filter(LeafParameter::hasValue).findFirst().get().getConcreteValue().toString();
        String mutatedStringValue = mutatedOperation.getLeaves().stream().filter(LeafParameter::hasValue).findFirst().get().getConcreteValue().toString();

        Assertions.assertNotEquals(originalStringValue, mutatedStringValue);
    }
}
