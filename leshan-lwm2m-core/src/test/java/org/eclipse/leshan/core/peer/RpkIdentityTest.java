package org.eclipse.leshan.core.peer;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class RpkIdentityTest {
    @Test
    public void assertEqualsHashcode() {
        EqualsVerifier.forClass(RpkIdentity.class).verify();
    }
}