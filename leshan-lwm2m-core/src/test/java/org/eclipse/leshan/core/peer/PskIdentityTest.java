package org.eclipse.leshan.core.peer;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class PskIdentityTest {
    @Test
    public void assertEqualsHashcode() {
        EqualsVerifier.forClass(PskIdentity.class).verify();
    }
}