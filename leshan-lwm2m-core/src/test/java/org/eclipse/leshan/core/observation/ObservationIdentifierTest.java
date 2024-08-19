package org.eclipse.leshan.core.observation;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class ObservationIdentifierTest {
    @Test
    public void assertEqualsHashcode() {
        EqualsVerifier.forClass(ObservationIdentifier.class).verify();
    }
}