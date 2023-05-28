package io.resttestgen.core.datatype.parameter.visitor;

import io.resttestgen.core.datatype.parameter.Parameter;
import io.resttestgen.core.datatype.parameter.combined.CombinedSchemaParameter;
import io.resttestgen.core.datatype.parameter.leaves.BooleanParameter;
import io.resttestgen.core.datatype.parameter.leaves.LeafParameter;
import io.resttestgen.core.datatype.parameter.leaves.NumberParameter;
import io.resttestgen.core.datatype.parameter.leaves.StringParameter;
import io.resttestgen.core.datatype.parameter.structured.ArrayParameter;
import io.resttestgen.core.datatype.parameter.structured.ObjectParameter;
import io.resttestgen.core.datatype.parameter.structured.StructuredParameter;

/*
 * This interface is the main interface for the visitor pattern in the project.
 * It is used to provide new functionalities to the parameters classes without
 * keeping on modifying them as the project grows.
 */
public interface Visitor<T> {

    default T visit(Parameter element) { return null; };
    default T visit(LeafParameter element) { return null; };
    default T visit(StructuredParameter element) { return null; };
    default T visit(CombinedSchemaParameter element) { return null; };
    T visit(ArrayParameter element);
    T visit(ObjectParameter element);
    T visit(StringParameter element);
    T visit(NumberParameter element);
    T visit(BooleanParameter element);
}
