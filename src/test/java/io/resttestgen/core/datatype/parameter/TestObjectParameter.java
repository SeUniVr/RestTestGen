package io.resttestgen.core.datatype.parameter;

import io.resttestgen.core.datatype.ParameterName;
import io.resttestgen.core.datatype.parameter.leaves.NumberParameter;
import io.resttestgen.core.datatype.parameter.leaves.StringParameter;
import io.resttestgen.core.datatype.parameter.structured.ObjectParameter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestObjectParameter {

    @Test
    public void testAddChild() {
        ObjectParameter objectParameter = new ObjectParameter();
        StringParameter str1 = new StringParameter();
        str1.setName(new ParameterName("name"));

        objectParameter.addChild(str1);
        assertEquals(1, objectParameter.getChildren().size());
        assertEquals(str1, objectParameter.getChildren().stream().findFirst().get());

        StringParameter str2 = new StringParameter();
        str2.setName(new ParameterName("name"));
        objectParameter.addChild(str2);

        assertEquals(1, objectParameter.getChildren().size());
        assertEquals(str1.merge(str2), objectParameter.getChildren().stream().findFirst().get());

        NumberParameter num1 = new NumberParameter();
        num1.setName(new ParameterName("other"));
        objectParameter.addChild(num1);
        assertEquals(2, objectParameter.getChildren().size());
        assertEquals(1, objectParameter.getChildren().stream().filter(p -> p.equals(num1)).count());
    }
}
