package org.eclipse.leshan.core.request;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.eclipse.leshan.core.node.*;
import org.eclipse.leshan.core.request.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;

import java.util.Collection;

class WriteRequestTest {
    private class ExtendedWriteRequest extends WriteRequest {
        public ExtendedWriteRequest(Mode mode, ContentFormat contentFormat, int objectId, int objectInstanceId,
                            Collection<LwM2mResource> resources) throws InvalidRequestException {
            super(mode, contentFormat, newPath(objectId, objectInstanceId), new LwM2mObjectInstance(objectId, resources),
                    null);
        }
        @Override
        public boolean canEqual(Object obj) {
            return (obj instanceof ExtendedWriteRequest);
        }
    }

    @Test
    public void assertEqualsHashcode() {
        EqualsVerifier.forClass(WriteRequest.class).withRedefinedSuperclass().withRedefinedSubclass(ExtendedWriteRequest.class).withIgnoredFields("coapRequest").verify();
    }

}