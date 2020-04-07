/*******************************************************************************
 * Copyright (c) 2019 Sierra Wireless and others.
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
package org.eclipse.leshan.client.util;

import static org.junit.Assert.assertEquals;

import java.net.InetSocketAddress;

import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.request.Identity;
import org.junit.Test;

public class ServerTest {

    @Test
    public void test_server_getUri_with_localhost_hostname() {
        ServerIdentity server = new ServerIdentity(Identity.unsecure(new InetSocketAddress("localhost", 5683)), 123l);
        assertEquals("coap://localhost:5683", server.getUri());
    }

    @Test
    public void test_server_getUri_with_unknown_hostname() {
        ServerIdentity server = new ServerIdentity(Identity.unsecure(new InetSocketAddress("unknownhost", 5683)), 123l);
        assertEquals("coap://unknownhost:5683", server.getUri());
    }

    @Test
    public void test_server_getUri_with_ipaddress() {
        ServerIdentity server = new ServerIdentity(Identity.unsecure(new InetSocketAddress("50.0.0.1", 5683)), 123l);
        assertEquals("coap://50.0.0.1:5683", server.getUri());
    }
}
