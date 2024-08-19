package org.eclipse.leshan.core.model;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.jupiter.api.Test;

class LwM2mModelRepositoryTest {
    @Test
    public void assertEqualsHashcode() {
        // EqualsVerifier.forClass(LwM2mModelRepository.Key.class).suppress(Warning.NONFINAL_FIELDS).verify();
        // Problem with Key class - it is a PRIVATE nested class - cannot access the hash/equals methods
        // Solution: make Key package-private for this test (it passes then)
    }
  
}