package org.eclipse.leshan.core.link.attributes;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class ValuelessAttributeTest {
    @Test
    public void assertEqualsHashcode() {
        EqualsVerifier.forClass(ValuelessAttribute.class).verify();
    }
}