package org.eclipse.leshan.core.oscore;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.eclipse.leshan.core.observation.ObservationIdentifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AeadAlgorithmTest {
    @Test
    public void assertEqualsHashcode() {
        EqualsVerifier.forClass(AeadAlgorithm.class).verify();
    }
}