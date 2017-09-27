/*******************************************************************************
 * Copyright (c) 2017 Sierra Wireless and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.client.californium;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class LeshanClientBuilderTest {

    private LeshanClientBuilder builder;
    private LeshanClient client;

    @Before
    public void start() {
        builder = new LeshanClientBuilder("client");
    }

    @Test
    public void create_client_without_any_parameter() {
        client = builder.build();

        assertNotNull(client.getSecuredAddress());
        assertNotNull(client.getUnsecuredAddress());
    }

    @Test
    public void create_client_disable_secured_endpoint() {
        builder.disableSecuredEndpoint();
        client = builder.build();

        assertNull(client.getSecuredAddress());
        assertNotNull(client.getUnsecuredAddress());
    }

    @Test
    public void create_server_disable_unsecured_endpoint() {
        builder.disableUnsecuredEndpoint();
        client = builder.build();

        assertNotNull(client.getSecuredAddress());
        assertNull(client.getUnsecuredAddress());
    }
}
