package io.resttestgen.core.datatype.parameter.visitor;

import io.resttestgen.core.datatype.parameter.Parameter;
import io.resttestgen.core.datatype.parameter.combined.CombinedSchemaParameter;
import io.resttestgen.core.datatype.parameter.leaves.BooleanParameter;
import io.resttestgen.core.datatype.parameter.leaves.NumberParameter;
import io.resttestgen.core.datatype.parameter.leaves.LeafParameter;
import io.resttestgen.core.datatype.parameter.leaves.StringParameter;
import io.resttestgen.core.datatype.parameter.structured.ArrayParameter;
import io.resttestgen.core.datatype.parameter.structured.ObjectParameter;
import io.resttestgen.core.datatype.parameter.structured.StructuredParameter;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * This visitor implementation helps to collect all arrays from a parameter
 */
public class ArraysVisitor implements Visitor<Collection<ArrayParameter>> {
    @Override
    public Collection<ArrayParameter> visit(Parameter element) {
        return List.of();
    }

    @Override
    public Collection<ArrayParameter> visit(LeafParameter element) {
        return List.of();
    }

    @Override
    public Collection<ArrayParameter> visit(StructuredParameter element) {
        return List.of();
    }

    @Override
    public Collection<ArrayParameter> visit(ArrayParameter element) {
        List<ArrayParameter> arrays = new LinkedList<>();
        arrays.add(element);
        element.getElements().forEach(e -> arrays.addAll(e.accept(this)));
        return arrays;
    }

    @Override
    public Collection<ArrayParameter> visit(ObjectParameter element) {
        List<ArrayParameter> arrays = new LinkedList<>();
        element.getProperties().forEach(e -> arrays.addAll(e.accept(this)));
        return arrays;
    }

    @Override
    public Collection<ArrayParameter> visit(StringParameter element) {
        return List.of();
    }

    @Override
    public Collection<ArrayParameter> visit(NumberParameter element) {
        return List.of();
    }

    @Override
    public Collection<ArrayParameter> visit(BooleanParameter element) {
        return List.of();
    }

    @Override
    public Collection<ArrayParameter> visit(CombinedSchemaParameter element) {
        List<ArrayParameter> arrays = new LinkedList<>();
        if (element.getOutputParameterSchema() != null) {
            arrays.addAll(element.getOutputParameterSchema().accept(this));
            return arrays;
        }

        element.getParametersSchemas().forEach(e -> arrays.addAll(e.accept(this)));
        return arrays;
    }
}
