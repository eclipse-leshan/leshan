package org.eclipse.leshan.core.link.attributes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.leshan.core.LwM2m.Version;
import org.eclipse.leshan.core.link.lwm2m.attributes.AssignationLevel;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttribute;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributes;
import org.junit.Test;

public class AttributeTest {

    @Test
    public void should_pick_correct_model() {
        LwM2mAttribute<Version> verAttribute = LwM2mAttributes.create(LwM2mAttributes.OBJECT_VERSION,
                new Version("1.0"));
        assertEquals("ver", verAttribute.getName());
        assertEquals(new Version("1.0"), verAttribute.getValue());
        assertTrue(verAttribute.canBeAssignedTo(AssignationLevel.OBJECT));
        assertFalse(verAttribute.isWritable());
    }
}
