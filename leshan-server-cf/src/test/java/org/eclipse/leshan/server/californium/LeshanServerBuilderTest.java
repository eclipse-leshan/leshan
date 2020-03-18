/*******************************************************************************
 * Copyright (c) 2017 Sierra Wireless and others.
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
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.server.californium;

import static org.junit.Assert.*;

import org.eclipse.leshan.server.security.InMemorySecurityStore;
import org.junit.Before;
import org.junit.Test;

public class LeshanServerBuilderTest {

    private LeshanServerBuilder builder;
    private LeshanServer server;

    @Before
    public void start() {
        builder = new LeshanServerBuilder();
    }

    @Test
    public void create_server_without_any_parameter() {
        server = builder.build();

        assertNull(server.getSecuredAddress());
        assertNotNull(server.getUnsecuredAddress());
        assertNull(server.getSecurityStore());
    }

    @Test
    public void create_server_with_securityStore() {
        builder.setSecurityStore(new InMemorySecurityStore());
        server = builder.build();

        assertNotNull(server.getSecuredAddress());
        assertNotNull(server.getUnsecuredAddress());
        assertNotNull(server.getSecurityStore());
    }

    @Test
    public void create_server_with_securityStore_and_disable_secured_endpoint() {
        builder.setSecurityStore(new InMemorySecurityStore());
        builder.disableSecuredEndpoint();
        server = builder.build();

        assertNull(server.getSecuredAddress());
        assertNotNull(server.getUnsecuredAddress());
    }

    @Test
    public void create_server_with_securityStore_and_disable_unsecured_endpoint() {
        builder.setSecurityStore(new InMemorySecurityStore());
        builder.disableUnsecuredEndpoint();
        server = builder.build();

        assertNotNull(server.getSecuredAddress());
        assertNull(server.getUnsecuredAddress());
    }
}
