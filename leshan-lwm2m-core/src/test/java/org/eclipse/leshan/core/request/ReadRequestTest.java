package org.eclipse.leshan.core.request;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class ReadRequestTest {

    private class ExtendedReadRequest extends ReadRequest {
        public ExtendedReadRequest(int objectId) {
            super(null, newPath(objectId), null);
        }
        @Override
        public boolean canEqual(Object obj) {
            return (obj instanceof ExtendedReadRequest);
        }
    }
    @Test
    public void assertEqualsHashcode() {
        EqualsVerifier.forClass(ReadRequest.class).withRedefinedSuperclass().withRedefinedSubclass(ExtendedReadRequest.class).withIgnoredFields("coapRequest").verify();
    }
}