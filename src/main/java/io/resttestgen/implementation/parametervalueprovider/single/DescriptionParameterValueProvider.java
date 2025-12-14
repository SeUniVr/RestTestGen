package io.resttestgen.implementation.parametervalueprovider.single;

import io.resttestgen.core.datatype.parameter.leaves.LeafParameter;
import io.resttestgen.core.testing.parametervalueprovider.CountableParameterValueProvider;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts possible values from the description if the content matches some specific patterns, such as quoted values,
 * items of a list, etc.
 */
public class DescriptionParameterValueProvider extends CountableParameterValueProvider {

    List<String> regularExpressions = List.of("(\\w,)", "'([^']+)'", "`([^`]+)", "\"([^\"]+)\"");

    @Override
    protected Collection<Object> collectValuesFor(LeafParameter leafParameter) {

        String description = leafParameter.getDescription();
        Collection<Object> foundValues = new LinkedList<>();

        // Match with regular expressions
        if (description != null && description.length() > 3) {
            for (String regularExpression : regularExpressions) {
                Pattern pattern = Pattern.compile(regularExpression);
                Matcher matcher = pattern.matcher(description);
                while (matcher.find()) {
                    String value = matcher.group(1);

                    // Clean symbols
                    if (value.startsWith("\"") || value.startsWith("'") || value.startsWith("`")) {
                        value = value.substring(1);
                    }
                    if (value.endsWith(",") || value.endsWith("\"") || value.endsWith("'") || value.endsWith("`")) {
                        value = value.substring(0, value.length() - 1);
                    }

                    foundValues.add(value);
                }
            }
        }

        return foundValues;
    }
}
