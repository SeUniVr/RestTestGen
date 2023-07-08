package io.resttestgen.implementation.helper;

import io.resttestgen.boot.ApiUnderTest;
import io.resttestgen.boot.Starter;
import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.ParameterName;
import io.resttestgen.core.datatype.rule.*;
import io.resttestgen.core.openapi.CannotParseOpenApiException;
import io.resttestgen.core.openapi.Operation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class RuleCombinationHelperTest {

    private static Environment environment;

    @BeforeAll
    public static void setUp() throws CannotParseOpenApiException, IOException {
        environment = Starter.initEnvironment(ApiUnderTest.loadTestApiFromFile("rule-combination"));
    }

    /**
     * If all the n rules are compatible with each other, then the total number of combinations should be 2^n
     */
    @Test
    public void testCountOfValidCombinations() {

        // Get the first operation from the specification
        Operation operation = environment.getOpenAPI().getOperations().stream().findFirst().get().deepClone();

        // Get set of rules of the operation
        HashSet<Rule> rules = operation.getRulesToValidate();

        // Artificially add test rules to the operation
        rules.add(new RequiredRule(new ParameterName("a"), true));

        rules.add(new DefaultRule(new ParameterName("a"), 20));

        rules.add(new MinimumRule(new ParameterName("b"), 10));

        rules.add(new RequiresRule("a==10", "b"));

        // Init of rule combination helper
        RulesCombinationHelper rulesCombinationHelper = new RulesCombinationHelper(operation);

        int count = 0;
        while (true) {
            List<Rule> staticallyValidatedRules = rulesCombinationHelper.getNextStaticallyValidCombination();
            System.out.println(staticallyValidatedRules);

            count++;

            if (staticallyValidatedRules.stream().allMatch(Rule::isAlwaysApplicable)) {
                break;
            }
        }

        Assertions.assertEquals(Math.pow(2, rules.size()), count);
    }

    @Test
    public void testValidation() {

        // Get the first operation from the specification
        Operation operation = environment.getOpenAPI().getOperations().stream().findFirst().get().deepClone();

        // Get set of rules of the operation
        HashSet<Rule> rules = operation.getRulesToValidate();

        // Artificially add test rules to the operation
        rules.add(new RequiredRule(new ParameterName("a"), true));
        rules.add(new RequiredRule(new ParameterName("b"), true));
        rules.add(new RequiredRule(new ParameterName("c"), true));
        rules.add(new RemoveRule(new ParameterName("b"), true));

        HashSet<ParameterName> zero = new HashSet<>();
        zero.add(new ParameterName("a"));
        zero.add(new ParameterName("b"));
        zero.add(new ParameterName("c"));
        rules.add(new OrRule(zero));

        HashSet<ParameterName> one = new HashSet<>();
        one.add(new ParameterName("d"));
        one.add(new ParameterName("e"));
        rules.add(new OrRule(one));

        HashSet<ParameterName> two = new HashSet<>();
        two.add(new ParameterName("a"));
        two.add(new ParameterName("b"));
        rules.add(new OrRule(two));

        HashSet<ParameterName> three = new HashSet<>();
        three.add(new ParameterName("c"));
        three.add(new ParameterName("d"));
        rules.add(new OrRule(three));

        HashSet<ParameterName> four = new HashSet<>();
        four.add(new ParameterName("b"));
        four.add(new ParameterName("c"));
        rules.add(new OrRule(four));

        rules.add(new DefaultRule(new ParameterName("a"), "AAA"));

        rules.add(new MinimumRule(new ParameterName("b"), 10));

        rules.add(new RequiresRule("a==10", "b"));

        //rules.add(new ExampleRule(new ParameterName("a"), "exampleValue"));

        // Init of rule combination helper
        RulesCombinationHelper rulesCombinationHelper = new RulesCombinationHelper(operation);

        List<List<Rule>> combinations = new LinkedList<>();

        while (true) {
            List<Rule> staticallyValidatedRules = rulesCombinationHelper.getNextStaticallyValidCombination();
            combinations.add(staticallyValidatedRules);

            if (staticallyValidatedRules.stream().allMatch(Rule::isAlwaysApplicable)) {
                break;
            }
        }

        Collections.shuffle(combinations);

        combinations = combinations.stream().sorted(Comparator.comparing(List::size, Collections.reverseOrder())).collect(Collectors.toList());

        for (List<Rule> combination : combinations) {
            System.out.println(combination);
        }
    }
}
