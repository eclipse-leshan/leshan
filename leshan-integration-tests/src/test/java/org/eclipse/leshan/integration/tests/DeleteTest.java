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

import static org.eclipse.leshan.integration.tests.IntegrationTestHelper.ENDPOINT_IDENTIFIER;
import static org.junit.Assert.assertEquals;

import org.eclipse.leshan.ResponseCode;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.request.CreateRequest;
import org.eclipse.leshan.core.request.DeleteRequest;
import org.eclipse.leshan.core.request.RegisterRequest;
import org.eclipse.leshan.core.response.DeleteResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DeleteTest {

    private final IntegrationTestHelper helper = new IntegrationTestHelper();

    @Before
    public void start() {
        helper.createServer();
        helper.server.start();
        helper.createClient();
        helper.client.start();
    }

    @After
    public void stop() {
        helper.server.stop();
        helper.client.stop();
    }

    @Test
    public void delete_created_object_instance() {
        // client registration
        helper.client.send(new RegisterRequest(ENDPOINT_IDENTIFIER));

        // create ACL instance
        helper.server.send(helper.getClient(), new CreateRequest(2, 0, new LwM2mResource[0]));

        // try to delete this instance
        DeleteResponse deleteResponse = helper.server.send(helper.getClient(), new DeleteRequest(2, 0));

        // verify result
        assertEquals(ResponseCode.DELETED, deleteResponse.getCode());
    }

    @Test
    public void cannot_delete_unknown_object_instance() {
        // client registration
        helper.client.send(new RegisterRequest(ENDPOINT_IDENTIFIER));

        // try to create an instance of object 50
        DeleteResponse response = helper.server.send(helper.getClient(), new DeleteRequest(2, 0));

        // verify result
        assertEquals(ResponseCode.NOT_FOUND, response.getCode());
    }

    @Test
    public void cannot_delete_single_manadatory_object_instance() {
        // client registration
        helper.client.send(new RegisterRequest(ENDPOINT_IDENTIFIER));

        // try to create an instance of object 50
        DeleteResponse response = helper.server.send(helper.getClient(), new DeleteRequest(3, 0));

        // verify result
        assertEquals(ResponseCode.METHOD_NOT_ALLOWED, response.getCode());
    }
}
