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

import java.util.Collection;

import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.request.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

class WriteRequestTest {
    private class ExtendedWriteRequest extends WriteRequest {
        public ExtendedWriteRequest(Mode mode, ContentFormat contentFormat, int objectId, int objectInstanceId,
                Collection<LwM2mResource> resources) throws InvalidRequestException {
            super(mode, contentFormat, newPath(objectId, objectInstanceId),
                    new LwM2mObjectInstance(objectId, resources), null);
        }

        @Override
        public boolean canEqual(Object obj) {
            return (obj instanceof ExtendedWriteRequest);
        }
    }

    @Test
    public void assertEqualsHashcode() {
        EqualsVerifier.forClass(WriteRequest.class).withRedefinedSuperclass()
                .withRedefinedSubclass(ExtendedWriteRequest.class).withIgnoredFields("coapRequest").verify();
    }

}
