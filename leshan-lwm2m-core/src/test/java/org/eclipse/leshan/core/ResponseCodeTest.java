package org.eclipse.leshan.core;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.jupiter.api.Test;

class ResponseCodeTest {
    @Test
    public void assertEqualsHashcode() {
        EqualsVerifier.forClass(ResponseCode.class).suppress(Warning.NONFINAL_FIELDS).verify();
    }
}