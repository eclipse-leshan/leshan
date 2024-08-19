package org.eclipse.leshan.senml;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.jupiter.api.Test;

class SenMLRecordTest {
    @Test
    public void assertEqualsHashcode() {
        EqualsVerifier.forClass(SenMLRecord.class).suppress(Warning.NONFINAL_FIELDS).verify();
    }
}