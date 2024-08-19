package org.eclipse.leshan.core.request;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

class ReadCompositeRequestTest {
    private class ExtendedReadCompositeRequest extends ReadCompositeRequest {
        ExtendedReadCompositeRequest(ContentFormat requestContentFormat, ContentFormat responseContentFormat,
                                     String... paths) {
            super(null, requestContentFormat, responseContentFormat, null);
        }
        @Override
        public boolean canEqual(Object obj) {
            return (obj instanceof ExtendedReadCompositeRequest);
        }
    }
    @Test
    public void assertEqualsHashcode() {
        EqualsVerifier.forClass(ReadCompositeRequest.class).withRedefinedSubclass(ExtendedReadCompositeRequest.class).withIgnoredFields("coapRequest").verify();
    }
}