package org.eclipse.leshan.core.node;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class LwM2mObjectTest {

    @Test
    public void assertEqualsHashcode() {
        EqualsVerifier.forClass(LwM2mObject.class).verify();
    }
}