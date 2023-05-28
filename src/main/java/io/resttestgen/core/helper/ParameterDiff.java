package io.resttestgen.core.helper;

import com.google.common.collect.Sets;
import io.resttestgen.core.datatype.NormalizedParameterName;
import io.resttestgen.core.datatype.parameter.Parameter;
import io.resttestgen.core.datatype.parameter.ParameterUtils;
import io.resttestgen.core.openapi.Operation;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class ParameterDiff {

    /**
     * Computes diff of parameters between the expected output of the producer and the expected input of the consumer.
     * @param producer the operation producing some output parameters.
     * @param consumer the operation consuming some input parameters.
     * @return the list of not common parameters.
     */
    public static List<Parameter> staticDiff(Operation producer, Operation consumer) {
        List<Parameter> diffParameters = new LinkedList<>();

        Set<NormalizedParameterName> producerNormalizedParameterNames = new HashSet<>();
        producer.getOutputParametersSet().forEach(p -> producerNormalizedParameterNames.add(p.getNormalizedName()));
        Set<NormalizedParameterName> consumerNormalizedParameterNames = new HashSet<>();
        consumer.getAllRequestParameters().forEach(p -> consumerNormalizedParameterNames.add(p.getNormalizedName()));

        Set<NormalizedParameterName> diffNormalizedParameterNames =
                Sets.difference(producerNormalizedParameterNames, consumerNormalizedParameterNames);

        diffNormalizedParameterNames.forEach(n -> diffParameters.addAll(producer.searchReferenceRequestParametersByNormalizedName(n)));

        System.out.println("DIFF PARAMS:");
        System.out.println(producerNormalizedParameterNames);
        System.out.println(consumerNormalizedParameterNames);
        System.out.println(diffNormalizedParameterNames);

        return diffParameters;
    }

    /**
     * Computes diff of parameters between the actual output of the producer and the expected input of the consumer.
     * @param producer the operation producing some output parameters.
     * @param consumer the operation consuming some input parameters.
     * @return the list of not common parameters.
     */
    public static List<Parameter> dynamicDiff(Operation producer, Operation consumer) {

        if (producer.getResponseBody() == null) {
            return null;
        }

        List<Parameter> diffParameters = new LinkedList<>();

        Set<NormalizedParameterName> producerNormalizedParameterNames = new HashSet<>();
        ParameterUtils.getReferenceLeaves(producer.getResponseBody()).forEach(p -> producerNormalizedParameterNames.add(p.getNormalizedName()));
        Set<NormalizedParameterName> consumerNormalizedParameterNames = new HashSet<>();
        consumer.getAllRequestParameters().forEach(p -> consumerNormalizedParameterNames.add(p.getNormalizedName()));

        Set<NormalizedParameterName> diffNormalizedParameterNames =
                Sets.difference(producerNormalizedParameterNames, consumerNormalizedParameterNames);

        diffNormalizedParameterNames.forEach(n -> diffParameters.addAll(producer.searchReferenceRequestParametersByNormalizedName(n)));

        System.out.println("DIFF PARAMS:");
        System.out.println(producerNormalizedParameterNames);
        System.out.println(consumerNormalizedParameterNames);
        System.out.println(diffNormalizedParameterNames);

        return diffParameters;
    }
}
