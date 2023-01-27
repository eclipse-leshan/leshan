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
 *     Zebra Technologies - initial API and implementation
 *     Achim Kraus (Bosch Software Innovations GmbH) - add test for deleting a resource
 *     Achim Kraus (Bosch Software Innovations GmbH) - add test for delete security object
 *     Achim Kraus (Bosch Software Innovations GmbH) - replace close() with destroy()
 *******************************************************************************/

package org.eclipse.leshan.integration.tests;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.elements.AddressEndpointContext;
import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.californium.CoapSyncRequestObserver;
import org.eclipse.leshan.core.californium.DefaultExceptionTranslator;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.request.CreateRequest;
import org.eclipse.leshan.core.request.DeleteRequest;
import org.eclipse.leshan.core.response.DeleteResponse;
import org.eclipse.leshan.integration.tests.util.IntegrationTestHelper;
import org.eclipse.leshan.server.californium.endpoint.CaliforniumServerEndpoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DeleteTest {

    private final IntegrationTestHelper helper = new IntegrationTestHelper();

    @BeforeEach
    public void start() {
        helper.initialize();
        helper.createServer();
        helper.server.start();
        helper.createClient();
        helper.client.start();
        helper.waitForRegistrationAtServerSide(1);
    }

    @AfterEach
    public void stop() {
        helper.client.destroy(false);
        helper.server.destroy();
        helper.dispose();
    }

    @Test
    public void delete_created_object_instance() throws InterruptedException {
        // create ACL instance
        helper.server.send(helper.getCurrentRegistration(),
                new CreateRequest(2, new LwM2mObjectInstance(0, LwM2mSingleResource.newIntegerResource(3, 33))));

        // try to delete this instance
        DeleteResponse response = helper.server.send(helper.getCurrentRegistration(), new DeleteRequest(2, 0));

        // verify result
        assertEquals(ResponseCode.DELETED, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));
    }

    @Test
    public void cannot_delete_resource() throws InterruptedException {
        // create ACL instance
        helper.server.send(helper.getCurrentRegistration(),
                new CreateRequest(2, new LwM2mObjectInstance(0, LwM2mSingleResource.newIntegerResource(0, 123))));

        // try to delete this resource using coap API as lwm2m API does not allow it.
        Request delete = Request.newDelete();
        delete.getOptions().addUriPath("2").addUriPath("0").addUriPath("0");

        // TODO TL add Coap API again ?
        delete.setDestinationContext(new AddressEndpointContext(helper.getCurrentRegistration().getSocketAddress()));
        CoapSyncRequestObserver syncMessageObserver = new CoapSyncRequestObserver(delete, 2000,
                new DefaultExceptionTranslator());
        delete.addMessageObserver(syncMessageObserver);

        CaliforniumServerEndpoint endpoint = (CaliforniumServerEndpoint) helper.server.getEndpoint(Protocol.COAP);
        endpoint.getCoapEndpoint().sendRequest(delete);
        Response response = syncMessageObserver.waitForCoapResponse();

        // verify result
        assertEquals(org.eclipse.californium.core.coap.CoAP.ResponseCode.BAD_REQUEST, response.getCode());
    }

    @Test
    public void cannot_delete_unknown_object_instance() throws InterruptedException {
        // try to create an instance of object 50
        DeleteResponse response = helper.server.send(helper.getCurrentRegistration(), new DeleteRequest(2, 0));

        // verify result
        assertEquals(ResponseCode.NOT_FOUND, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));
    }

    @Test
    public void cannot_delete_device_object_instance() throws InterruptedException {
        // try to create an instance of object 50
        DeleteResponse response = helper.server.send(helper.getCurrentRegistration(), new DeleteRequest(3, 0));

        // verify result
        assertEquals(ResponseCode.METHOD_NOT_ALLOWED, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));
    }

    @Test
    public void cannot_delete_security_object_instance() throws InterruptedException {
        DeleteResponse response = helper.server.send(helper.getCurrentRegistration(), new DeleteRequest(0, 0));

        // verify result
        assertEquals(ResponseCode.NOT_FOUND, response.getCode());
        assertNotNull(response.getCoapResponse());
        assertThat(response.getCoapResponse(), is(instanceOf(Response.class)));
    }

}
