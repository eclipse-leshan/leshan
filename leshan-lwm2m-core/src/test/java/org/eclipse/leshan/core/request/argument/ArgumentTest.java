package org.eclipse.leshan.core.request.argument;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class ArgumentTest {

    @Test
    public void assertEqualsHashcode() {
        EqualsVerifier.forClass(Argument.class).verify();
    }
}