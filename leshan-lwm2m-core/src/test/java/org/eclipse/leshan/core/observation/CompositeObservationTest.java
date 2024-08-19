package org.eclipse.leshan.core.observation;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.request.ContentFormat;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

class CompositeObservationTest {

    public class ExtendedCompositeObservation extends CompositeObservation {

        public ExtendedCompositeObservation(ObservationIdentifier id, String registrationId, List<LwM2mPath> paths,
                                            ContentFormat requestContentFormat, ContentFormat responseContentFormat, Map<String, String> context,
                                            Map<String, String> protocolData) {
            super(id, registrationId, paths, requestContentFormat, responseContentFormat, context, protocolData);
        }

        @Override
        public boolean canEqual(Object obj) {
            return (obj instanceof ExtendedCompositeObservation);
        }
    }

    @Test
    public void assertEqualsHashcode() {
        EqualsVerifier.forClass(CompositeObservation.class).withRedefinedSuperclass().withRedefinedSubclass(ExtendedCompositeObservation.class).verify();
    }
}