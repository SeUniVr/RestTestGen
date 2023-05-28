package io.resttestgen.core.datatype.parameter.visitor;

import io.resttestgen.core.datatype.parameter.Parameter;
import io.resttestgen.core.datatype.parameter.combined.CombinedSchemaParameter;
import io.resttestgen.core.datatype.parameter.leaves.BooleanParameter;
import io.resttestgen.core.datatype.parameter.leaves.LeafParameter;
import io.resttestgen.core.datatype.parameter.leaves.NumberParameter;
import io.resttestgen.core.datatype.parameter.leaves.StringParameter;
import io.resttestgen.core.datatype.parameter.structured.ObjectParameter;
import io.resttestgen.core.datatype.parameter.structured.ArrayParameter;
import io.resttestgen.core.datatype.parameter.structured.StructuredParameter;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * This visitor implementation helps to collect all leaves or reference leaves from a parameter
 */
public class LeavesVisitor implements Visitor<Collection<LeafParameter>> {
    private final boolean getReferenceLeaves;

    public LeavesVisitor(boolean getReferenceLeaves) {
        this.getReferenceLeaves = getReferenceLeaves;
    }

    public LeavesVisitor() {
        this(false);
    }

    @Override
    public Collection<LeafParameter> visit(Parameter element) {
        return null;
    }

    @Override
    public Collection<LeafParameter> visit(LeafParameter element) {
        List<LeafParameter> leaves = new LinkedList<>();
        leaves.add(element);
        return leaves;
    }

    @Override
    public Collection<LeafParameter> visit(StructuredParameter element) {
        return null;
    }

    @Override
    public Collection<LeafParameter> visit(ArrayParameter element) {
        List<LeafParameter> leaves = new LinkedList<>();
        if (getReferenceLeaves) {
            leaves.addAll(element.getReferenceElement().accept(this));
            return leaves;
        }

        element.getElements().forEach(e -> leaves.addAll(e.accept(this)));
        return leaves;
    }

    @Override
    public Collection<LeafParameter> visit(ObjectParameter element) {
        List<LeafParameter> leaves = new LinkedList<>();
        element.getProperties().forEach(e ->
                leaves.addAll(e.accept(this)));
        return leaves;
    }

    @Override
    public Collection<LeafParameter> visit(StringParameter element) {
        return visit((LeafParameter) element);
    }

    @Override
    public Collection<LeafParameter> visit(NumberParameter element) {
        return visit((LeafParameter) element);
    }

    @Override
    public Collection<LeafParameter> visit(BooleanParameter element) {
        return visit((LeafParameter) element);
    }

    @Override
    public Collection<LeafParameter> visit(CombinedSchemaParameter element) {
        List<LeafParameter> leaves = new LinkedList<>();

        if (element.getOutputParameterSchema() != null) {
            leaves.addAll(
                    element.getOutputParameterSchema().accept(this));
            return leaves;
        }

        element.getParametersSchemas()
                .forEach(e -> leaves.addAll(e.accept(this)));

        return leaves;
    }
}
