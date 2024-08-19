package org.eclipse.leshan.core.json;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.jupiter.api.Test;

class JsonRootObjectTest {
    @Test
    public void assertEqualsHashcode() {
        EqualsVerifier.forClass(JsonRootObject.class).suppress(Warning.NONFINAL_FIELDS).verify();
    }
}