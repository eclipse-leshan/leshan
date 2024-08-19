package org.eclipse.leshan.core.observation;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.request.ContentFormat;
import org.junit.jupiter.api.Test;

import java.util.Map;

class SingleObservationTest {

    private class ExtendedSingleObservation extends SingleObservation {
        ExtendedSingleObservation(ObservationIdentifier id, String registrationId, LwM2mPath path,
                                  ContentFormat contentFormat, Map<String, String> context, Map<String, String> protocolData) {
            super(id, registrationId, path, contentFormat, context, protocolData);
        }
        @Override
        public boolean canEqual(Object obj){
            return (obj instanceof ExtendedSingleObservation);
        }
    }
    @Test
    public void assertEqualsHashcode() {
        EqualsVerifier.forClass(SingleObservation.class).withRedefinedSuperclass().withRedefinedSubclass(ExtendedSingleObservation.class).verify();
    }
}