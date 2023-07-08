package io.resttestgen.core.helper;

import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.NormalizedParameterName;
import io.resttestgen.core.datatype.parameter.Parameter;
import io.resttestgen.core.datatype.parameter.leaves.LeafParameter;
import io.resttestgen.core.openapi.OpenApi;
import io.resttestgen.core.openapi.Operation;
import weka.clusterers.EM;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.util.*;
import java.util.stream.Collectors;

public class CrudInformationExtractor {

    OpenApi openAPI = Environment.getInstance().getOpenAPI();
    Map<Integer, Operation> operationIndexes = new HashMap<>();
    List<NormalizedParameterName> parameterNames;
    Instances dataset;

    public CrudInformationExtractor() {

        parameterNames = collectAllNormalizedParameterNames();
        ArrayList<Attribute> attributes = new ArrayList<>();

        for (NormalizedParameterName normalizedParameterName : parameterNames) {
            ArrayList<String> labels = new ArrayList<>();
            labels.add("false");
            labels.add("true");
            Attribute nominal = new Attribute(normalizedParameterName.toString(), labels);
            attributes.add(nominal);
        }

        dataset = new Instances("operations", attributes, parameterNames.size());
    }

    public void extract() {

        // Add data to dataset
        for (Operation operation : openAPI.getOperations()) {
            operationIndexes.put(dataset.size(), operation);
            dataset.add(getOperationInstance(operation));
        }

        EM em = new EM();
        try {
            em.buildClusterer(dataset);

            for (int i = 0; i < dataset.size(); i++) {
                int clusterIndex = em.clusterInstance(dataset.get(i));
                operationIndexes.get(i).setInferredCrudResourceType("Cluster" + clusterIndex);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        for (Operation operation : openAPI.getOperations()) {
            Set<NormalizedParameterName> firstLevelParameterNames =
                    new HashSet<>(getOperationFirstLevelParameterNames(operation));
            Set<NormalizedParameterName> allParameterNames = new HashSet<>(getOperationParameterNames(operation));

            NormalizedParameterName resourceIdentifierName = null;

            // Check if the first level parameters have a field with ID
            for (NormalizedParameterName name : firstLevelParameterNames) {
                if (name.toString().toLowerCase().endsWith("id")) {
                    resourceIdentifierName = name;
                    break;
                }
            }

            // Check if first level parameters have a field with username
            if (resourceIdentifierName == null) {
                for (NormalizedParameterName name : firstLevelParameterNames) {
                    if (name.toString().toLowerCase().endsWith("username") ||
                            name.toString().toLowerCase().endsWith("usernam")) {
                        resourceIdentifierName = name;
                        break;
                    }
                }
            }

            // Check if all parameters have a field with ID
            if (resourceIdentifierName == null) {
                for (NormalizedParameterName name : allParameterNames) {
                    if (name.toString().toLowerCase().endsWith("id")) {
                        resourceIdentifierName = name;
                        break;
                    }
                }
            }

            // Check if all parameters have a field with username
            if (resourceIdentifierName == null) {
                // Check if first level parameters have a field with username
                for (NormalizedParameterName name : allParameterNames) {
                    if (name.toString().toLowerCase().endsWith("username") ||
                            name.toString().toLowerCase().endsWith("usernam")) {
                        resourceIdentifierName = name;
                        break;
                    }
                }
            }

            // Apply tag to parameter with identified name

            if (resourceIdentifierName != null) {
                Collection<Parameter> foundParameters = operation.getAllRequestParameters();
                foundParameters.addAll(operation.getOutputParametersSet());

                for (Parameter element : foundParameters) {
                    if (element instanceof LeafParameter && element.getNormalizedName().equals(resourceIdentifierName)) {
                        ((LeafParameter) element).setInferredResourceIdentifier(true);
                    }
                }
            }
        }

        /*Set<String> inferredResourceTypes = CRUDManager.collectInferredResourceTypes(openAPI);
        for (String inferredResourceType : inferredResourceTypes) {
            CRUDGroup crudGroup = new CRUDGroup(openAPI, inferredResourceType, true);
            Set<NormalizedParameterName> firstLevelParameterNames = new HashSet<>();
            Set<NormalizedParameterName> allParameterNames = new HashSet<>();
            for (Operation operation : crudGroup.getOperations()) {
                firstLevelParameterNames.addAll(getOperationFirstLevelParameterNames(operation));
                allParameterNames.addAll(getOperationParameterNames(operation));
            }

            NormalizedParameterName resourceIdentifierName = null;

            // Check if the first level parameters have a field with ID
            for (NormalizedParameterName name : firstLevelParameterNames) {
                if (name.toString().toLowerCase().endsWith("id")) {
                    resourceIdentifierName = name;
                    break;
                }
            }

            // Check if first level parameters have a field with username
            if (resourceIdentifierName == null) {
                for (NormalizedParameterName name : firstLevelParameterNames) {
                    if (name.toString().toLowerCase().endsWith("username") ||
                            name.toString().toLowerCase().endsWith("usernam")) {
                        resourceIdentifierName = name;
                        break;
                    }
                }
            }

            // Check if all parameters have a field with ID
            if (resourceIdentifierName == null) {
                for (NormalizedParameterName name : allParameterNames) {
                    if (name.toString().toLowerCase().endsWith("id")) {
                        resourceIdentifierName = name;
                        break;
                    }
                }
            }

            // Check if all parameters have a field with username
            if (resourceIdentifierName == null) {
                // Check if first level parameters have a field with username
                for (NormalizedParameterName name : allParameterNames) {
                    if (name.toString().toLowerCase().endsWith("username") ||
                            name.toString().toLowerCase().endsWith("usernam")) {
                        resourceIdentifierName = name;
                        break;
                    }
                }
            }

            // Tag as resource identifiers the parameters with matching name
            if (resourceIdentifierName != null) {
                for (Operation operation : crudGroup.getOperations()) {
                    Set<ParameterElement> foundParameters = operation.getInputParametersSet();
                    foundParameters.addAll(operation.getOutputParametersSet());

                    for (Parameter parameter : foundParameters) {
                        if (element instanceof LeafParameter && parameter.getNormalizedName().equals(resourceIdentifierName)) {
                            ((LeafParameter) parameter).setInferredResourceIdentifier(true);
                        }
                    }
                }
            }
        }*/
    }



    private List<NormalizedParameterName> collectAllNormalizedParameterNames() {
        List<NormalizedParameterName> normalizedParameterNames = new ArrayList<>();
        Environment.getInstance().getOpenAPI().getOperations()
                .forEach(o -> normalizedParameterNames.addAll(getOperationParameterNames(o)));
        return normalizedParameterNames.stream().distinct().collect(Collectors.toList());
    }

    private List<NormalizedParameterName> getOperationParameterNames(Operation operation) {

        List<NormalizedParameterName> normalizedParameterNames = new ArrayList<>();

        Set<Parameter> elements = new HashSet<>(operation.getAllRequestParameters());
        elements.addAll(operation.getOutputParametersSet());
        for (Parameter element : elements) {
            if (element instanceof LeafParameter) {
                normalizedParameterNames.add(element.getNormalizedName());
            }
        }

        return normalizedParameterNames.stream().distinct().collect(Collectors.toList());
    }

    private List<NormalizedParameterName> getOperationFirstLevelParameterNames(Operation operation) {

        List<NormalizedParameterName> normalizedParameterNames = new ArrayList<>();

        Set<Parameter> elements = new HashSet<>(operation.getFirstLevelRequestParameters());
        elements.addAll(operation.getFirstLevelOutputParameters());
        for (Parameter element : elements) {
            if (element instanceof LeafParameter) {
                normalizedParameterNames.add(element.getNormalizedName());
            }
        }

        return normalizedParameterNames.stream().distinct().collect(Collectors.toList());
    }

    private Instance getOperationInstance(Operation operation) {

        List<NormalizedParameterName> parameterNamesInOperation = getOperationParameterNames(operation);
        double[] values = new double[parameterNames.size()];

        for (int i = 0; i < parameterNames.size(); i++) {
            if (parameterNamesInOperation.contains(parameterNames.get(i))) {
                values[i] = dataset.attribute(i).indexOfValue("true");
            } else {
                values[i] = dataset.attribute(i).indexOfValue("false");
            }
        }
        return new DenseInstance(1., values);
    }
}
