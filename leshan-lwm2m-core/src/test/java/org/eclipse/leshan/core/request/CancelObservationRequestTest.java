package org.eclipse.leshan.core.request;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.eclipse.leshan.core.observation.SingleObservation;
import org.junit.jupiter.api.Test;


class CancelObservationRequestTest {

    public class ExtendedCancelObservationRequest extends CancelObservationRequest {

        public ExtendedCancelObservationRequest(SingleObservation observation) {
            super(observation);
        }

        @Override
        public boolean canEqual(Object obj) {
            return (obj instanceof ExtendedCancelObservationRequest);
        }
    }

    @Test
    public void assertEqualsHashcode() {
        EqualsVerifier.forClass(CancelObservationRequest.class).withRedefinedSuperclass().withRedefinedSubclass(ExtendedCancelObservationRequest.class).withIgnoredFields("coapRequest").verify();
    }
}