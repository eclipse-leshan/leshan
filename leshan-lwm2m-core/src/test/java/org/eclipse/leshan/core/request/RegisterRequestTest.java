package org.eclipse.leshan.core.request;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.eclipse.leshan.core.link.Link;
import org.eclipse.leshan.core.request.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Map;

class RegisterRequestTest {

    private class ExtendedRegisterRequest extends RegisterRequest {
        ExtendedRegisterRequest(String endpointName, Long lifetime, String lwVersion, EnumSet<BindingMode> bindingMode,
                                Boolean queueMode, String smsNumber, Link[] objectLinks, Map<String, String> additionalAttributes)
                throws InvalidRequestException {
            super(endpointName, lifetime, lwVersion, bindingMode, queueMode, smsNumber, objectLinks, additionalAttributes,
                    null);
        }
        @Override
        public boolean canEqual(Object o){
            return (o instanceof ExtendedRegisterRequest);
        }
    }
    @Test
    public void assertEqualsHashcode() {
        EqualsVerifier.forClass(RegisterRequest.class).withRedefinedSubclass(ExtendedRegisterRequest.class).withIgnoredFields("coapRequest").verify();
    }
}