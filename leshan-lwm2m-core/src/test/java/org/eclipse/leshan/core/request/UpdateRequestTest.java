package org.eclipse.leshan.core.request;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.eclipse.leshan.core.link.Link;
import org.eclipse.leshan.core.request.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Map;

class UpdateRequestTest {

    private class ExtendedUpdateRequest extends UpdateRequest {
        ExtendedUpdateRequest(String registrationId, Long lifetime, String smsNumber, EnumSet<BindingMode> binding,
                              Link[] objectLinks, Map<String, String> additionalAttributes) throws InvalidRequestException {
            super(registrationId, lifetime, smsNumber, binding, objectLinks, additionalAttributes, null);
        }
        @Override
        public boolean canEqual(Object obj) {
            return (obj instanceof ExtendedUpdateRequest);
        }
    }

    @Test
    public void assertEqualsHashcode() {
        EqualsVerifier.forClass(UpdateRequest.class).withRedefinedSubclass(ExtendedUpdateRequest.class).withIgnoredFields("coapRequest").verify();
    }

}