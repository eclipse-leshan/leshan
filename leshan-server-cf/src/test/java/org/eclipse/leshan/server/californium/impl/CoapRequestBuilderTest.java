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
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.server.californium.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.leshan.ObserveSpec;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.CreateRequest;
import org.eclipse.leshan.core.request.DeleteRequest;
import org.eclipse.leshan.core.request.DiscoverRequest;
import org.eclipse.leshan.core.request.ExecuteRequest;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.WriteAttributesRequest;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.request.WriteRequest.Mode;
import org.eclipse.leshan.server.client.Client;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Unit tests for {@link CoapRequestBuilder}
 */
public class CoapRequestBuilderTest {

    private static LwM2mModel model;

    @BeforeClass
    public static void loadModel() {
        model = new LwM2mModel(ObjectLoader.loadDefault());
    }

    private Client newClient() throws UnknownHostException {
        Client client = mock(Client.class);
        InetAddress address = Inet4Address.getByName("127.0.0.1");
        when(client.getAddress()).thenReturn(address);
        when(client.getPort()).thenReturn(12354);
        return client;
    }

    @Test
    public void build_read_request() throws Exception {
        Client client = newClient();

        // test
        CoapRequestBuilder builder = new CoapRequestBuilder(client, model);
        ReadRequest request = new ReadRequest(3, 0);
        builder.visit(request);

        // verify
        Request coapRequest = builder.getRequest();
        assertEquals(CoAP.Code.GET, coapRequest.getCode());
        assertEquals("127.0.0.1", coapRequest.getDestination().getHostAddress());
        assertEquals(12354, coapRequest.getDestinationPort());
        assertEquals("coap://localhost/3/0", coapRequest.getURI());
    }

    @Test
    public void build_read_request_with_non_default_object_path() throws Exception {
        Client client = newClient();
        when(client.getRootPath()).thenReturn("/lwm2m");

        // test
        CoapRequestBuilder builder = new CoapRequestBuilder(client, model);
        ReadRequest request = new ReadRequest(3, 0, 1);
        builder.visit(request);

        // verify
        Request coapRequest = builder.getRequest();
        assertEquals("coap://localhost/lwm2m/3/0/1", coapRequest.getURI());
    }

    @Test
    public void build_read_request_with_root_path() throws Exception {
        Client client = newClient();
        when(client.getRootPath()).thenReturn("/");

        // test
        CoapRequestBuilder builder = new CoapRequestBuilder(client, model);
        ReadRequest request = new ReadRequest(3);
        builder.visit(request);

        // verify
        Request coapRequest = builder.getRequest();
        assertEquals("coap://localhost/3", coapRequest.getURI());
    }

    @Test
    public void build_discover_request() throws Exception {
        Client client = newClient();

        // test
        CoapRequestBuilder builder = new CoapRequestBuilder(client, model);
        DiscoverRequest request = new DiscoverRequest(3, 0);
        builder.visit(request);

        // verify
        Request coapRequest = builder.getRequest();
        assertEquals(CoAP.Code.GET, coapRequest.getCode());
        assertEquals("127.0.0.1", coapRequest.getDestination().getHostAddress());
        assertEquals(12354, coapRequest.getDestinationPort());
        assertEquals(MediaTypeRegistry.APPLICATION_LINK_FORMAT, coapRequest.getOptions().getAccept());
        assertEquals("coap://localhost/3/0", coapRequest.getURI());
    }

    @Test
    public void build_write_request() throws Exception {
        Client client = newClient();

        // test
        CoapRequestBuilder builder = new CoapRequestBuilder(client, model);
        WriteRequest request = new WriteRequest(Mode.UPDATE, 3, 0, 14, "value");
        builder.visit(request);

        // verify
        Request coapRequest = builder.getRequest();
        assertEquals(CoAP.Code.POST, coapRequest.getCode());
        assertEquals("127.0.0.1", coapRequest.getDestination().getHostAddress());
        assertEquals(12354, coapRequest.getDestinationPort());
        assertEquals("value", coapRequest.getPayloadString());
        assertEquals("coap://localhost/3/0/14", coapRequest.getURI());
    }

    @Test
    public void build_write_request_replace() throws Exception {
        Client client = newClient();

        // test
        CoapRequestBuilder builder = new CoapRequestBuilder(client, model);
        WriteRequest request = new WriteRequest(Mode.REPLACE, 3, 0, 14, "value");
        builder.visit(request);

        // verify
        Request coapRequest = builder.getRequest();
        assertEquals(CoAP.Code.PUT, coapRequest.getCode());
    }

    @Test
    public void build_write_attribute_request() throws Exception {
        Client client = newClient();

        // test
        CoapRequestBuilder builder = new CoapRequestBuilder(client, model);
        WriteAttributesRequest request = new WriteAttributesRequest(3, 0, 14, new ObserveSpec.Builder().minPeriod(10)
                .maxPeriod(100).build());
        builder.visit(request);

        // verify
        Request coapRequest = builder.getRequest();
        assertEquals(CoAP.Code.PUT, coapRequest.getCode());
        assertEquals("127.0.0.1", coapRequest.getDestination().getHostAddress());
        assertEquals(12354, coapRequest.getDestinationPort());
        assertEquals("coap://localhost/3/0/14?pmin=10&pmax=100", coapRequest.getURI());
    }

    @Test
    public void build_execute_request() throws Exception {
        Client client = newClient();

        // test
        CoapRequestBuilder builder = new CoapRequestBuilder(client, model);
        ExecuteRequest request = new ExecuteRequest(3, 0, 12, "params");
        builder.visit(request);

        // verify
        Request coapRequest = builder.getRequest();
        assertEquals(CoAP.Code.POST, coapRequest.getCode());
        assertEquals("127.0.0.1", coapRequest.getDestination().getHostAddress());
        assertEquals(12354, coapRequest.getDestinationPort());
        assertEquals("coap://localhost/3/0/12", coapRequest.getURI());
        assertEquals("params", coapRequest.getPayloadString());
    }

    @Test
    public void build_create_request() throws Exception {
        Client client = newClient();

        // test
        CoapRequestBuilder builder = new CoapRequestBuilder(client, model);
        CreateRequest request = new CreateRequest(12, 0, LwM2mSingleResource.newStringResource(0, "value"));
        builder.visit(request);

        // verify
        Request coapRequest = builder.getRequest();
        assertEquals(CoAP.Code.POST, coapRequest.getCode());
        assertEquals("127.0.0.1", coapRequest.getDestination().getHostAddress());
        assertEquals(12354, coapRequest.getDestinationPort());
        assertEquals("coap://localhost/12/0", coapRequest.getURI());
        assertNotNull(coapRequest.getPayload());
        assertEquals(ContentFormat.TLV.getCode(), coapRequest.getOptions().getContentFormat());
    }

    @Test
    public void build_delete_request() throws Exception {
        Client client = newClient();

        // test
        CoapRequestBuilder builder = new CoapRequestBuilder(client, model);
        DeleteRequest request = new DeleteRequest(12, 0);
        builder.visit(request);

        // verify
        Request coapRequest = builder.getRequest();
        assertEquals(CoAP.Code.DELETE, coapRequest.getCode());
        assertEquals("127.0.0.1", coapRequest.getDestination().getHostAddress());
        assertEquals(12354, coapRequest.getDestinationPort());
        assertEquals("coap://localhost/12/0", coapRequest.getURI());
    }

    @Test
    public void build_observe_request() throws Exception {
        Client client = newClient();

        // test
        CoapRequestBuilder builder = new CoapRequestBuilder(client, model);
        ObserveRequest request = new ObserveRequest(12, 0);
        builder.visit(request);

        // verify
        Request coapRequest = builder.getRequest();
        assertEquals(CoAP.Code.GET, coapRequest.getCode());
        assertEquals(0, coapRequest.getOptions().getObserve().intValue());
        assertEquals("127.0.0.1", coapRequest.getDestination().getHostAddress());
        assertEquals(12354, coapRequest.getDestinationPort());
        assertEquals("coap://localhost/12/0", coapRequest.getURI());
    }
}
