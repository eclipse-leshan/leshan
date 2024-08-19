package org.eclipse.leshan.core.peer;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class X509IdentityTest {
    @Test
    public void assertEqualsHashcode() {
        EqualsVerifier.forClass(X509Identity.class).verify();
    }
}