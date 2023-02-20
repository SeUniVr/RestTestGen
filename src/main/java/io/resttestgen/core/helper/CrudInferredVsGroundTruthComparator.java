package io.resttestgen.core.helper;

import io.resttestgen.core.Configuration;
import io.resttestgen.core.Environment;
import io.resttestgen.core.datatype.parameter.ParameterElement;
import io.resttestgen.core.datatype.parameter.ParameterLeaf;
import io.resttestgen.core.openapi.Operation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class CrudInferredVsGroundTruthComparator {

    public static void compare() {

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("operation,realCrudSemantics,inferredCrudSemantics,realResourceType,inferredResourceType,realInputResourceIdentifier,inferredInputResourceIdentifier,realOutputResourceIdentifier,inferredOutputResourceIdentifier\n");

        for (Operation operation : Environment.getInstance().getOpenAPI().getOperations()) {
            stringBuilder.append(operation.toString()).append(",");
            stringBuilder.append(operation.getCrudSemantics()).append(",");
            stringBuilder.append(operation.getInferredCrudSemantics()).append(",");
            stringBuilder.append(operation.getCrudResourceType()).append(",");
            stringBuilder.append(operation.getInferredCrudResourceType()).append(",");

            for (ParameterElement element : operation.getAllRequestParameters()) {
                if (element instanceof ParameterLeaf) {
                    if (((ParameterLeaf) element).isResourceIdentifier()) {
                        stringBuilder.append(element.getNormalizedName());
                        break;
                    }
                }
            }

            stringBuilder.append(",");

            for (ParameterElement element : operation.getAllRequestParameters()) {
                if (element instanceof ParameterLeaf) {
                    if (((ParameterLeaf) element).isInferredResourceIdentifier()) {
                        stringBuilder.append(element.getNormalizedName());
                        break;
                    }
                }
            }

            stringBuilder.append(",");

            for (ParameterElement element : operation.getOutputParametersSet()) {
                if (element instanceof ParameterLeaf) {
                    if (((ParameterLeaf) element).isResourceIdentifier()) {
                        stringBuilder.append(element.getNormalizedName());
                        break;
                    }
                }
            }

            stringBuilder.append(",");

            for (ParameterElement element : operation.getOutputParametersSet()) {
                if (element instanceof ParameterLeaf) {
                    if (((ParameterLeaf) element).isInferredResourceIdentifier()) {
                        stringBuilder.append(element.getNormalizedName());
                        break;
                    }
                }
            }

            stringBuilder.append("\n");
        }

        System.out.println(stringBuilder);
        write(stringBuilder.toString());
    }

    public static void write(String toWrite) {

        try {

            Configuration configuration = Environment.getInstance().getConfiguration();

            String path = configuration.getOutputPath() + configuration.getTestingSessionName() + "/";

            File file = new File(path);

            file.mkdirs();

            String filename = path + "CRUDinfo.csv";

            BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
            writer.write(toWrite);

            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
