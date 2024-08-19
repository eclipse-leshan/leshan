package org.eclipse.leshan.core.peer;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class SocketIdentityTest {
    @Test
    public void assertEqualsHashcode() {
        EqualsVerifier.forClass(SocketIdentity.class).verify();
    }
}