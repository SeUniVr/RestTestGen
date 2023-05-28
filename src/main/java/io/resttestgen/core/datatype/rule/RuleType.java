package io.resttestgen.core.datatype.rule;

/**
 * Available rule types, in the order in which they are applied.
 */
public enum RuleType {

    REMOVE,
    REQUIRED,
    OR,
    ONLY_ONE,
    ALL_OR_NONE,
    ZERO_OR_ONE,
    TYPE,
    FORMAT,
    MAXIMUM,
    MINIMUM,
    EXCLUSIVE_MAXIMUM,
    EXCLUSIVE_MINIMUM,
    DEFAULT,
    COLLECTION_FORMAT,
    REQUIRES,
    ENUM,
    EXAMPLE,
    ARITHMETIC_RELATIONAL;

    public static RuleType parse(String ruleTypeString) {
        ruleTypeString = ruleTypeString.toLowerCase();
        ruleTypeString = ruleTypeString.replaceAll("-", "_");
        switch (ruleTypeString) {
            case "remove":
                return REMOVE;
            case "required":
                return REQUIRED;
            case "or":
                return OR;
            case "only_one":
                return ONLY_ONE;
            case "all_or_none":
                return ALL_OR_NONE;
            case "zero_or_one":
                return ZERO_OR_ONE;
            case "type":
                return TYPE;
            case "format":
                return FORMAT;
            case "maximum":
                return MAXIMUM;
            case "minimum":
                return MINIMUM;
            case "exclusive_maximum":
                return EXCLUSIVE_MAXIMUM;
            case "exclusive_minimum":
                return EXCLUSIVE_MINIMUM;
            case "default":
                return DEFAULT;
            case "collection_format":
                return COLLECTION_FORMAT;
            case "requires":
                return REQUIRES;
            case "enum":
                return ENUM;
            case "example":
                return EXAMPLE;
            case "arithmetic_relational":
                return ARITHMETIC_RELATIONAL;
        }
        return null;
    }

    public static Class getRuleClassFromRuleType(RuleType ruleType) {
        switch (ruleType) {
            case REMOVE:
                return RemoveRule.class;
            case REQUIRED:
                return RequiredRule.class;
            case OR:
                return OrRule.class;
            case ONLY_ONE:
                return OnlyOneRule.class;
            case ALL_OR_NONE:
                return AllOrNoneRule.class;
            case ZERO_OR_ONE:
                return ZeroOrOneRule.class;
            case TYPE:
                return TypeRule.class;
            case FORMAT:
                return FormatRule.class;
            case MAXIMUM:
                return MaximumRule.class;
            case MINIMUM:
                return MinimumRule.class;
            case EXCLUSIVE_MAXIMUM:
                return ExclusiveMaximumRule.class;
            case EXCLUSIVE_MINIMUM:
                return ExclusiveMinimumRule.class;
            case DEFAULT:
                return DefaultRule.class;
            case COLLECTION_FORMAT:
                return CollectionFormatRule.class;
            case REQUIRES:
                return RequiresRule.class;
            case ENUM:
                return EnumRule.class;
            case EXAMPLE:
                return ExampleRule.class;
            case ARITHMETIC_RELATIONAL:
                return ArithmeticRelationalRule.class;
        }
        return null;
    }
}