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

import java.util.Map;

import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.request.ContentFormat;
import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

class SingleObservationTest {

    private class ExtendedSingleObservation extends SingleObservation {
        ExtendedSingleObservation(ObservationIdentifier id, String registrationId, LwM2mPath path,
                ContentFormat contentFormat, Map<String, String> context, Map<String, String> protocolData) {
            super(id, registrationId, path, contentFormat, context, protocolData);
        }

        @Override
        public boolean canEqual(Object obj) {
            return (obj instanceof ExtendedSingleObservation);
        }
    }

    @Test
    public void assertEqualsHashcode() {
        EqualsVerifier.forClass(SingleObservation.class).withRedefinedSuperclass()
                .withRedefinedSubclass(ExtendedSingleObservation.class).withIgnoredFields("protocolData").verify();
    }
}
