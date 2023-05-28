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
import java.util.List;

public class CombinedSchemasVisitor implements Visitor<Collection<CombinedSchemaParameter>> {
    @Override
    public Collection<CombinedSchemaParameter> visit(Parameter element) {
        return List.of();
    }

    @Override
    public Collection<CombinedSchemaParameter> visit(LeafParameter element) {
        return List.of();
    }

    @Override
    public Collection<CombinedSchemaParameter> visit(StructuredParameter element) {
        return List.of();
    }

    @Override
    public Collection<CombinedSchemaParameter> visit(ArrayParameter element) {
        return List.of();
    }

    @Override
    public Collection<CombinedSchemaParameter> visit(ObjectParameter element) {
        return List.of();
    }

    @Override
    public Collection<CombinedSchemaParameter> visit(StringParameter element) {
        return List.of();
    }

    @Override
    public Collection<CombinedSchemaParameter> visit(NumberParameter element) {
        return List.of();
    }

    @Override
    public Collection<CombinedSchemaParameter> visit(BooleanParameter element) {
        return List.of();
    }

    @Override
    public Collection<CombinedSchemaParameter> visit(CombinedSchemaParameter element) {
        return List.of();
    }
}
