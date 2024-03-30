package io.resttestgen.core.datatype.parameter;

import io.resttestgen.core.datatype.parameter.combined.CombinedSchemaParameter;
import io.resttestgen.core.datatype.parameter.leaves.*;
import io.resttestgen.core.datatype.parameter.structured.ArrayParameter;
import io.resttestgen.core.datatype.parameter.structured.ObjectParameter;
import io.resttestgen.core.datatype.parameter.visitor.ArraysVisitor;
import io.resttestgen.core.datatype.parameter.visitor.CombinedSchemasVisitor;
import io.resttestgen.core.datatype.parameter.visitor.LeavesVisitor;
import io.resttestgen.core.datatype.parameter.visitor.ObjectsVisitor;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Collection of static helper methods for the parameter classes
 */
public class ParameterUtils {

    static public boolean isObject(@NotNull Parameter element) {
        return element instanceof ObjectParameter;
    }

    static public boolean isArray(@NotNull Parameter element) {
        return element instanceof ArrayParameter;
    }

    static public boolean isArrayOfLeaves(@NotNull Parameter element) {
        return element instanceof ArrayParameter && ((ArrayParameter) element).getReferenceElement() instanceof LeafParameter;
    }

    static public boolean isArrayOfArrays(@NotNull Parameter element) {
        return element instanceof ArrayParameter && ((ArrayParameter) element).getReferenceElement() instanceof ArrayParameter;
    }

    static public boolean isArrayOfObjects(@NotNull Parameter element) {
        return element instanceof ArrayParameter && ((ArrayParameter) element).getReferenceElement() instanceof ObjectParameter;
    }

    static public boolean isLeaf(@NotNull Parameter element) {
        return element instanceof LeafParameter;
    }

    static public boolean isNumber(@NotNull Parameter element) {
        return element instanceof NumberParameter;
    }

    static public boolean isString(@NotNull Parameter element) {
        return element instanceof StringParameter;
    }

    static public boolean isBoolean(@NotNull Parameter element) {
        return element instanceof BooleanParameter;
    }

    static public boolean isNull(@NotNull Parameter element) {
        return element instanceof NullParameter;
    }

    /**
     * Check if a parameter element is a reference element of an array parameter.
     * @return true if parameter is reference element.
     */
    static public boolean isReferenceElement(@NotNull Parameter element) {
        return element.getParent() != null
                && element.getParent() instanceof ArrayParameter
                && ((ArrayParameter) element.getParent()).getReferenceElement() == element;
    }

    /**
     * Check if a parameter is an element of an array parameter.
     * @return true if parameter is an element of an array parameter.
     */
    static public boolean isArrayElement(@NotNull Parameter element) {
        return element.getParent() != null
                && element.getParent() instanceof ArrayParameter
                && ((ArrayParameter) element.getParent()).getElements().contains(element);
    }

    /**
     * Check if a parameter is a property of an object.
     * @return true if parameter is property of an object.
     */
    static public boolean isObjectProperty(@NotNull Parameter element) {
        return element.getParent() != null
                && element.getParent() instanceof ObjectParameter
                && ((ObjectParameter) element.getParent()).getProperties().contains(element);
    }

    /**
     * Return a collection containing the arrays in the parameter element and underlying elements.
     * @return the collection of arrays in the parameter.
     */
    static public Collection<ArrayParameter> getArrays(@NotNull Parameter element) {
        return element.accept(new ArraysVisitor());
    }

    /**
     * Return a collection containing the objects in the parameter element and underlying elements.
     * @return the collection of objects in the parameter.
     */
    static public Collection<ObjectParameter> getObjects(@NotNull Parameter element) {
        return element.accept(new ObjectsVisitor());
    }

    /**
     * Return a collection containing the objects in the parameter element and underlying elements. In case of arrays,
     * only the objects in reference element are returned.
     * @return the collection of objects in the parameter.
     */
    static public Collection<ObjectParameter> getReferenceObjects(@NotNull Parameter element) {
        return element.accept(new ObjectsVisitor(true));
    }

    /**
     * Returns a collection containing the leaves in the parameter element and underlying elements.
     * @return the collection of leaves in the parameter
     */
    static public Collection<LeafParameter> getLeaves(@NotNull Parameter element) {
        return element.accept(new LeavesVisitor());
    }

    /**
     * Get the leaves of the parameters. In case of arrays, the returned leaves are part of the reference element of the
     * array.
     * @return the reference leaves in the parameter.
     */
    static public Collection<LeafParameter> getReferenceLeaves(@NotNull Parameter element) {
        return element.accept(new LeavesVisitor(true));
    }


    /**
     * Returns a collection containing the combined schemas in the parameter element and underlying elements.
     * @return the collection of combined schemas in the parameter
     */
    static public Collection<CombinedSchemaParameter> getCombinedSchemas(@NotNull Parameter element) {
        return element.accept(new CombinedSchemasVisitor());
    }
}
