package io.resttestgen.core.datatype.parameter;

import io.resttestgen.core.datatype.parameter.attributes.ParameterLocation;
import io.resttestgen.core.datatype.parameter.combined.CombinedSchemaParameter;
import io.resttestgen.core.datatype.parameter.leaves.*;
import io.resttestgen.core.datatype.parameter.structured.ArrayParameter;
import io.resttestgen.core.datatype.parameter.structured.ObjectParameter;
import io.resttestgen.core.datatype.parameter.structured.StructuredParameter;
import io.resttestgen.core.datatype.parameter.visitor.ArraysVisitor;
import io.resttestgen.core.datatype.parameter.visitor.CombinedSchemasVisitor;
import io.resttestgen.core.datatype.parameter.visitor.LeavesVisitor;
import io.resttestgen.core.datatype.parameter.visitor.ObjectsVisitor;
import io.resttestgen.core.openapi.Operation;

import java.util.Collection;
import java.util.Map;

/**
 * Collection of static helper methods for the parameter classes
 */
public class ParameterUtils {
    static public boolean isObject(Parameter element) {
        return element instanceof ObjectParameter;
    }

    static public boolean isArray(Parameter element) {
        return element instanceof ArrayParameter;
    }

    static public boolean isArrayOfLeaves(Parameter element) {
        return element instanceof ArrayParameter && ((ArrayParameter) element).getReferenceElement() instanceof LeafParameter;
    }

    static public boolean isArrayOfArrays(Parameter element) {
        return element instanceof ArrayParameter && ((ArrayParameter) element).getReferenceElement() instanceof ArrayParameter;
    }

    static public boolean isArrayOfObjects(Parameter element) {
        return element instanceof ArrayParameter && ((ArrayParameter) element).getReferenceElement() instanceof ObjectParameter;
    }

    static public boolean isLeaf(Parameter element) {
        return element instanceof LeafParameter;
    }

    static public boolean isNumber(Parameter element) {
        return element instanceof NumberParameter;
    }

    static public boolean isString(Parameter element) {
        return element instanceof StringParameter;
    }

    static public boolean isBoolean(Parameter element) {
        return element instanceof BooleanParameter;
    }

    static public boolean isNull(Parameter element) {
        return element instanceof NullParameter;
    }

    /**
     * Check if a parameter element is a reference element of an array parameter.
     * @return true if parameter is reference element.
     */
    static public boolean isReferenceElement(Parameter element) {
        return element.getParent() != null
                && element.getParent() instanceof ArrayParameter
                && ((ArrayParameter) element.getParent()).getReferenceElement() == element;
    }

    /**
     * Check if a parameter is an element of an array parameter.
     * @return true if parameter is an element of an array parameter.
     */
    static public boolean isArrayElement(Parameter element) {
        return element.getParent() != null
                && element.getParent() instanceof ArrayParameter
                && ((ArrayParameter) element.getParent()).getElements().contains(element);
    }

    /**
     * Check if a parameter is a property of an object.
     * @return true if parameter is property of an object.
     */
    static public boolean isObjectProperty(Parameter element) {
        return element.getParent() != null
                && element.getParent() instanceof ObjectParameter
                && ((ObjectParameter) element.getParent()).getProperties().contains(element);
    }

    /**
     * Return a collection containing the arrays in the parameter element and underlying elements.
     * @return the collection of arrays in the parameter.
     */
    static public Collection<ArrayParameter> getArrays(Parameter element) {
        return element.accept(new ArraysVisitor());
    }

    /**
     * Return a collection containing the objects in the parameter element and underlying elements.
     * @return the collection of objects in the parameter.
     */
    static public Collection<ObjectParameter> getObjects(Parameter element) {
        return element.accept(new ObjectsVisitor());
    }

    /**
     * Return a collection containing the objects in the parameter element and underlying elements. In case of arrays,
     * only the objects in reference element are returned.
     * @return the collection of objects in the parameter.
     */
    static public Collection<ObjectParameter> getReferenceObjects(Parameter element) {
        return element.accept(new ObjectsVisitor(true));
    }

    /**
     * Returns a collection containing the leaves in the parameter element and underlying elements.
     * @return the collection of leaves in the parameter
     */
    static public Collection<LeafParameter> getLeaves(Parameter element) {
        return element.accept(new LeavesVisitor());
    }

    /**
     * Get the leaves of the parameters. In case of arrays, the returned leaves are part of the reference element of the
     * array.
     * @return the reference leaves in the parameter.
     */
    static public Collection<LeafParameter> getReferenceLeaves(Parameter element) {
        return element.accept(new LeavesVisitor(true));
    }


    /**
     * Returns a collection containing the combined schemas in the parameter element and underlying elements.
     * @return the collection of combined schemas in the parameter
     */
    static public Collection<CombinedSchemaParameter> getCombinedSchemas(Parameter element) {
        return element.accept(new CombinedSchemasVisitor());
    }
}
