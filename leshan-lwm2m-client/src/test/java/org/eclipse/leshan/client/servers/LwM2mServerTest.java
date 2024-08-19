package org.eclipse.leshan.client.servers;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.leshan.core.node.LwM2mPath;
import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

class LwM2mServerTest {
    @Test
    public void assertEqualsHashcode() {
        EqualsVerifier.forClass(LwM2mServer.class).withIgnoredFields("uri").verify();
    }

}