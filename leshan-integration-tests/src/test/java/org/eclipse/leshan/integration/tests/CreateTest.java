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
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.CreateRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.RegisterRequest;
import org.eclipse.leshan.core.response.CreateResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class CreateTest {

    IntegrationTestHelper helper = new IntegrationTestHelper();

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
    public void can_create_instance_of_object() {
        // client registration
        helper.client.send(new RegisterRequest(ENDPOINT_IDENTIFIER));

        // create ACL instance
        CreateResponse response = helper.server.send(helper.getClient(), new CreateRequest(2, 0, new LwM2mResource[0]));

        // verify result
        assertEquals(ResponseCode.CREATED, response.getCode());
        assertEquals("2/0", response.getLocation());
    }

    @Test
    public void can_create_instance_of_object_without_instance_id() {
        // client registration
        helper.client.send(new RegisterRequest(ENDPOINT_IDENTIFIER));

        // create ACL instance
        CreateResponse response = helper.server.send(helper.getClient(), new CreateRequest(2, new LwM2mResource[0]));

        // verify result
        assertEquals(ResponseCode.CREATED, response.getCode());
        assertEquals("2/0", response.getLocation());

        // create a second ACL instance
        response = helper.server.send(helper.getClient(), new CreateRequest(2, new LwM2mResource[0]));

        // verify result
        assertEquals(ResponseCode.CREATED, response.getCode());
        assertEquals("2/1", response.getLocation());

    }

    @Test
    public void can_create_specific_instance_of_object() {
        // client registration
        helper.client.send(new RegisterRequest(ENDPOINT_IDENTIFIER));

        // create ACL instance
        LwM2mResource accessControlOwner = LwM2mSingleResource.newIntegerResource(3, 123);
        CreateResponse response = helper.server.send(helper.getClient(), new CreateRequest(2, 0, accessControlOwner));

        // verify result
        assertEquals(ResponseCode.CREATED, response.getCode());
        assertEquals("2/0", response.getLocation());
    }

    @Test
    public void can_create_specific_instance_of_object_with_json() {
        // client registration
        helper.client.send(new RegisterRequest(ENDPOINT_IDENTIFIER));

        // create ACL instance
        LwM2mResource accessControlOwner = LwM2mSingleResource.newIntegerResource(3, 123);
        CreateResponse response = helper.server.send(helper.getClient(), new CreateRequest(ContentFormat.JSON, 2, 0,
                accessControlOwner));

        // verify result
        assertEquals(ResponseCode.CREATED, response.getCode());
        assertEquals("2/0", response.getLocation());
    }

    @Test
    public void cannot_create_instance_of_object() {
        // client registration
        helper.client.send(new RegisterRequest(ENDPOINT_IDENTIFIER));

        // try to create an instance of object 50
        CreateResponse response = helper.server
                .send(helper.getClient(), new CreateRequest(50, 0, new LwM2mResource[0]));

        // verify result
        assertEquals(ResponseCode.NOT_FOUND, response.getCode());
    }

    // TODO not sure all the writable mandatory resource should be present
    // E.g. for softwareUpdate (object 9) packageURI and package are writable resource mandatory
    // but you will not make a create with this two resource.
    @Ignore
    @Test
    public void cannot_create_instance_without_all_required_resources() {
        // client registration
        helper.client.send(new RegisterRequest(ENDPOINT_IDENTIFIER));

        // create ACL instance
        CreateResponse response = helper.server.send(helper.getClient(), new CreateRequest(2, 0, new LwM2mResource[0]));

        // verify result
        assertEquals(ResponseCode.BAD_REQUEST, response.getCode());

        // try to read to check if the instance is not created
        // client registration
        ReadResponse readResponse = helper.server.send(helper.getClient(), new ReadRequest(2, 0));
        assertEquals(ResponseCode.NOT_FOUND, readResponse.getCode());
    }

    // TODO we must probably implement this.
    @Ignore
    @Test
    public void cannot_create_instance_with_extraneous_resources() {
        // client registration
        helper.client.send(new RegisterRequest(ENDPOINT_IDENTIFIER));

        // create ACL instance
        LwM2mResource accessControlOwner = LwM2mSingleResource.newIntegerResource(3, 123);
        LwM2mResource extraneousResource = LwM2mSingleResource.newIntegerResource(50, 123);
        CreateRequest request = new CreateRequest(2, 0, accessControlOwner, extraneousResource);
        CreateResponse response = helper.server.send(helper.getClient(), request);

        // verify result
        assertEquals(ResponseCode.BAD_REQUEST, response.getCode());

        // try to read to check if the instance is not created
        // client registration
        ReadResponse readResponse = helper.server.send(helper.getClient(), new ReadRequest(2, 0));
        assertEquals(ResponseCode.NOT_FOUND, readResponse.getCode());
    }

    // TODO I'm not sure we can do use only writable resource on create
    // see https://github.com/OpenMobileAlliance/OMA-LwM2M-Public-Review/issues/30
    @Ignore
    @Test
    public void cannot_create_instance_with_non_writable_resource() {
    }

    @Test
    public void cannot_create_mandatory_single_object() {
        // client registration
        helper.client.send(new RegisterRequest(ENDPOINT_IDENTIFIER));

        // try to create another instance of device object
        CreateResponse response = helper.server.send(helper.getClient(), new CreateRequest(3, 0, new LwM2mResource[0]));

        // verify result
        assertEquals(ResponseCode.METHOD_NOT_ALLOWED, response.getCode());
    }
}
