package org.eclipse.leshan.core.endpoint;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class ProtocolTest {
    @Test
    public void assertEqualsHashcode() {
        EqualsVerifier.forClass(Protocol.class).withIgnoredFields("uriScheme").verify();
    }
}