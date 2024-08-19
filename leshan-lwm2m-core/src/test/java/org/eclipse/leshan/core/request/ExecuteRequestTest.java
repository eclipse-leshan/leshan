package org.eclipse.leshan.core.request;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.eclipse.leshan.core.request.argument.Arguments;
import org.eclipse.leshan.core.request.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;

class ExecuteRequestTest {

    private class ExtendedExecuteRequest extends ExecuteRequest {
        ExtendedExecuteRequest(String path) throws InvalidRequestException {
            super(path, (Arguments) null);
        }
        @Override
        public boolean canEqual(Object obj) {
            return (obj instanceof ExtendedExecuteRequest);
        }
    }
    @Test
    public void assertEqualsHashcode() {
        EqualsVerifier.forClass(ExecuteRequest.class).withRedefinedSuperclass().withRedefinedSubclass(ExtendedExecuteRequest.class).withIgnoredFields("coapRequest").verify();
    }

}