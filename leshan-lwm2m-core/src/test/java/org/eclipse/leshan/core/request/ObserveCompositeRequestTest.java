package org.eclipse.leshan.core.request;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

class ObserveCompositeRequestTest {

    private class ExtendedObserveCompositeRequest extends ObserveCompositeRequest {
        ExtendedObserveCompositeRequest(ContentFormat requestContentFormat, ContentFormat responseContentFormat,
                                        List<LwM2mPath> paths) {
            super(requestContentFormat, responseContentFormat, paths, null);
        }
        @Override
        public boolean canEqual(Object obj) {
            return (obj instanceof ExtendedObserveCompositeRequest);
        }
    }
    @Test
    public void assertEqualsHashcode() {
        EqualsVerifier.forClass(ObserveCompositeRequest.class).withRedefinedSubclass(ExtendedObserveCompositeRequest.class).withIgnoredFields("context", "coapRequest").verify();
    }
}