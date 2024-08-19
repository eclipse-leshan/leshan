package org.eclipse.leshan.core.peer;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class IpPeerTest {
    @Test
    public void assertEqualsHashcode() {
        EqualsVerifier.forClass(IpPeer.class).withIgnoredFields("virtualHost").verify();
    }
}