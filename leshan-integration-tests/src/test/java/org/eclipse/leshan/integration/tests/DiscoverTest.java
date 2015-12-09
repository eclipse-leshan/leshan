/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
 *     Zebra Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.leshan.integration.tests;

import static org.eclipse.leshan.ResponseCode.CONTENT;
import static org.eclipse.leshan.ResponseCode.NOT_FOUND;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.eclipse.leshan.LinkObject;
import org.eclipse.leshan.core.request.DiscoverRequest;
import org.eclipse.leshan.core.response.DiscoverResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DiscoverTest {

    private IntegrationTestHelper helper = new IntegrationTestHelper();

    @Before
    public void start() {
        helper.createServer();
        helper.server.start();
        helper.createClient();
        helper.client.start();
        helper.waitForRegistration(1);
    }

    @After
    public void stop() {
        helper.client.stop();
        helper.server.stop();
    }

    @Test
    public void can_discover_object() {
        // read ACL object
        DiscoverResponse response = helper.server.send(helper.getClient(), new DiscoverRequest(2));

        // verify result
        assertEquals(CONTENT, response.getCode());

        LinkObject[] payload = response.getObjectLinks();
        assertArrayEquals(LinkObject.parse("</2>, </2/0/0>, </2/0/1>, </2/0/2>, </2/0/3>".getBytes()), payload);
    }

    @Test
    public void cant_discover_non_existent_object() {
        // read ACL object
        DiscoverResponse response = helper.server.send(helper.getClient(), new DiscoverRequest(4));

        // verify result
        assertEquals(NOT_FOUND, response.getCode());
    }

    @Test
    public void can_discover_object_instance() {
        // read ACL object
        DiscoverResponse response = helper.server.send(helper.getClient(), new DiscoverRequest(3, 0));

        // verify result
        assertEquals(CONTENT, response.getCode());

        LinkObject[] payload = response.getObjectLinks();
        assertArrayEquals(LinkObject.parse("</3/0>".getBytes()), payload);
    }

    @Test
    public void cant_discover_non_existent_instance() {
        // read ACL object
        DiscoverResponse response = helper.server.send(helper.getClient(), new DiscoverRequest(3, 1));

        // verify result
        assertEquals(NOT_FOUND, response.getCode());
    }

    @Test
    public void can_discover_resource() {
        // read ACL object
        DiscoverResponse response = helper.server.send(helper.getClient(), new DiscoverRequest(3, 0, 0));

        // verify result
        assertEquals(CONTENT, response.getCode());

        LinkObject[] payload = response.getObjectLinks();
        assertArrayEquals(LinkObject.parse("</3/0/0>".getBytes()), payload);
    }

    @Test
    public void cant_discover_resource_of_non_existent_object() {
        // read ACL object
        DiscoverResponse response = helper.server.send(helper.getClient(), new DiscoverRequest(4, 0, 0));

        // verify result
        assertEquals(NOT_FOUND, response.getCode());
    }

    @Test
    public void cant_discover_resource_of_non_existent_instance() {
        // read ACL object
        DiscoverResponse response = helper.server.send(helper.getClient(), new DiscoverRequest(3, 1, 0));

        // verify result
        assertEquals(NOT_FOUND, response.getCode());
    }

    @Test
    public void cant_discover_resource_of_non_existent_instance_and_resource() {
        // read ACL object
        DiscoverResponse response = helper.server.send(helper.getClient(), new DiscoverRequest(3, 1, 20));

        // verify result
        assertEquals(NOT_FOUND, response.getCode());
    }

    @Test
    public void cant_discover_resource_of_non_existent_resource() {
        // read ACL object
        DiscoverResponse response = helper.server.send(helper.getClient(), new DiscoverRequest(3, 0, 20));

        // verify result
        assertEquals(NOT_FOUND, response.getCode());
    }
}
