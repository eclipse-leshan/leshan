package org.eclipse.leshan.core.request;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class BootstrapReadRequestTest {

    private class ExtendedBootstrapReadRequest extends BootstrapReadRequest {
        ExtendedBootstrapReadRequest(int objectId) {
            super(null, newPath(objectId), null);
        }
        @Override
        public boolean canEqual(Object obj) {
            return (obj instanceof ExtendedBootstrapReadRequest);
        }
    }
    @Test
    public void assertEqualsHashcode() {
        EqualsVerifier.forClass(BootstrapReadRequest.class).withRedefinedSuperclass().withRedefinedSubclass(ExtendedBootstrapReadRequest.class).withIgnoredFields("coapRequest").verify();
    }
}