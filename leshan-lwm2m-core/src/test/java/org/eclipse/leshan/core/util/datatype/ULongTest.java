package org.eclipse.leshan.core.util.datatype;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class ULongTest {
    @Test
    public void assertEqualsHashcode() {
        EqualsVerifier.forClass(ULong.class).verify();
    }
}