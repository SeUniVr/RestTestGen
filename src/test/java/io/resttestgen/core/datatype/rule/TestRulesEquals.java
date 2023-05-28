package io.resttestgen.core.datatype.rule;

import io.resttestgen.core.datatype.ParameterName;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashSet;

public class TestRulesEquals {

    @Test
    public void allOrNoneEquals() {
        HashSet<ParameterName> parameterNamesA = new HashSet<>();
        parameterNamesA.add(new ParameterName("a"));
        parameterNamesA.add(new ParameterName("B"));
        AllOrNoneRule a = new AllOrNoneRule(parameterNamesA);

        HashSet<ParameterName> parameterNamesB = new HashSet<>();
        parameterNamesB.add(new ParameterName("a"));
        parameterNamesB.add(new ParameterName("B"));
        AllOrNoneRule b = new AllOrNoneRule(parameterNamesB);

        Assertions.assertEquals(a, b);

        HashSet<Rule> rules = new HashSet<>();
        rules.add(a);
        rules.add(b);
        Assertions.assertEquals(1, rules.size());
    }

    @Test
    public void defaultEquals() {
        DefaultRule defaultRuleA = new DefaultRule(new ParameterName("a"), "ciao");
        DefaultRule defaultRuleB = new DefaultRule(new ParameterName("a"), "ciao");
        DefaultRule defaultRuleC = new DefaultRule(new ParameterName("aaaa"), "ciao");
        DefaultRule defaultRuleD = new DefaultRule(new ParameterName("a"), "ciaooooo");

        Assertions.assertEquals(defaultRuleA, defaultRuleB);
        Assertions.assertNotEquals(defaultRuleA, defaultRuleC);
        Assertions.assertNotEquals(defaultRuleA, defaultRuleD);

        HashSet<Rule> rules = new HashSet<>();
        rules.add(defaultRuleA);
        rules.add(defaultRuleB);

        Assertions.assertEquals(1, rules.size());

        rules.add(defaultRuleC);

        Assertions.assertEquals(2, rules.size());
    }

    @Test
    public void enumEquals() {

    }
}
