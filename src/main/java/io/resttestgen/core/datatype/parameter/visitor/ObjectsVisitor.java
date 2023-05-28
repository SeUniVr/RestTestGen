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
 * This visitor implementation helps to collect all objects from a parameter
 */
public class ObjectsVisitor implements Visitor<Collection<ObjectParameter>> {
    private final boolean getReferenceObjects;

    public ObjectsVisitor(boolean getReferenceObjects) {
        this.getReferenceObjects = getReferenceObjects;
    }

    public ObjectsVisitor() {
        this(false);
    }

    @Override
    public Collection<ObjectParameter> visit(Parameter element) {
        return List.of();
    }

    @Override
    public Collection<ObjectParameter> visit(LeafParameter element) {
        return List.of();
    }

    @Override
    public Collection<ObjectParameter> visit(StructuredParameter element) {
        return List.of();
    }

    @Override
    public Collection<ObjectParameter> visit(ArrayParameter element) {
        List<ObjectParameter> objects = new LinkedList<>();
        if (getReferenceObjects) {
            objects.addAll(element.getReferenceElement().accept(this));
            return objects;
        }

        element.getElements().forEach(e -> objects.addAll(e.accept(this)));
        return objects;
    }

    @Override
    public Collection<ObjectParameter> visit(ObjectParameter element) {
        List<ObjectParameter> objects = new LinkedList<>();
        objects.add(element);
        element.getProperties().forEach(e -> objects.addAll(e.accept(this)));
        return objects;
    }

    @Override
    public Collection<ObjectParameter> visit(StringParameter element) {
        return List.of();
    }

    @Override
    public Collection<ObjectParameter> visit(NumberParameter element) {
        return List.of();
    }

    @Override
    public Collection<ObjectParameter> visit(BooleanParameter element) {
        return List.of();
    }

    @Override
    public Collection<ObjectParameter> visit(CombinedSchemaParameter element) {
        return List.of();
    }
}
