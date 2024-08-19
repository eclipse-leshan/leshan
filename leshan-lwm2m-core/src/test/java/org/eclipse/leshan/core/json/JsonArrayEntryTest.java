package org.eclipse.leshan.core.json;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.jupiter.api.Test;

class JsonArrayEntryTest {
    @Test
    public void assertEqualsHashcode() {
        EqualsVerifier.forClass(JsonArrayEntry.class).suppress(Warning.NONFINAL_FIELDS).verify();
    }
}