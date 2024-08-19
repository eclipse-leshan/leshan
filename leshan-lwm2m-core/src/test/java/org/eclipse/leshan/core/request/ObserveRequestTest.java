package org.eclipse.leshan.core.request;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class ObserveRequestTest {

    private class ExtendedObserveRequest extends ObserveRequest {
        public ExtendedObserveRequest(int objectId) {
            super((String) null);
        }
        @Override
        public boolean canEqual(Object obj) {
            return (obj instanceof ExtendedObserveRequest);
        }
    }
    @Test
    public void assertEqualsHashcode() {
        EqualsVerifier.forClass(ObserveRequest.class).withRedefinedSuperclass().withRedefinedSubclass(ExtendedObserveRequest.class).withIgnoredFields("coapRequest").verify();
    }
}