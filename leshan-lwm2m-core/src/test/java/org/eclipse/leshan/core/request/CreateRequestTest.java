package org.eclipse.leshan.core.request;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.request.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;

class CreateRequestTest {

    public class ExtendedCreateRequest extends CreateRequest {
        public ExtendedCreateRequest(ContentFormat contentFormat, int objectId, LwM2mResource... resources)
                throws InvalidRequestException {
            super(contentFormat, objectId, resources);
        }

        @Override
        public boolean canEqual(Object obj) {
            return (obj instanceof ExtendedCreateRequest);
        }
    }

    @Test
    public void assertEqualsHashcode() {
        EqualsVerifier.forClass(CreateRequest.class).withRedefinedSubclass(ExtendedCreateRequest.class).withRedefinedSuperclass().withIgnoredFields("coapRequest").verify();
    }
}