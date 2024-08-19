package org.eclipse.leshan.transport.javacoap.client.observe;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class HashMapObserversStoreTest {
    @Test
    public void assertEqualsHashcode() {
        EqualsVerifier.forClass(HashMapObserversStore.ObserverKey.class).verify();
    }
}