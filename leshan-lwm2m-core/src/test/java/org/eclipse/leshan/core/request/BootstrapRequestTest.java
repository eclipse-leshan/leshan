package org.eclipse.leshan.core.request;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.eclipse.leshan.core.request.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;

class BootstrapRequestTest {

    private class ExtendedBootstrapRequest extends BootstrapRequest {
        public ExtendedBootstrapRequest(String endpointName) throws InvalidRequestException {
            super(endpointName, null, null);
        }
        @Override
        public boolean canEqual(Object obj) {
            return (obj instanceof ExtendedBootstrapRequest);
        }
    }
    @Test
    public void assertEqualsHashcode() {
        EqualsVerifier.forClass(BootstrapRequest.class).withRedefinedSubclass(ExtendedBootstrapRequest.class).withIgnoredFields("coapRequest").verify();
    }
}