package org.eclipse.leshan.core.request;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.junit.jupiter.api.Test;

class AbstractSimpleDownlinkRequestTest {
    private class ExtendedAbstractSimpleDownlinkRequest extends AbstractSimpleDownlinkRequest {
        public ExtendedAbstractSimpleDownlinkRequest(LwM2mPath path, Object coapRequest) {
            super(path, coapRequest);
        }
        @Override
        public boolean canEqual(Object obj) {
            return (obj instanceof ExtendedAbstractSimpleDownlinkRequest);
        }

        @Override
        public void accept(DownlinkRequestVisitor visitor) {

        }
    }

    @Test
    public void assertEqualsHashcode() {
        EqualsVerifier.forClass(AbstractSimpleDownlinkRequest.class).withRedefinedSubclass(ExtendedAbstractSimpleDownlinkRequest.class).withIgnoredFields("coapRequest").verify();
    }
}