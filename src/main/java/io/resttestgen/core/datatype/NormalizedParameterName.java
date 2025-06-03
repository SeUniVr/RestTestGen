package io.resttestgen.core.datatype;

import io.resttestgen.core.datatype.parameter.Parameter;
import io.resttestgen.core.datatype.parameter.attributes.ParameterLocation;
import opennlp.tools.stemmer.PorterStemmer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class NormalizedParameterName {

    private static Set<String> qualifiableNames;
    private final static PorterStemmer stemmer = new PorterStemmer();
    private final static Set<String> qualifiedNames = new HashSet<>();

    private String normalizedParameterName;

    private static final Logger logger = LogManager.getLogger(NormalizedParameterName.class);

    public NormalizedParameterName(ParameterName parameterName) {
        setNormalizedParameterName(parameterName);
    }

    public NormalizedParameterName(String name) {
        this.normalizedParameterName = computeNormalizedName(name);
    }

    public void setNormalizedParameterName(ParameterName parameterName) {
        this.normalizedParameterName = computeNormalizedName(parameterName.toString());
    }

    public static void setQualifiableNames(Collection<String> names) {
        qualifiableNames = new HashSet<>();

        logger.debug("Names to qualify: {}", names);

        for (String name : names) {
            qualifiableNames.add(stemmer.stem(name));
        }

    }

    public static String computeNormalizedName(String name) {

        if (name.length() <= 2) {
            return name;
        }

        // First, split using camelCase, '_' , '-'
        String[] tokens = name.split("(?=[A-Z])|_|-");

        // Force splitting of last token if ends with a qualifiable string
        String lastToken = tokens[tokens.length - 1];
        for (String qualifiableName : qualifiableNames) {
            if (lastToken.toLowerCase().endsWith(qualifiableName) && lastToken.length() > qualifiableName.length()) {
                tokens[tokens.length - 1] = lastToken.substring(0, lastToken.length() - qualifiableName.length());
                tokens = Arrays.copyOf(tokens, tokens.length + 1);
                tokens[tokens.length - 1] = qualifiableName;
                break;
            }
        }

        ArrayList<String> stemmed = new ArrayList<>();

        // Stem each token
        for (String token : tokens) {
            if (!token.isEmpty()) {
                stemmed.add(stemmer.stem(token));
            }
        }

        // Create the normalized name using camelCase
        StringBuilder normalizedName = new StringBuilder();
        for (String s : stemmed) {
            try {
                normalizedName.append(s.substring(0, 1).toUpperCase()).append(s.substring(1));
            } catch (StringIndexOutOfBoundsException e) {
                normalizedName.append(s.substring(0, 1).toUpperCase());
            }
        }

        return normalizedName.toString();
    }

    public static NormalizedParameterName computeParameterNormalizedName(Parameter parameter) {
        return new NormalizedParameterName(qualifyName(parameter));
    }

    public static String qualifyName(Parameter parameter) {

        String name = parameter.getName().toString();

        // Check whether the name is qualifiable. Skip header parameters.
        if (qualifiableNames != null && qualifiableNames.contains(stemmer.stem(name)) &&
                !parameter.getLocation().equals(ParameterLocation.HEADER)) {

            // First, check out if it has a parent
            Parameter parent = parameter.getParent();

            // Check for the parent node name
            if (parent != null && !parent.getNormalizedName().toString().isEmpty()) {
                String qualified;

                // Concat the two names in camel case
                try {
                    qualified = parent.getNormalizedName() + name.substring(0,1).toUpperCase() + name.substring(1);
                } catch (IndexOutOfBoundsException e) {
                    qualified = parent.getNormalizedName() + name.substring(0,1).toUpperCase();
                }

                qualifiedNames.add(qualified);
                return qualified;
            }

            // Check for additional info in the parameter's endpoint
            String endpoint = parameter.getOperation().getEndpoint();
            String[] tokens = endpoint.split("/");
            ArrayList<String> keywords = new ArrayList<>();

            // Remove path parameters from the path
            for (String token : tokens) {
                if (!token.contains("{") && !token.isEmpty()) {
                    keywords.add(token);
                }
            }

            // Check for matches with already qualified names
            for (String keyword : keywords) {
                String candidateName = computeNormalizedName(stemmer.stem(keyword) + "_" + name);

                if (qualifiedNames.contains(candidateName)) {
                    return candidateName;
                }
            }

            // If no candidate has a match, qualify using the last URL part of the path
            String candidateName = !keywords.isEmpty() ?
                    computeNormalizedName(stemmer.stem(keywords.get(keywords.size() - 1)) + "_" + name) :
                    name;
            qualifiedNames.add(candidateName);
            return candidateName;
        }

        // No match is found, name cannot be qualified
        return name;
    }

    /**
     * ATTENTION: WORSE PERFORMANCE THAN qualifyName(): it misses dependencies when, for example, a schema exists
     * for entire properties, but only some are used in other places. The idea could still be usable within the odg
     * creation.
     * Alternative approach to name qualification. If a parameter has an associated schema name return it as qualified
     * name.
     * @param parameter the parameter for which a qualified name should be computed.
     * @return the qualified name.
     */
    public static String alternativeQualifyName(Parameter parameter) {

        String name = parameter.getName().toString();

        // Check that name is qualifiable. Skip header parameters.
        if (qualifiableNames.contains(stemmer.stem(name)) && !parameter.getLocation().equals(ParameterLocation.HEADER)) {

            // First, check for schema names
            if (parameter.getSchemaName() != null) {
                return parameter.getSchemaName();
            }

            // Alternatively, check out if it has a parent
            Parameter parent = parameter.getParent();

            // Check for parent node name
            if (parent != null && !parent.getNormalizedName().toString().isEmpty()) {
                String qualified;

                // Concat the two names in camel case
                try {
                    qualified = parent.getNormalizedName() + name.substring(0,1).toUpperCase() + name.substring(1);
                } catch (IndexOutOfBoundsException e) {
                    qualified = parent.getNormalizedName() + name.substring(0,1).toUpperCase();
                }

                qualifiedNames.add(qualified);
                return qualified;
            }

            // Check for additional info in parameter endpoint
            String endpoint = parameter.getOperation().getEndpoint();
            String[] tokens = endpoint.split("/");
            ArrayList<String> keywords = new ArrayList<>();

            // Remove path parameters from the path
            for (String token : tokens) {
                if (!token.contains("{") && !token.isEmpty()) {
                    keywords.add(token);
                }
            }

            // Check for matches with already qualified names
            for (String keyword : keywords) {
                String candidateName = computeNormalizedName(stemmer.stem(keyword) + "_" + name);

                if (qualifiedNames.contains(candidateName)) {
                    return candidateName;
                }
            }

            // If no candidate has a match, qualify using last URL part of the path
            String candidateName = !keywords.isEmpty() ?
                    computeNormalizedName(stemmer.stem(keywords.get(keywords.size() - 1)) + "_" + name) :
                    name;
            qualifiedNames.add(candidateName);
            return candidateName;
        }

        // No match is found, name cannot be qualified
        return name;
    }

    @Override
    public String toString() {
        return normalizedParameterName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NormalizedParameterName that = (NormalizedParameterName) o;
        return Objects.equals(normalizedParameterName, that.normalizedParameterName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(normalizedParameterName);
    }
}
