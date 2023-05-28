package io.resttestgen.core.helper.jsonserializer;

import com.google.gson.*;
import io.resttestgen.core.datatype.ParameterName;
import io.resttestgen.core.datatype.parameter.attributes.ParameterType;
import io.resttestgen.core.datatype.parameter.attributes.ParameterTypeFormat;
import io.resttestgen.core.datatype.rule.*;

import java.lang.reflect.Type;
import java.util.HashSet;

/**
 * Deserializes rule to the corresponding subclass.
 */
public class RuleDeserializer implements JsonDeserializer<Rule> {

    Gson gson = new Gson();

    @Override
    public Rule deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject ruleObject = json.getAsJsonObject();
        RuleType ruleType = RuleType.parse(ruleObject.get("ruleType").getAsString());

        HashSet<ParameterName> parameterNames = new HashSet<>();
        ParameterName parameterName = null;

        JsonArray parameterNamesArray = ruleObject.get("parameterNames").getAsJsonArray();
        for (int i = 0; i < parameterNamesArray.size(); i++) {
            ParameterName tempParameterName = gson.fromJson(parameterNamesArray.get(i), ParameterName.class);
            parameterNames.add(parameterName);
            if (i == 0) {
                parameterName = tempParameterName;
            }
        }

        switch (ruleType) {
            case REMOVE:
                return new RemoveRule(parameterName, gson.fromJson(ruleObject.get("remove"), Boolean.class));
            case REQUIRED:
                return new RequiredRule(parameterName, gson.fromJson(ruleObject.get("required"), Boolean.class));
            case OR:
                return new OrRule(parameterNames);
            case ONLY_ONE:
                return new OnlyOneRule(parameterNames);
            case ALL_OR_NONE:
                return new AllOrNoneRule(parameterNames);
            case ZERO_OR_ONE:
                return new ZeroOrOneRule(parameterNames);
            case TYPE:
                return new TypeRule(parameterName, gson.fromJson(ruleObject.get("parameterType"), ParameterType.class));
            case FORMAT:
                return new FormatRule(parameterName, gson.fromJson(ruleObject.get("format"), ParameterTypeFormat.class));
            case MAXIMUM:
                return new MaximumRule(parameterName, gson.fromJson(ruleObject.get("maximum"), Double.class));
            case MINIMUM:
                return new MinimumRule(parameterName, gson.fromJson(ruleObject.get("minimum"), Double.class));
            case EXCLUSIVE_MAXIMUM:
                return new ExclusiveMaximumRule(parameterName, gson.fromJson(ruleObject.get("exclusiveMaximum"), Boolean.class));
            case EXCLUSIVE_MINIMUM:
                return new ExclusiveMinimumRule(parameterName, gson.fromJson(ruleObject.get("exclusiveMinimum"), Boolean.class));
            case DEFAULT:
                return new DefaultRule(parameterName, gson.fromJson(ruleObject.get("defaultValue"), String.class));
            case COLLECTION_FORMAT:
                return new CollectionFormatRule(parameterName, gson.fromJson(ruleObject.get("collectionFormat"), String.class));
            case REQUIRES:
                return new RequiresRule(gson.fromJson(ruleObject.get("condition"), String.class), gson.fromJson(ruleObject.get("statement"), String.class));
            case ENUM:
                return new EnumRule(parameterName, gson.fromJson(ruleObject.get("enumValue"), String.class));
            case EXAMPLE:
                return new ExampleRule(parameterName, gson.fromJson(ruleObject.get("exampleValue"), String.class));
            case ARITHMETIC_RELATIONAL:
                return new ArithmeticRelationalRule(parameterNames, "null");
        }
        return null;
    }
}
