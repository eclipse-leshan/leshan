package org.eclipse.leshan.core.request;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.eclipse.leshan.core.request.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;

class DeregisterRequestTest {

    private class ExtendedDeregisterRequest extends DeregisterRequest {
        public ExtendedDeregisterRequest(String registrationId) throws InvalidRequestException {
            super(registrationId, null);
        }
        @Override
        public boolean canEqual(Object obj) {
            return (obj instanceof ExtendedDeregisterRequest);
        }
    }
    @Test
    public void assertEqualsHashcode() {
        EqualsVerifier.forClass(DeregisterRequest.class).withRedefinedSubclass(ExtendedDeregisterRequest.class).withIgnoredFields("coapRequest").verify();
    }
}