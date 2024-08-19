/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 *
 * Contributors:
 *     Natalia Krzykała Orange Polska S.A. - initial implementation
 *******************************************************************************/
package org.eclipse.leshan.core.observation;

import java.util.List;
import java.util.Map;

import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.request.ContentFormat;
import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

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
        EqualsVerifier.forClass(CompositeObservation.class).withRedefinedSuperclass()
                .withRedefinedSubclass(ExtendedCompositeObservation.class).withIgnoredFields("protocolData").verify();
    }
}
