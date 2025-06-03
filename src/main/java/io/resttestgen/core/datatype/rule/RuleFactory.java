package io.resttestgen.core.datatype.rule;

import io.resttestgen.core.datatype.ParameterName;
import io.resttestgen.core.datatype.parameter.attributes.ParameterType;
import io.resttestgen.core.datatype.parameter.attributes.ParameterTypeFormat;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RuleFactory {

    public Rule createRule(ParameterName parameterName, Pair<String, String> rule) {
        if (parameterName.toString().isEmpty()) {
            return null;
        }
        switch (rule.getFirst()) {
            case "default":
                return new DefaultRule(parameterName, rule.getSecond());
            case "enum":
                return new EnumRule(parameterName, rule.getSecond());
            case "example":
                return new ExampleRule(parameterName, rule.getSecond());
            case "required":
                boolean required = Boolean.parseBoolean(rule.getSecond());
                return new RequiredRule(parameterName, required);
            case "remove":
                boolean remove = Boolean.parseBoolean(rule.getSecond());
                return new RemoveRule(parameterName, remove);
            case "minimum":
                double minimum = Double.parseDouble(rule.getSecond());
                return new MinimumRule(parameterName, minimum);
            case "maximum":
                double maximum = Double.parseDouble(rule.getSecond());
                return new MaximumRule(parameterName, maximum);
            case "exclusiveMinimum":
                return new ExclusiveMinimumRule(parameterName, Boolean.parseBoolean(rule.getSecond()));
            case "exclusiveMaximum":
                return new ExclusiveMaximumRule(parameterName, Boolean.parseBoolean(rule.getSecond()));
            case "type":
                return new TypeRule(parameterName, ParameterType.getTypeFromString(rule.getSecond()));
            case "format":
                return new FormatRule(parameterName, ParameterTypeFormat.getFormatFromString(rule.getSecond()));
            case "collectionFormat":
                return new CollectionFormatRule(parameterName, rule.getSecond());
            case "IPD":
                if (rule.getSecond().contains("IF ") && rule.getSecond().contains(" THEN ")) {
                    // FIXME: discard complex rules
                    Pattern pattern = Pattern.compile("IF (.*) THEN (.*);");
                    Matcher matcher = pattern.matcher(rule.getSecond());
                    if (matcher.find()) {
                        return new RequiresRule(matcher.group(1), matcher.group(2));
                    } else {
                        System.out.println("DISCARDED RULE FROM VALIDATION BECAUSE NOT SUPPORTED BY RTG: " + parameterName + " -- > " + rule);
                        return null;
                    }
                } else if (rule.getSecond().startsWith("Or(")) {
                    return new OrRule(extractParameterNamesFromIpdRule(rule.getSecond()));
                } else if (rule.getSecond().contains("OnlyOne(")) {
                    return new OnlyOneRule(extractParameterNamesFromIpdRule(rule.getSecond()));
                } else if (rule.getSecond().contains("AllOrNone(")) {
                    return new AllOrNoneRule(extractParameterNamesFromIpdRule(rule.getSecond()));
                } else if (rule.getSecond().contains("ZeroOrOne(")) {
                    return new ZeroOrOneRule(extractParameterNamesFromIpdRule(rule.getSecond()));
                } else {
                    System.out.println("DISCARDED RULE FROM VALIDATION BECAUSE NOT SUPPORTED BY RTG: " + parameterName + " -- > " + rule);
                    return null;
                }
            default:
                System.out.println("DISCARDED RULE FROM VALIDATION BECAUSE NOT SUPPORTED BY RTG: " + parameterName + " -- > " + rule);
                return null;
        }
    }

    public Rule createRule(HashSet<ParameterName> parameterNames, Pair<String, String> rule) {
        return null;
    }

    @NotNull
    private HashSet<ParameterName> extractParameterNamesFromIpdRule(String rule) {
        HashSet<ParameterName> parameterNames = new HashSet<>();
        String[] stringParameterNames = rule.split("\\(")[1].split("\\)")[0].split(",");
        for (String stringParameterName : stringParameterNames) {
            parameterNames.add(new ParameterName(stringParameterName));
        }
        return parameterNames;
    }
}
