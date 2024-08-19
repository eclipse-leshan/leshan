package org.eclipse.leshan.servers.security;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class SecurityInfoTest {
    @Test
    public void assertEqualsHashcode() {
        EqualsVerifier.forClass(SecurityInfo.class).verify();
    }
}