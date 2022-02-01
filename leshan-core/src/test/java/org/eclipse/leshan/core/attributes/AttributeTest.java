package org.eclipse.leshan.core.attributes;

import static org.junit.Assert.*;

import org.eclipse.leshan.core.link.lwm2m.attributes.AssignationLevel;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttribute;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributes;
import org.junit.Test;

public class AttributeTest {

    @Test
    public void should_pick_correct_model() {
        LwM2mAttribute<String> verAttribute = new LwM2mAttribute<String>(LwM2mAttributes.OBJECT_VERSION, "1.0");
        assertEquals("ver", verAttribute.getName());
        assertEquals("1.0", verAttribute.getValue());
        assertTrue(verAttribute.canBeAssignedTo(AssignationLevel.OBJECT));
        assertFalse(verAttribute.isWritable());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test(expected = IllegalArgumentException.class)
    public void should_throw_on_invalid_value_type() {
        new LwM2mAttribute(LwM2mAttributes.OBJECT_VERSION, 123);
    }
}
