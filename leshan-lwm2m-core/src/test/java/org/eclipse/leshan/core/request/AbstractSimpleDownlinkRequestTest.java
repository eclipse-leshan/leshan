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

import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

class AbstractSimpleDownlinkRequestTest {
    @Test
    public void assertEqualsHashcode() {
        EqualsVerifier.forClass(AbstractSimpleDownlinkRequest.class).withRedefinedSubclass(CreateRequest.class)
                .withIgnoredFields("coapRequest").verify();
        EqualsVerifier.forClass(AbstractSimpleDownlinkRequest.class).withRedefinedSubclass(BootstrapReadRequest.class)
                .withIgnoredFields("coapRequest").verify();
        EqualsVerifier.forClass(AbstractSimpleDownlinkRequest.class)
                .withRedefinedSubclass(CancelObservationRequest.class).withIgnoredFields("coapRequest").verify();
        EqualsVerifier.forClass(AbstractSimpleDownlinkRequest.class).withRedefinedSubclass(ExecuteRequest.class)
                .withIgnoredFields("coapRequest").verify();
        EqualsVerifier.forClass(AbstractSimpleDownlinkRequest.class).withRedefinedSubclass(ObserveRequest.class)
                .withIgnoredFields("coapRequest").verify();
        EqualsVerifier.forClass(AbstractSimpleDownlinkRequest.class).withRedefinedSubclass(ReadRequest.class)
                .withIgnoredFields("coapRequest").verify();
        EqualsVerifier.forClass(AbstractSimpleDownlinkRequest.class).withRedefinedSubclass(WriteAttributesRequest.class)
                .withIgnoredFields("coapRequest").verify();
        EqualsVerifier.forClass(AbstractSimpleDownlinkRequest.class).withRedefinedSubclass(WriteRequest.class)
                .withIgnoredFields("coapRequest").verify();
        EqualsVerifier.forClass(AbstractSimpleDownlinkRequest.class).withRedefinedSubclass(DiscoverRequest.class)
                .withIgnoredFields("coapRequest").verify();
        EqualsVerifier.forClass(AbstractSimpleDownlinkRequest.class).withRedefinedSubclass(BootstrapDeleteRequest.class)
                .withIgnoredFields("coapRequest").verify();
        EqualsVerifier.forClass(AbstractSimpleDownlinkRequest.class)
                .withRedefinedSubclass(BootstrapDiscoverRequest.class).withIgnoredFields("coapRequest").verify();
        EqualsVerifier.forClass(AbstractSimpleDownlinkRequest.class).withRedefinedSubclass(BootstrapWriteRequest.class)
                .withIgnoredFields("coapRequest").verify();
        EqualsVerifier.forClass(AbstractSimpleDownlinkRequest.class).withRedefinedSubclass(DeleteRequest.class)
                .withIgnoredFields("coapRequest").verify();
    }
}
