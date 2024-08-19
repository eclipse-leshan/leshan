package org.eclipse.leshan.bsserver;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.jupiter.api.Test;

class InMemoryBootstrapConfigStoreTest {
    @Test
    public void assertEqualsHashcode() {
        EqualsVerifier.forClass(InMemoryBootstrapConfigStore.PskByServer.class).suppress(Warning.NONFINAL_FIELDS).verify();
    }
}