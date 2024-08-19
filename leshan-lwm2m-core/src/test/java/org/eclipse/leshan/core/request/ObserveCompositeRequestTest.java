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
package org.eclipse.leshan.core.request;

import java.util.List;

import org.eclipse.leshan.core.node.LwM2mPath;
import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

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
        EqualsVerifier.forClass(ObserveCompositeRequest.class)
                .withRedefinedSubclass(ExtendedObserveCompositeRequest.class)
                .withIgnoredFields("context", "coapRequest").verify();
    }
}
