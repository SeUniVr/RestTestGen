package io.resttestgen.implementation.helper;

import com.google.common.collect.Sets;
import com.google.common.primitives.Booleans;
import io.resttestgen.core.datatype.ParameterName;
import io.resttestgen.core.datatype.parameter.Parameter;
import io.resttestgen.core.datatype.parameter.attributes.ParameterType;
import io.resttestgen.core.datatype.parameter.attributes.ParameterTypeFormat;
import io.resttestgen.core.datatype.parameter.leaves.NumberParameter;
import io.resttestgen.core.datatype.parameter.structured.ArrayParameter;
import io.resttestgen.core.datatype.rule.*;
import io.resttestgen.core.helper.ObjectHelper;
import io.resttestgen.core.openapi.Operation;
import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class RulesCombinationHelper {

    private final Operation operation;
    private final Set<ParameterName> operationParameters;
    private final List<Rule> rulesToValidate;
    private final Set<ParameterName> removedParameters = new HashSet<>();
    
    private final List<Rule> presenceRules;
    private final List<Rule> setIpdRules;
    private final List<Rule> singleRules;
    private final List<Rule> constraintRules;
    private final List<Rule> requireCombinatorialValidationRules;
    private final List<Rule> alwaysApplicableRules;
    private boolean[] combinationHotEncoding;
    private static final long MAX_NUMBER_OF_PRETESTED_COMBINATIONS = 1048576;
    private static final long MAX_NUMBER_OF_PRETESTED_COMBINATIONS_PER_ITERATION = 16384;
    private final Set<boolean[]> validCombinations = new HashSet<>();

    // Final results
    private final Set<Rule> rulesRelatedToNonExistentParameters = new HashSet<>();


    
    public RulesCombinationHelper(Operation operation) {
        this.operation = operation;
        operationParameters = operation.getAllRequestParameters().stream()
                .filter(e -> !(e.getParent() instanceof ArrayParameter))
                .map(Parameter::getName)
                .collect(Collectors.toSet());
        rulesToValidate = operation.getRulesToValidate().stream()
                .filter(r -> operationParameters.containsAll(r.getParameterNames()))
                .sorted(Comparator.comparing(Rule::getRuleType))
                .collect(Collectors.toList());

        presenceRules = rulesToValidate.stream().filter(Rule::isPresenceRule).collect(Collectors.toList());

        setIpdRules = rulesToValidate.stream()
                .filter(r -> r.isSetIpdRule() && Collections.disjoint(r.getParameterNames(), removedParameters))
                .collect(Collectors.toList());

        singleRules = rulesToValidate.stream()
                .filter(r -> r.isSingleRule() && Collections.disjoint(r.getParameterNames(), removedParameters))
                .collect(Collectors.toList());

        constraintRules = rulesToValidate.stream()
                .filter(r -> r.isConstraintRule() && Collections.disjoint(r.getParameterNames(), removedParameters))
                .collect(Collectors.toList());

        requireCombinatorialValidationRules = rulesToValidate.stream()
                .filter(r -> r.requiresCombinatorialValidation() && Collections.disjoint(r.getParameterNames(), removedParameters))
                .collect(Collectors.toList());

        alwaysApplicableRules = rulesToValidate.stream()
                .filter(r -> r.isAlwaysApplicable() && Collections.disjoint(r.getParameterNames(), removedParameters))
                .collect(Collectors.toList());

        combinationHotEncoding = new boolean[presenceRules.size() + setIpdRules.size() + singleRules.size() +
                constraintRules.size() + requireCombinatorialValidationRules.size()];

        // Precompute
        for (long i = 0; i < MAX_NUMBER_OF_PRETESTED_COMBINATIONS; i++) {

            // Precompute combination and add it to the set
            boolean[] currentCombination = precomputeNextStaticallyValidCombination();
            validCombinations.add(currentCombination);

            // Break the cycle if the empty combination has been reached
            if (combinationHotEncoding.length == 0 || !BooleanUtils.or(combinationHotEncoding)) {
                break;
            }
        }
    }


    @NotNull
    public List<Rule> getNextStaticallyValidCombination() {

        // If not all combinations have been precomputed
        if (combinationHotEncoding.length > 0 && BooleanUtils.or(combinationHotEncoding)) {

            for (long i = 0; i < MAX_NUMBER_OF_PRETESTED_COMBINATIONS_PER_ITERATION; i++) {

                // Precompute combination and add it to the set
                boolean[] currentCombination = precomputeNextStaticallyValidCombination();
                validCombinations.add(currentCombination);

                // Break the cycle if the empty combination has been reached
                if (combinationHotEncoding.length == 0 || !BooleanUtils.or(combinationHotEncoding)) {
                    break;
                }
            }
        }

        boolean[] staticallyValidCombinationWithMostRules;

        if (validCombinations.size() > 0) {
            staticallyValidCombinationWithMostRules = validCombinations.stream()
                    .sorted(Comparator.comparing(c -> Booleans.countTrue((boolean[]) c)).reversed())
                    .collect(Collectors.toList()).get(0);
            validCombinations.remove(staticallyValidCombinationWithMostRules);
        } else {
            return new LinkedList<>();
        }

        List<Rule> staticallyValidatedRules = getRulesFromCombination(rulesToValidate, staticallyValidCombinationWithMostRules);

        // Add enum and examples that match the type
        staticallyValidatedRules.addAll(alwaysApplicableRules.stream()
                .filter(r -> {
                    ParameterName parameterName = r.getParameterNames().stream().findFirst().get();
                    if (r instanceof EnumRule) {
                        return isValueCompatibleWithParameter(parameterName, ((EnumRule) r).getEnumValue());
                    } else if (r instanceof ExampleRule) {
                        return isValueCompatibleWithParameter(parameterName, ((ExampleRule) r).getExampleValue());
                    }
                    return false;
                })
                .collect(Collectors.toList()));

        return staticallyValidatedRules;
    }

    public boolean[] precomputeNextStaticallyValidCombination() {

        // Move to the next combination, to test if it statically valid
        combinationHotEncoding = nextCombination(combinationHotEncoding);

        int index = validatePresenceRulesCombination();
        while (index >= 0) {
            combinationHotEncoding[index] = false;
            index = validatePresenceRulesCombination();
        }

        // Update removed parameters
        removedParameters.clear();
        for (int i = 0; i < presenceRules.size(); i++) {
            if (combinationHotEncoding[i] && presenceRules.get(i) instanceof RemoveRule) {
                RemoveRule rule = (RemoveRule) presenceRules.get(i);
                removedParameters.addAll(rule.getParameterNames());
            }
        }

        index = validateSetIpdRulesCombination();
        while (index >= 0) {
            combinationHotEncoding[index] = false;
            index = validateSetIpdRulesCombination();
        }

        index = validateConstraintRulesCombination();
        while (index >= 0) {
            combinationHotEncoding[index] = false;
            index = validateConstraintRulesCombination();
        }

        index = validateSingleRulesCombination();
        while (index >= 0) {
            combinationHotEncoding[index] = false;
            index = validateSingleRulesCombination();
        }

        return combinationHotEncoding;
    }

    /**
     * Computes the next combination.
     * @param combinationHotEncoding the start combination.
     * @return the next combination.
     * FIXME: can be optimized to O(n) rather than O(2n)
     */
    private boolean[] nextCombination(boolean[] combinationHotEncoding) {
        boolean[] nextOneHotCombination = Arrays.copyOf(combinationHotEncoding, combinationHotEncoding.length);
        for (int i = nextOneHotCombination.length - 1; i >= 0; i--) {
            if (nextOneHotCombination[i]) {
                nextOneHotCombination[i] = false;
                return nextOneHotCombination;
            }
            nextOneHotCombination[i] = true;
        }
        return nextOneHotCombination;
    }

    @NotNull
    private List<Rule> getRulesFromCombination(List<Rule> rules, boolean[] combinationHotEncoding) {

        List<Rule> combination = new LinkedList<>();

        for (int i = 0; i < combinationHotEncoding.length; i++) {
            if (combinationHotEncoding[i]) {
                combination.add(rules.get(i));
            }
        }

        return combination;
    }

    /**
     * Validates a combination of presence rules.
     * @return the index of the first rule that violates the compatibility, or -1 if no incompatibility is found.
     */
    private int validatePresenceRulesCombination() {

        int startIndex = 0;
        int endIndex = presenceRules.size();

        Set<ParameterName> required = new HashSet<>();
        Set<ParameterName> removed = new HashSet<>();

        // Iterate on rules
        for (int i = startIndex; i < endIndex; i++) {

            // Consider only rules enabled in combination
            if (combinationHotEncoding[i]) {

                Rule rule = rulesToValidate.get(i);

                if (rule instanceof RequiredRule) {
                    if (removed.containsAll(rule.getParameterNames())) {
                        return i;
                    } else {
                        required.addAll(rule.getParameterNames());
                    }
                } else if (rule instanceof RemoveRule) {
                    if (required.containsAll(rule.getParameterNames())) {
                        return i;
                    } else {
                        removed.addAll(rule.getParameterNames());
                    }
                }
            }
        }

        return -1;
    }

    private int validateSetIpdRulesCombination() {

        int startIndex = presenceRules.size();
        int endIndex = startIndex + setIpdRules.size();

        Set<ParameterName> alreadyUsedParameters = new HashSet<>();

        // Get the names of the required parameters in the operation, according to the specification
        Set<ParameterName> requiredParameters = operation.getAllRequestParameters().stream()
                .filter(e -> e.isRequired() && !(e.getParent() instanceof ArrayParameter))
                .map(Parameter::getName)
                .collect(Collectors.toSet());

        // Add or remove required parameters, based on the current combination
        for (int i = 0; i < presenceRules.size(); i++) {
            if (combinationHotEncoding[i] && rulesToValidate.get(i) instanceof RequiredRule) {
                if (((RequiredRule) rulesToValidate.get(i)).getRequired()) {
                    requiredParameters.addAll(rulesToValidate.get(i).getParameterNames());
                } else {
                    requiredParameters.removeAll(rulesToValidate.get(i).getParameterNames());
                }
            }
        }

        // Remove removed parameters from required
        requiredParameters.removeAll(removedParameters);

        // Iterate on rules
        for (int i = startIndex; i < endIndex; i++) {

            // Consider only rules enabled in combination
            if (combinationHotEncoding[i]) {

                Rule rule = rulesToValidate.get(i);

                int sizeOfRequiredParametersInRule = Sets.intersection(rule.getParameterNames(), requiredParameters).size();

                // Return if more than 1 parameter is required within an only one rule or a zero or one rule
                if ((rule instanceof OnlyOneRule || rule instanceof ZeroOrOneRule) && sizeOfRequiredParametersInRule > 1){
                    return i;
                }

                // Return if some parameters, but not all, are required within an all or none rule
                if (rule instanceof AllOrNoneRule && (requiredParameters.size() == 0 || requiredParameters.size() == rule.getParameterNames().size())) {
                    return i;
                }

                // Return if the involved parameters are already used in another rule or are removed
                if (rule.getParameterNames().stream().anyMatch(pn -> alreadyUsedParameters.contains(pn) || removedParameters.contains(pn))) {
                    return i;
                }
                alreadyUsedParameters.addAll(rule.getParameterNames());
            }
        }

        return -1;
    }

    private int validateConstraintRulesCombination() {

        int startIndex = presenceRules.size() + setIpdRules.size();
        int endIndex = startIndex + constraintRules.size();

        Map<ParameterName, ParameterType> typeMap = new HashMap<>();
        Map<ParameterName, ParameterTypeFormat> formatMap = new HashMap<>();
        Map<ParameterName, Double> minimumMap = new HashMap<>();
        Map<ParameterName, Double> maximumMap = new HashMap<>();
        Map<ParameterName, Boolean> exclusiveMinimumMap = new HashMap<>();
        Map<ParameterName, Boolean> exclusiveMaximumMap = new HashMap<>();

        // Iterate on rules
        for (int i = startIndex; i < endIndex; i++) {

            // Consider only rules enabled in combination
            if (combinationHotEncoding[i]) {

                Rule rule = rulesToValidate.get(i);

                // Remove rule if the involved parameter was removed by a presence rule
                if (rule.getParameterNames().stream().anyMatch(removedParameters::contains)) {
                    return i;
                }

                @SuppressWarnings("OptionalGetWithoutIsPresent")
                ParameterName parameterName = rule.getParameterNames().stream().findFirst().get();

                if (rule instanceof TypeRule) {

                    // Check if there is already a rule for the type of this parameter
                    if (typeMap.get(parameterName) == null) {
                        typeMap.put(parameterName, ((TypeRule) rule).getParameterType());
                    } else {
                        return i;
                    }
                } else if (rule instanceof FormatRule) {

                    // Check if there is already a rule for the format of this parameter
                    if (formatMap.get(parameterName) == null) {

                        // Get the current type, either from rules or from specification
                        ParameterType parameterType = typeMap.get(parameterName);
                        if (parameterType == null) {
                            parameterType = operation.searchRequestParametersByName(parameterName).get(0).getType();
                        }

                        // Check if the format is compatible with the type
                        if (parameterType == null || ((FormatRule) rule).getFormat().isCompatibleWithType(parameterType)) {
                            formatMap.put(parameterName, ((FormatRule) rule).getFormat());
                        } else {
                            return i;
                        }
                    } else {
                        return i;
                    }
                } else if (rule instanceof MinimumRule) {

                    // Only one minimum rule per parameter can exist
                    if (minimumMap.get(parameterName) != null) {
                        return i;
                    }

                    // Get the current type, either from rules or from specification
                    ParameterType parameterType = typeMap.get(parameterName);
                    if (parameterType == null) {
                        parameterType = operation.searchRequestParametersByName(parameterName).get(0).getType();
                    }

                    // For strings and arrays, the minimum can not be less than 0 and must be integer
                    if (parameterType == ParameterType.STRING || parameterType == ParameterType.ARRAY) {
                        if (((MinimumRule) rule).getMinimum() < 0  || (((MinimumRule) rule).getMinimum() % 1) != 0) {
                            return i;
                        }
                    }

                    // The rule seems to be valid, so I add it to the map
                    minimumMap.put(parameterName, ((MinimumRule) rule).getMinimum());

                } else if (rule instanceof MaximumRule) {

                    // Only one maximum rule per parameter can exist
                    if (maximumMap.get(parameterName) != null) {
                        return i;
                    }

                    // Get the current type, either from rules or from specification
                    ParameterType parameterType = typeMap.get(parameterName);
                    if (parameterType == null) {
                        parameterType = operation.searchRequestParametersByName(parameterName).get(0).getType();
                    }

                    // For strings and arrays, the maximum can not be less than 0 and must be integer
                    if (parameterType == ParameterType.STRING || parameterType == ParameterType.ARRAY) {
                        if (((MaximumRule) rule).getMaximum() < 0  || (((MaximumRule) rule).getMaximum() % 1) != 0) {
                            return i;
                        }
                    }

                    // Get the minimum value for the parameter
                    Double minimum = minimumMap.get(parameterName);
                    if (minimum == null) {
                        if (parameterType == ParameterType.INTEGER || parameterType == ParameterType.NUMBER) {
                            minimum = ((NumberParameter) (operation.searchRequestParametersByName(parameterName).get(0))).getMinimum();
                        }
                    }

                    // The maximum must be greater or equal to the minimum for the same parameter
                    if (minimum != null) {
                        if (minimum > ((MaximumRule) rule).getMaximum()) {
                            return i;
                        }
                    }

                    // The rule seems to be valid, so I add it to the map
                    maximumMap.put(parameterName, ((MaximumRule) rule).getMaximum());
                } else if (rule instanceof ExclusiveMinimumRule) {

                    // Only one exclusive minimum rule per parameter can exist
                    if (exclusiveMinimumMap.get(parameterName) != null) {
                        return i;
                    }

                    // Get the current type, either from rules or from specification
                    ParameterType parameterType = typeMap.get(parameterName);
                    if (parameterType == null) {
                        parameterType = operation.searchRequestParametersByName(parameterName).get(0).getType();
                    }

                    // The rule is applicable to number parameters
                    if (!(parameterType == ParameterType.NUMBER || parameterType == ParameterType.INTEGER)) {
                        return i;
                    }

                    // The rule is only applicable if a minimum value is defined
                    Double minimum = minimumMap.get(parameterName);
                    if (minimum == null) {
                        minimum = ((NumberParameter) (operation.searchRequestParametersByName(parameterName).get(0))).getMinimum();
                        if (minimum == null) {
                            return i;
                        }
                    }

                    // The rule seems to be valid, so I add it to the map
                    exclusiveMinimumMap.put(parameterName, ((ExclusiveMinimumRule) rule).isExclusiveMinimum());
                } else if (rule instanceof ExclusiveMaximumRule) {

                    // Only one exclusive maximum rule per parameter can exist
                    if (exclusiveMaximumMap.get(parameterName) != null) {
                        return i;
                    }

                    // Get the current type, either from rules or from specification
                    ParameterType parameterType = typeMap.get(parameterName);
                    if (parameterType == null) {
                        parameterType = operation.searchRequestParametersByName(parameterName).get(0).getType();
                    }

                    // The rule is applicable to number parameters
                    if (!(parameterType == ParameterType.NUMBER || parameterType == ParameterType.INTEGER)) {
                        return i;
                    }

                    // The rule is only applicable if a maximum value is defined
                    Double maximum = maximumMap.get(parameterName);
                    if (maximum == null) {
                        maximum = ((NumberParameter) (operation.searchRequestParametersByName(parameterName).get(0))).getMaximum();
                        if (maximum == null) {
                            return i;
                        }
                    }

                    // The rule seems to be valid, so I add it to the map
                    exclusiveMaximumMap.put(parameterName, ((ExclusiveMaximumRule) rule).isExclusiveMaximum());
                }
            }
        }

        return -1;
    }

    private int validateSingleRulesCombination() {

        int startIndex = presenceRules.size() + setIpdRules.size() + constraintRules.size();
        int endIndex = startIndex + singleRules.size();

        // To extend in case there are multiple types of single rules
        Set<ParameterName> defaultParameters = new HashSet<>();
        Set<ParameterName> collectionFormatParameters = new HashSet<>();

        // Iterate on rules
        for (int i = startIndex; i < endIndex; i++) {

            // Consider only rules enabled in combination
            if (combinationHotEncoding[i]) {

                Rule rule = rulesToValidate.get(i);

                // Remove rule if the involved parameter was removed by a presence rule
                if (rule.getParameterNames().stream().anyMatch(removedParameters::contains)) {
                    return i;
                }

                if (rule instanceof DefaultRule) {

                    // If there is another default rule operating on the same parameter, return
                    if (defaultParameters.containsAll(rule.getParameterNames())) {
                        return i;
                    }

                    // If the current value is not compatible with the type, return
                    //noinspection OptionalGetWithoutIsPresent
                    if (!isValueCompatibleWithParameter(rule.getParameterNames().stream().findFirst().get(), ((DefaultRule) rule).getDefaultValue())) {
                        return i;
                    }

                    defaultParameters.addAll(rule.getParameterNames());
                } else if (rule instanceof CollectionFormatRule) {

                    // If there is another collection format rule operating on the same parameter, return
                    if (collectionFormatParameters.containsAll(rule.getParameterNames())) {
                        return i;
                    }

                    collectionFormatParameters.addAll(rule.getParameterNames());
                }
            }
        }

        return -1;
    }

    /**
     * Checks if a value is compatible with the type of a parameter.
     * @param parameterName the name of the involved parameter.
     * @param value the value to check.
     * @return true if the value is compatible.
     */
    private boolean isValueCompatibleWithParameter(ParameterName parameterName, Object value) {

        // Get the type from the specification
        ParameterType type = operation.searchRequestParametersByName(parameterName).get(0).getType();

        // Search for rules that might change the type
        int startIndex = presenceRules.size() + setIpdRules.size();
        int endIndex = startIndex + constraintRules.size();
        for (int i = startIndex; i < endIndex; i++) {
            if (combinationHotEncoding[i] && rulesToValidate.get(i) instanceof TypeRule &&
                    rulesToValidate.get(i).getParameterNames().contains(parameterName)) {
                ParameterType ruleType = ((TypeRule) rulesToValidate.get(i)).getParameterType();
                if (ruleType != ParameterType.ARRAY) {
                    type = ruleType;
                }
                break;
            }
        }

        // Try to cast the value to the given type. If an exception is raised, return false.
        try {
            ObjectHelper.castToParameterValueType(value, type);
            return true;
        } catch (ClassCastException e) {
            return false;
        }
    }
 }
