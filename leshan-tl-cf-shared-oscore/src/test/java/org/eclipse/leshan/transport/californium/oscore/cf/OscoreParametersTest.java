package org.eclipse.leshan.transport.californium.oscore.cf;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class OscoreParametersTest {
    @Test
    public void assertEqualsHashcode() {
        EqualsVerifier.forClass(OscoreParameters.class).verify();
    }
}