package org.eclipse.leshan.core.link.lwm2m.attributes;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class LwM2mAttributeTest {

    @Test
    public void assertEqualsHashcode() {
        EqualsVerifier.forClass(LwM2mAttribute.class).verify();
    }

}