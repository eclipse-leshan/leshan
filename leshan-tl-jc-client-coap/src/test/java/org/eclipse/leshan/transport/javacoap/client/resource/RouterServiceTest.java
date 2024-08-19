package org.eclipse.leshan.transport.javacoap.client.resource;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class RouterServiceTest {
    @Test
    public void assertEqualsHashcode() {
        EqualsVerifier.forClass(RouterService.RequestMatcher.class).verify();
    }

}