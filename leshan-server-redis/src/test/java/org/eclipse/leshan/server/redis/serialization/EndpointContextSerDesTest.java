/*******************************************************************************
 * Copyright (c) 2021 Bosch.IO GmbH and others.
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
 *     Bosch IO.GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.server.redis.serialization;

import static org.junit.Assert.assertEquals;

import java.net.InetSocketAddress;
import java.security.Principal;

import org.eclipse.californium.elements.Definition;
import org.eclipse.californium.elements.EndpointContext;
import org.eclipse.californium.elements.MapBasedEndpointContext;
import org.eclipse.californium.elements.MapBasedEndpointContext.Attributes;
import org.eclipse.californium.elements.auth.PreSharedKeyIdentity;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class EndpointContextSerDesTest {

    @Test
    public void endpoint_context_ser_des_then_equal() {
        Definition<InetSocketAddress> source = new Definition<>("source", InetSocketAddress.class,
                MapBasedEndpointContext.ATTRIBUTE_DEFINITIONS);
        Definition<Boolean> enable = new Definition<>("enable", Boolean.class,
                MapBasedEndpointContext.ATTRIBUTE_DEFINITIONS);
        InetSocketAddress address4 = new InetSocketAddress("127.0.0.1", 5683);
        InetSocketAddress address6 = new InetSocketAddress("::1", 5684);
        Attributes attributes = new Attributes().add(source, address4).add(enable, true);
        Principal principal = new PreSharedKeyIdentity("me");
        EndpointContext endpoint = new MapBasedEndpointContext(address6, principal, attributes);

        ObjectNode data = EndpointContextSerDes.serialize(endpoint);

        EndpointContext endpoint2 = EndpointContextSerDes.deserialize(data);
        assertEquals(endpoint.getPeerAddress(), endpoint2.getPeerAddress());
        assertEquals(endpoint.getPeerIdentity(), endpoint2.getPeerIdentity());
        assertEquals(endpoint.entries(), endpoint2.entries());
    }
}
