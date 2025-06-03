package io.resttestgen.implementation.helper;

import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.ParameterName;
import io.resttestgen.core.datatype.parameter.Parameter;
import io.resttestgen.core.datatype.parameter.leaves.LeafParameter;
import io.resttestgen.core.datatype.parameter.structured.ArrayParameter;
import io.resttestgen.core.helper.ExtendedRandom;
import io.resttestgen.core.helper.ObjectHelper;
import io.resttestgen.core.openapi.Operation;
import io.resttestgen.core.testing.parametervalueprovider.ParameterValueProvider;
import io.resttestgen.core.testing.parametervalueprovider.ValueNotAvailableException;
import kotlin.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static io.resttestgen.core.datatype.parameter.ParameterUtils.isLeaf;
import static io.resttestgen.core.datatype.parameter.ParameterUtils.isArrayOfLeaves;

public class InterParameterDependenciesHelper {

    private static final Logger logger = LogManager.getLogger(InterParameterDependenciesHelper.class);

    private final Operation operation;
    private final ParameterValueProvider parameterValueProvider;
    private static final ExtendedRandom random = Environment.getInstance().getRandom();

    public InterParameterDependenciesHelper(Operation operation, ParameterValueProvider parameterValueProvider) {
        this.operation = operation;
        this.parameterValueProvider = parameterValueProvider;
    }

    /**
     * Extract example values from requires inter-parameter dependencies.
     * E.g., from IF a=="hi" THEN b=="bye" we extract value "hi" for parameter a, and value "bye" for parameter b.
     */
    public void extractExampleValuesFromRequiresIpds() {
        for (Pair<String, String> requires : operation.getRequires()) {

            List<String> statements = new ArrayList<>();
            statements.add(requires.getFirst());
            statements.add(requires.getSecond());

            for (String statement : statements) {
                if (statement.contains("==")) {
                    Pattern pattern = Pattern.compile("(.*)==(.*)");
                    Matcher matcher = pattern.matcher(statement);
                    if (matcher.find()) {
                        String parameterName = matcher.group(1);
                        String parameterValue = matcher.group(2);

                        HashSet<ParameterName> parameterNames = new HashSet<>();
                        parameterNames.add(new ParameterName(parameterName));
                        List<Parameter> parameters = collectRequestParametersWithNames(operation, parameterNames);

                        // Cut out quotes from string values
                        if (parameterValue.length() > 2 && (parameterValue.startsWith("'") && parameterValue.endsWith("'")) ||
                                (parameterValue.startsWith("\"") && parameterValue.endsWith("\""))) {
                            parameterValue = parameterValue.substring(1, parameterValue.length() - 1);
                        }

                        if (!parameters.isEmpty()) {
                            for (Parameter parameter : parameters) {
                                if (parameter instanceof LeafParameter) {
                                    parameter.addExample(parameterValue);
                                } else if (isArrayOfLeaves(parameter)) {
                                    ((ArrayParameter) parameter).getReferenceElement().addExample(parameterValue);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void applyInterParameterDependencies() {
        applyRequiresIpds();
        applyOrIpds();
        applyOnlyOneIpds();
        applyAllOrNoneIpds();
        applyZeroOrOneIpds();
    }

    public void applyRequiresIpds() {

        for (Pair<String, String> requires : operation.getRequires()) {

            boolean conditionHolds = true;

            // In the case the condition requires a parameter to have a value
            if (requires.getFirst().contains("==")) {
                Pattern pattern = Pattern.compile("(.*)==(.*)");
                Matcher matcher = pattern.matcher(requires.getFirst());
                if (matcher.find()) {
                    String parameterName = matcher.group(1);
                    String parameterValue = matcher.group(2);

                    HashSet<ParameterName> parameterNames = new HashSet<>();
                    parameterNames.add(new ParameterName(parameterName));
                    List<Parameter> conditionParameters = collectRequestParametersWithNames(operation, parameterNames)
                            .stream().filter(p -> p.getParent() == null || !(p.getParent().getName().equals(p.getName()))).collect(Collectors.toList());

                    if (!conditionParameters.isEmpty()) {
                        for (Parameter parameter : conditionParameters) {
                            if (parameter instanceof LeafParameter) {

                                // Cut out quotes from string values
                                if (parameterValue.length() > 2 && (parameterValue.startsWith("'") && parameterValue.endsWith("'")) ||
                                        (parameterValue.startsWith("\"") && parameterValue.endsWith("\""))) {
                                    parameterValue = parameterValue.substring(1, parameterValue.length() - 1);
                                }

                                if (((LeafParameter) parameter).getConcreteValue() == null ||
                                        !(((LeafParameter) parameter).getConcreteValue().toString().equals(parameterValue))) {
                                    conditionHolds = false;
                                    break;
                                }

                            } else if (isArrayOfLeaves(parameter)) {
                                if (!((ArrayParameter) parameter).hasValues(parameterValue)) {
                                    conditionHolds = false;
                                    break;
                                }
                            }
                        }
                    }
                } else {
                    conditionHolds = false;
                }
            }

            // In the case the condition concerns the presence of the parameter
            else {
                HashSet<ParameterName> parameterNames = new HashSet<>();
                parameterNames.add(new ParameterName(requires.getFirst()));
                List<Parameter> conditionParameters = collectRequestParametersWithNames(operation, parameterNames)
                        .stream().filter(p -> p.getParent() == null || !(p.getParent().getName().equals(p.getName()))).collect(Collectors.toList());
                if (!conditionParameters.isEmpty()) {
                    for (Parameter parameter : conditionParameters) {
                        if (parameter instanceof LeafParameter && ((LeafParameter) parameter).getConcreteValue() == null) {
                            conditionHolds = false;
                            break;
                        } else if (isArrayOfLeaves(parameter)) {
                            if (((ArrayParameter) parameter).getElements().isEmpty()) {
                                conditionHolds = false;
                                break;
                            }
                        }
                    }
                } else {
                    conditionHolds = false;
                }
            }

            if (conditionHolds) {

                // If the requirement is a parameter with a specific value, we set the value accordingly
                applyStatementToOperation(requires.getSecond());
            }
        }
    }

    public void applyOrIpds() {
        for (Set<ParameterName> or : operation.getOr()) {
            List<Parameter> orParameters = collectRequestParametersWithNames(operation, or);
            // If none of the or parameters is set, then set a value for a subset of these parameters
            if (orParameters.stream().noneMatch(p -> (p instanceof LeafParameter && ((LeafParameter) p).getConcreteValue() != null) ||
                    (isArrayOfLeaves(p) && ((ArrayParameter) p).getElements().isEmpty()))) {
                random.nextElement(orParameters).ifPresent(this::setValue);
            }
        }
    }

    public void applyOnlyOneIpds() {
        for (Set<ParameterName> onlyOne : operation.getOnlyOne()) {
            List<Parameter> onlyOneParameters = collectRequestParametersWithNames(operation, onlyOne);
            List<Parameter> setOnlyOneParameters = filterBySetParameters(onlyOneParameters);
            // In case all parameters are not set, set one of them with value
            if (setOnlyOneParameters.isEmpty() && !onlyOneParameters.isEmpty()) {
                random.nextElement(onlyOneParameters).ifPresent(this::setValue);
            }
            // In case more than one parameter is set, just keep one and remove all the others
            else if (setOnlyOneParameters.size() > 1) {
                setOnlyOneParameters.remove(random.nextElement(setOnlyOneParameters).get());
                setOnlyOneParameters.forEach(this::removeValue);
            }
        }
    }

    public void applyAllOrNoneIpds() {
        for (Set<ParameterName> allOrNone : operation.getAllOrNone()) {
            List<Parameter> allOrNoneParameters = collectRequestParametersWithNames(operation, allOrNone);
            List<Parameter> setAllOrNoneParameters = filterBySetParameters(allOrNoneParameters);
            if (!(setAllOrNoneParameters.isEmpty() || setAllOrNoneParameters.size() == allOrNoneParameters.size())) {
                boolean allOrNoneChoice = random.nextBoolean(); // true: all, false: none
                if (allOrNoneChoice) {
                    for (Parameter parameter : allOrNoneParameters) {
                        if ((parameter instanceof LeafParameter && ((LeafParameter) parameter).getConcreteValue() == null) ||
                                (isArrayOfLeaves(parameter) && ((ArrayParameter) parameter).getElements().isEmpty())) {
                            setValue(parameter);
                        }
                    }
                } else {
                    allOrNoneParameters.forEach(this::removeValue);
                }
            }
        }
    }

    public void applyZeroOrOneIpds() {
        for (Set<ParameterName> zeroOrOne : operation.getZeroOrOne()) {
            List<Parameter> zeroOrOneParameters = collectRequestParametersWithNames(operation, zeroOrOne);
            List<Parameter> setZeroOrOneParameters = filterBySetParameters(zeroOrOneParameters);
            if (setZeroOrOneParameters.size() > 1) {
                boolean zeroOrOneChoice = random.nextBoolean(); // true: 1, false: 0
                if (zeroOrOneChoice) {
                    setZeroOrOneParameters.remove(random.nextElement(setZeroOrOneParameters).get());
                }
                setZeroOrOneParameters.forEach(this::removeValue);
            }
        }
    }

    public void applyStatementToOperation(String statement) {
        if (statement.contains("==")) {
            Pattern pattern = Pattern.compile("(.*)==(.*)");
            Matcher matcher = pattern.matcher(statement);
            if (matcher.find()) {
                String parameterName = matcher.group(1).trim();
                String parameterValue = matcher.group(2);

                HashSet<ParameterName> parameterNames = new HashSet<>();
                parameterNames.add(new ParameterName(parameterName));
                List<Parameter> conditionParameters = collectRequestParametersWithNames(operation, parameterNames);

                for (Parameter parameter : conditionParameters) {
                    if (parameter instanceof LeafParameter) {
                        try {

                            // Cut out quotes from string values
                            if (parameterValue.length() > 2 && (parameterValue.startsWith("'") && parameterValue.endsWith("'")) ||
                                    (parameterValue.startsWith("\"") && parameterValue.endsWith("\""))) {
                                parameterValue = parameterValue.substring(1, parameterValue.length() - 1);
                            }

                            LeafParameter leafParameter = (LeafParameter) parameter;

                            // If the value is not already complying, apply the complying value
                            if (leafParameter.getConcreteValue() == null || !leafParameter.getConcreteValue().toString().equals(parameterValue)) {
                                Object castedValue = ObjectHelper.castToParameterValueType(parameterValue, leafParameter.getType());
                                leafParameter.setValueManually(castedValue);
                            }
                        } catch (ClassCastException e) {
                            logger.warn("Could not cast value from IPD.");
                        }
                    } else if (isArrayOfLeaves(parameter)) {
                        ArrayParameter arrayParameter = (ArrayParameter) parameter;

                        // If values are not already complying, apply complying values
                        if (!arrayParameter.hasValues(parameterValue)) {

                            arrayParameter.clearElements();
                            arrayParameter.setValuesFromCommaSeparatedString(parameterValue);
                        }
                    }
                }
            }
        }

        // If the requirement is the presence of a parameter, we set the value of the parameter
        else {
            HashSet<ParameterName> parameterNames = new HashSet<>();
            parameterNames.add(new ParameterName(statement.trim()));
            List<Parameter> conditionParameters = collectRequestParametersWithNames(operation, parameterNames);

            for (Parameter parameter : conditionParameters) {
                if (parameter instanceof LeafParameter) {
                    LeafParameter leafParameter = (LeafParameter) parameter;

                    // Add value only if leaf has no value already
                    if (leafParameter.getConcreteValue() == null) {
                        leafParameter.setValueWithProvider(parameterValueProvider);
                    }
                } else if (isArrayOfLeaves(parameter)) {
                    ArrayParameter arrayParameter = (ArrayParameter) parameter;

                    // Add values, only if no values are present
                    if (arrayParameter.getElements().isEmpty()) {
                        int n = random.nextShortLength(arrayParameter.getMinItems(), arrayParameter.getMaxItems());

                        // No elements are not accepted
                        if (n == 0) {
                            n = 1;
                        }

                        for (int i = 0; i < n; i++) {
                            LeafParameter newLeaf = (LeafParameter) arrayParameter.getReferenceElement().deepClone();
                            newLeaf.setValueWithProvider(parameterValueProvider);
                            arrayParameter.addElement(newLeaf);
                        }
                    }
                }
            }
        }
    }

    public void applyNegationOfStatementToOperation(String statement) {
        if (statement.contains("==")) {
            Pattern pattern = Pattern.compile("(.*)==(.*)");
            Matcher matcher = pattern.matcher(statement);
            if (matcher.find()) {
                String parameterName = matcher.group(1).trim();
                String parameterValue = matcher.group(2);

                HashSet<ParameterName> parameterNames = new HashSet<>();
                parameterNames.add(new ParameterName(parameterName));
                List<Parameter> conditionParameters = collectRequestParametersWithNames(operation, parameterNames);

                for (Parameter parameter : conditionParameters) {
                    if (parameter instanceof LeafParameter) {
                        try {

                            // Cut out quotes from string values
                            if (parameterValue.length() > 2 && (parameterValue.startsWith("'") && parameterValue.endsWith("'")) ||
                                    (parameterValue.startsWith("\"") && parameterValue.endsWith("\""))) {
                                parameterValue = parameterValue.substring(1, parameterValue.length() - 1);
                            }

                            LeafParameter leafParameter = (LeafParameter) parameter;

                            // If the value is the one in the statement, change the value
                            if (leafParameter.getConcreteValue() == null || leafParameter.getConcreteValue().toString().equals(parameterValue)) {

                                String newValue = parameterValue;

                                // Generate a new value, different from the one in the statement (100 attempts)
                                for (int i = 0; i < 100 || newValue.equals(parameterValue); i++) {
                                    try {
                                        newValue = parameterValueProvider.provideValueFor(leafParameter).toString();
                                    } catch (ValueNotAvailableException e) {
                                        logger.warn("Could not retrieve a value for parameter {}", leafParameter);
                                    }
                                }

                                Object castedValue = ObjectHelper.castToParameterValueType(parameterValue, leafParameter.getType());
                                leafParameter.setValueManually(castedValue);
                            }
                        } catch (ClassCastException e) {
                            logger.warn("Could not cast value from IPD.");
                        }
                    } else if (isArrayOfLeaves(parameter)) {

                        ArrayParameter arrayParameter = (ArrayParameter) parameter;

                        // If values are complying, remove one of them
                        if (arrayParameter.hasValues(parameterValue) && !arrayParameter.getElements().isEmpty()) {
                            Parameter firstParamenter = arrayParameter.getElements().get(0);
                            if (firstParamenter != null) {
                                //
                            }
                        }
                    }
                }
            }
        }

        // If the requirement is the presence of a parameter, we remove the value of the parameter
        else {
            HashSet<ParameterName> parameterNames = new HashSet<>();
            parameterNames.add(new ParameterName(statement.trim()));
            List<Parameter> conditionParameters = collectRequestParametersWithNames(operation, parameterNames);

            for (Parameter parameter : conditionParameters) {
                if (parameter instanceof LeafParameter) {
                    LeafParameter leafParameter = (LeafParameter) parameter;
                    leafParameter.removeValue();
                } else if (isArrayOfLeaves(parameter)) {
                    ArrayParameter arrayParameter = (ArrayParameter) parameter;
                    arrayParameter.clearElements();
                }
            }
        }
    }

    /**
     * Given a set of parameter names, returns a list with the actual parameter elements with the corresponding names.
     * @param parameterNames a set of parameter names.
     * @return a list of the corresponding parameters.
     */
    @NotNull
    private List<Parameter> collectRequestParametersWithNames(Operation operation, Set<ParameterName> parameterNames) {

        // At the moment, we only support leaves and arrays of leaves
        return operation.getAllRequestParameters().stream()
                .filter(p -> parameterNames.contains(p.getName()) && (isLeaf(p) || isArrayOfLeaves(p)))
                .collect(Collectors.toList());
    }

    /**
     * Filters a list of parameters by only returning the parameter leaves with a value or arrays with at least one
     * element.
     * @param parameters the list of parameters to filter.
     * @return the filtered list of parameters.
     */
    @NotNull
    private List<Parameter> filterBySetParameters(List<Parameter> parameters) {
        return parameters.stream()
                .filter(p -> (p instanceof LeafParameter && ((LeafParameter) p).getConcreteValue() != null) ||
                        (isArrayOfLeaves(p) && !((ArrayParameter) p).getElements().isEmpty()))
                .collect(Collectors.toList());
    }

    public void setValue(Parameter parameter) {
        try {
            if (parameter instanceof LeafParameter) {
                ((LeafParameter) parameter).setValueWithProvider(parameterValueProvider);
            } else if (isArrayOfLeaves(parameter)) {
                ArrayParameter arrayParameter = (ArrayParameter) parameter;
                arrayParameter.setValuesFromCommaSeparatedString(parameterValueProvider.provideValueFor((LeafParameter) arrayParameter.getReferenceElement()).toString());
            }
        } catch (ValueNotAvailableException e) {
            logger.warn("Could not retrieve a value for parameter {}", parameter);
        }
    }

    public void removeValue(Parameter parameter) {
        if (parameter instanceof LeafParameter) {
            ((LeafParameter) parameter).removeValue();
        } else if (isArrayOfLeaves(parameter)) {
            ((ArrayParameter) parameter).clearElements();
        }
    }
}
