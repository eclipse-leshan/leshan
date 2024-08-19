package org.eclipse.leshan.core.node;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class ObjectLinkTest {
    @Test
    public void assertEqualsHashcode() {
        EqualsVerifier.forClass(ObjectLink.class).verify();
    }
}