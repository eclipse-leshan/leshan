package org.eclipse.leshan.core.node;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class TimestampedLwM2mNodeTest {
    @Test
    public void assertEqualsHashcode() {
        EqualsVerifier.forClass(TimestampedLwM2mNode.class).verify();
    }
}