package io.resttestgen.implementation.helper;

import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.ParameterName;
import io.resttestgen.core.datatype.parameter.ParameterArray;
import io.resttestgen.core.datatype.parameter.ParameterElement;
import io.resttestgen.core.datatype.parameter.ParameterLeaf;
import io.resttestgen.core.helper.ExtendedRandom;
import io.resttestgen.core.helper.ObjectHelper;
import io.resttestgen.core.openapi.Operation;
import io.resttestgen.core.testing.parametervalueprovider.ParameterValueProvider;
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
                        List<ParameterElement> parameters = collectRequestParametersWithNames(operation, parameterNames);

                        // Cut out quotes from string values
                        if (parameterValue.length() > 2 && (parameterValue.startsWith("'") && parameterValue.endsWith("'")) ||
                                (parameterValue.startsWith("\"") && parameterValue.endsWith("\""))) {
                            parameterValue = parameterValue.substring(1, parameterValue.length() - 1);
                        }

                        if (parameters.size() > 0) {
                            for (ParameterElement parameter : parameters) {
                                if (parameter instanceof ParameterLeaf) {
                                    parameter.addExample(parameterValue);
                                } else if (parameter.isArrayOfLeaves()) {
                                    ((ParameterArray) parameter).getReferenceElement().addExample(parameterValue);
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
                    List<ParameterElement> conditionParameters = collectRequestParametersWithNames(operation, parameterNames)
                            .stream().filter(p -> p.getParent() == null || !(p.getParent().getName().equals(p.getName()))).collect(Collectors.toList());

                    if (conditionParameters.size() > 0) {
                        for (ParameterElement parameter : conditionParameters) {
                            if (parameter instanceof ParameterLeaf) {

                                // Cut out quotes from string values
                                if (parameterValue.length() > 2 && (parameterValue.startsWith("'") && parameterValue.endsWith("'")) ||
                                        (parameterValue.startsWith("\"") && parameterValue.endsWith("\""))) {
                                    parameterValue = parameterValue.substring(1, parameterValue.length() - 1);
                                }

                                if (((ParameterLeaf) parameter).getConcreteValue() == null ||
                                        !(((ParameterLeaf) parameter).getConcreteValue().toString().equals(parameterValue))) {
                                    conditionHolds = false;
                                    break;
                                }

                            } else if (parameter.isArrayOfLeaves()) {
                                if (!((ParameterArray) parameter).hasValues(parameterValue)) {
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
                List<ParameterElement> conditionParameters = collectRequestParametersWithNames(operation, parameterNames)
                        .stream().filter(p -> p.getParent() == null || !(p.getParent().getName().equals(p.getName()))).collect(Collectors.toList());
                if (conditionParameters.size() > 0) {
                    for (ParameterElement parameter : conditionParameters) {
                        if (parameter instanceof ParameterLeaf && ((ParameterLeaf) parameter).getConcreteValue() == null) {
                            conditionHolds = false;
                            break;
                        } else if (parameter.isArrayOfLeaves()) {
                            if (((ParameterArray) parameter).getElements().size() == 0) {
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
            List<ParameterElement> orParameters = collectRequestParametersWithNames(operation, or);
            // If none of the or parameters is set, then set a value for a subset of these parameters
            if (orParameters.stream().noneMatch(p -> (p instanceof ParameterLeaf && ((ParameterLeaf) p).getConcreteValue() != null) ||
                    (p.isArrayOfLeaves() && ((ParameterArray) p).getElements().size() == 0))) {
                random.nextElement(orParameters).ifPresent(this::setValue);
            }
        }
    }

    public void applyOnlyOneIpds() {
        for (Set<ParameterName> onlyOne : operation.getOnlyOne()) {
            List<ParameterElement> onlyOneParameters = collectRequestParametersWithNames(operation, onlyOne);
            List<ParameterElement> setOnlyOneParameters = filterBySetParameters(onlyOneParameters);
            // In case all parameters are not set, set one of them with value
            if (setOnlyOneParameters.size() == 0 && onlyOneParameters.size() > 0) {
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
            List<ParameterElement> allOrNoneParameters = collectRequestParametersWithNames(operation, allOrNone);
            List<ParameterElement> setAllOrNoneParameters = filterBySetParameters(allOrNoneParameters);
            if (!(setAllOrNoneParameters.size() == 0 || setAllOrNoneParameters.size() == allOrNoneParameters.size())) {
                boolean allOrNoneChoice = random.nextBoolean(); // true: all, false: none
                if (allOrNoneChoice) {
                    for (ParameterElement parameter : allOrNoneParameters) {
                        if ((parameter instanceof ParameterLeaf && ((ParameterLeaf) parameter).getConcreteValue() == null) ||
                                (parameter.isArrayOfLeaves() && ((ParameterArray) parameter).getElements().size() == 0)) {
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
            List<ParameterElement> zeroOrOneParameters = collectRequestParametersWithNames(operation, zeroOrOne);
            List<ParameterElement> setZeroOrOneParameters = filterBySetParameters(zeroOrOneParameters);
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
                List<ParameterElement> conditionParameters = collectRequestParametersWithNames(operation, parameterNames);

                for (ParameterElement parameter : conditionParameters) {
                    if (parameter instanceof ParameterLeaf) {
                        try {

                            // Cut out quotes from string values
                            if (parameterValue.length() > 2 && (parameterValue.startsWith("'") && parameterValue.endsWith("'")) ||
                                    (parameterValue.startsWith("\"") && parameterValue.endsWith("\""))) {
                                parameterValue = parameterValue.substring(1, parameterValue.length() - 1);
                            }

                            ParameterLeaf parameterLeaf = (ParameterLeaf) parameter;

                            // If the value is not already complying, apply the complying value
                            if (parameterLeaf.getConcreteValue() == null || !parameterLeaf.getConcreteValue().toString().equals(parameterValue)) {
                                Object castedValue = ObjectHelper.castToParameterValueType(parameterValue, parameterLeaf.getType());
                                parameterLeaf.setValue(castedValue);
                            }
                        } catch (ClassCastException e) {
                            logger.warn("Could not cast value from IPD.");
                        }
                    } else if (parameter.isArrayOfLeaves()) {

                        ParameterArray parameterArray = (ParameterArray) parameter;

                        // If values are not already complying, apply complying values
                        if (!parameterArray.hasValues(parameterValue)) {

                            parameterArray.getElements().clear();
                            parameterArray.setValuesFromCommaSeparatedString(parameterValue);
                        }
                    }
                }
            }
        }

        // If the requirement is the presence of a parameter, we set the value of the parameter
        else {
            HashSet<ParameterName> parameterNames = new HashSet<>();
            parameterNames.add(new ParameterName(statement.trim()));
            List<ParameterElement> conditionParameters = collectRequestParametersWithNames(operation, parameterNames);

            for (ParameterElement parameter : conditionParameters) {
                if (parameter instanceof ParameterLeaf) {
                    ParameterLeaf parameterLeaf = (ParameterLeaf) parameter;

                    // Add value only if leaf has no value already
                    if (parameterLeaf.getConcreteValue() == null) {
                        parameterLeaf.setValue(parameterValueProvider.provideValueFor((ParameterLeaf) parameter));
                    }
                } else if (parameter.isArrayOfLeaves()) {
                    ParameterArray parameterArray = (ParameterArray) parameter;

                    // Add values, only if no values are present
                    if (parameterArray.getElements().size() == 0) {
                        int n = random.nextShortLength(parameterArray.getMinItems(), parameterArray.getMaxItems());

                        // No elements are not accepted
                        if (n == 0) {
                            n = 1;
                        }

                        for (int i = 0; i < n; i++) {
                            ParameterLeaf newLeaf = (ParameterLeaf) parameterArray.getReferenceElement().deepClone();
                            newLeaf.setValue(parameterValueProvider.provideValueFor(newLeaf));
                            parameterArray.addElement(newLeaf);
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
                List<ParameterElement> conditionParameters = collectRequestParametersWithNames(operation, parameterNames);

                for (ParameterElement parameter : conditionParameters) {
                    if (parameter instanceof ParameterLeaf) {
                        try {

                            // Cut out quotes from string values
                            if (parameterValue.length() > 2 && (parameterValue.startsWith("'") && parameterValue.endsWith("'")) ||
                                    (parameterValue.startsWith("\"") && parameterValue.endsWith("\""))) {
                                parameterValue = parameterValue.substring(1, parameterValue.length() - 1);
                            }

                            ParameterLeaf parameterLeaf = (ParameterLeaf) parameter;

                            // If the value is the one in the statement, change the value
                            if (parameterLeaf.getConcreteValue() == null || parameterLeaf.getConcreteValue().toString().equals(parameterValue)) {

                                String newValue = parameterValue;

                                // Generate a new value, different from the one in the statement (100 attempts)
                                for (int i = 0; i < 100 || newValue.equals(parameterValue); i++) {
                                    newValue = parameterValueProvider.provideValueFor(parameterLeaf).toString();
                                }

                                Object castedValue = ObjectHelper.castToParameterValueType(parameterValue, parameterLeaf.getType());
                                parameterLeaf.setValue(castedValue);
                            }
                        } catch (ClassCastException e) {
                            logger.warn("Could not cast value from IPD.");
                        }
                    } else if (parameter.isArrayOfLeaves()) {

                        ParameterArray parameterArray = (ParameterArray) parameter;

                        // If values are complying, remove one of them
                        if (parameterArray.hasValues(parameterValue) && parameterArray.getElements().size() > 0) {
                            parameterArray.getElements().remove(0);
                        }
                    }
                }
            }
        }

        // If the requirement is the presence of a parameter, we remove the value of the parameter
        else {
            HashSet<ParameterName> parameterNames = new HashSet<>();
            parameterNames.add(new ParameterName(statement.trim()));
            List<ParameterElement> conditionParameters = collectRequestParametersWithNames(operation, parameterNames);

            for (ParameterElement parameter : conditionParameters) {
                if (parameter instanceof ParameterLeaf) {
                    ParameterLeaf parameterLeaf = (ParameterLeaf) parameter;
                    parameterLeaf.removeValue();
                } else if (parameter.isArrayOfLeaves()) {
                    ParameterArray parameterArray = (ParameterArray) parameter;
                    parameterArray.getElements().clear();
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
    private List<ParameterElement> collectRequestParametersWithNames(Operation operation, Set<ParameterName> parameterNames) {

        // At the moment, we only support leaves and arrays of leaves
        return operation.getAllRequestParameters().stream()
                .filter(p -> parameterNames.contains(p.getName()) && (p.isLeaf() || p.isArrayOfLeaves()))
                .collect(Collectors.toList());
    }

    /**
     * Filters a list of parameters by only returning the parameter leaves with a value or arrays with at least one
     * element.
     * @param parameters the list of parameters to filter.
     * @return the filtered list of parameters.
     */
    @NotNull
    private List<ParameterElement> filterBySetParameters(List<ParameterElement> parameters) {
        return parameters.stream()
                .filter(p -> (p instanceof ParameterLeaf && ((ParameterLeaf) p).getConcreteValue() != null) ||
                        (p.isArrayOfLeaves() && ((ParameterArray) p).getElements().size() > 0))
                .collect(Collectors.toList());
    }

    public void setValue(ParameterElement parameter) {
        if (parameter instanceof ParameterLeaf) {
            ((ParameterLeaf) parameter).setValue(parameterValueProvider.provideValueFor((ParameterLeaf) parameter));
        } else if (parameter.isArrayOfLeaves()) {
            ParameterArray parameterArray = (ParameterArray) parameter;
            parameterArray.setValuesFromCommaSeparatedString(parameterValueProvider.provideValueFor((ParameterLeaf) parameterArray.getReferenceElement()).toString());
        }
    }

    public void removeValue(ParameterElement parameter) {
        if (parameter instanceof ParameterLeaf) {
            ((ParameterLeaf) parameter).removeValue();
        } else if (parameter.isArrayOfLeaves()) {
            ((ParameterArray) parameter).getElements().clear();
        }
    }
}
