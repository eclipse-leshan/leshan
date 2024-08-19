package org.eclipse.leshan.core.node;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class LwM2mSingleResourceTest {
    @Test
    public void assertEqualsHashcode() {
        EqualsVerifier.forClass(LwM2mSingleResource.class).verify();
    }
}