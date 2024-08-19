package org.eclipse.leshan.core.peer;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class OscoreIdentityTest {
    @Test
    public void assertEqualsHashcode() {
        EqualsVerifier.forClass(OscoreIdentity.class).verify();
    }
}