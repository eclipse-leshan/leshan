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
 *     Achim Kraus (Bosch Software Innovations GmbH) - replace close() with destroy()
 *******************************************************************************/

package org.eclipse.leshan.integration.tests;

import static org.eclipse.leshan.integration.tests.IntegrationTestHelper.LIFETIME;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.californium.core.coap.CoAP.Code;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.californium.elements.AddressEndpointContext;
import org.eclipse.leshan.Link;
import org.eclipse.leshan.ResponseCode;
import org.eclipse.leshan.client.californium.LeshanClient;
import org.eclipse.leshan.client.californium.LeshanClientBuilder;
import org.eclipse.leshan.client.californium.impl.CaliforniumLwM2mRequestSender;
import org.eclipse.leshan.client.resource.LwM2mInstanceEnabler;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectEnabler;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.DeregisterRequest;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.RegisterRequest;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.RegisterResponse;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.security.NonUniqueSecurityInfoException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class RegistrationTest {

    protected IntegrationTestHelper helper = new IntegrationTestHelper();

    @Before
    public void start() {
        helper.initialize();
        helper.createServer();
        helper.server.start();
        helper.createClient();
    }

    @After
    public void stop() throws InterruptedException {
        helper.client.destroy(true);
        helper.server.destroy();
        helper.dispose();
    }

    @Test
    public void register_update_deregister() {
        // Check client is not registered
        helper.assertClientNotRegisterered();

        // Start it and wait for registration
        helper.client.start();
        helper.waitForRegistration(1);

        // Check client is well registered
        helper.assertClientRegisterered();
        assertArrayEquals(Link.parse("</>;rt=\"oma.lwm2m\",</1/0>,</2>,</3/0>,</2000/0>".getBytes()),
                helper.getCurrentRegistration().getObjectLinks());

        // Check for update
        helper.waitForUpdate(LIFETIME);
        helper.assertClientRegisterered();

        // Check deregistration
        helper.client.stop(true);
        helper.waitForDeregistration(1);
        helper.assertClientNotRegisterered();
    }

    @Test
    public void deregister_cancel_multiple_pending_request() throws InterruptedException {
        // Check client is not registered
        helper.assertClientNotRegisterered();

        // Start it and wait for registration
        helper.client.start();
        helper.waitForRegistration(1);

        // Check client is well registered
        helper.assertClientRegisterered();
        assertArrayEquals(Link.parse("</>;rt=\"oma.lwm2m\",</1/0>,</2>,</3/0>,</2000/0>".getBytes()),
                helper.getCurrentRegistration().getObjectLinks());

        // Stop client with out de-registration
        helper.client.stop(false);

        // Send multiple reads which should be retransmitted.
        List<Callback<ReadResponse>> callbacks = new ArrayList<>();

        for (int index = 0; index < 4; ++index) {
            Callback<ReadResponse> callback = new Callback<>();
            helper.server.send(helper.getCurrentRegistration(), new ReadRequest(3, 0, 1), callback, callback);
            callbacks.add(callback);
        }

        // Restart client (de-registration/re-registration)
        helper.client.start();

        // Check the request was cancelled.
        int index = 0;
        for (Callback<ReadResponse> callback : callbacks) {
            boolean timedout = !callback.waitForResponse(1000);
            assertFalse("Response or Error expected, no timeout, call " + index, timedout);
            assertTrue("Response or Error expected, call " + index, callback.isCalled().get());
            assertNull("No response expected, call " + index, callback.getResponse());
            assertNotNull("Exception expected, call " + index, callback.getException());
            ++index;
        }
    }

    @Test
    public void register_update_deregister_reregister() throws NonUniqueSecurityInfoException, InterruptedException {
        // Check client is not registered
        helper.assertClientNotRegisterered();

        // Start it and wait for registration
        helper.client.start();
        helper.waitForRegistration(1);

        // Check client is well registered
        helper.assertClientRegisterered();

        // Check for update
        helper.waitForUpdate(LIFETIME);
        helper.assertClientRegisterered();

        // Check de-registration
        helper.client.stop(true);
        helper.waitForDeregistration(1);
        helper.assertClientNotRegisterered();

        // Check new registration
        helper.resetLatch();
        helper.client.start();
        helper.waitForRegistration(1);
        helper.assertClientRegisterered();
    }

    @Test
    public void register_update_reregister() throws NonUniqueSecurityInfoException, InterruptedException {
        // Check client is not registered
        helper.assertClientNotRegisterered();

        // Start it and wait for registration
        helper.client.start();
        helper.waitForRegistration(1);

        // Check client is well registered
        helper.assertClientRegisterered();

        // Check for update
        helper.waitForUpdate(LIFETIME);
        helper.assertClientRegisterered();

        // check stop do not de-register
        helper.client.stop(false);
        helper.ensureNoDeregistration(1);
        helper.assertClientRegisterered();

        // check new registration
        helper.resetLatch();
        helper.client.start();
        helper.waitForRegistration(1);
        helper.assertClientRegisterered();
    }

    @Test
    public void register_observe_deregister_observe() throws NonUniqueSecurityInfoException, InterruptedException {
        // Check client is not registered
        helper.assertClientNotRegisterered();

        // Start it and wait for registration
        helper.client.start();
        helper.waitForRegistration(1);

        // Check client is well registered
        helper.assertClientRegisterered();

        // observe device timezone
        ObserveResponse observeResponse = helper.server.send(helper.getCurrentRegistration(), new ObserveRequest(3, 0));
        assertEquals(ResponseCode.CONTENT, observeResponse.getCode());
        assertNotNull(observeResponse.getCoapResponse());
        assertThat(observeResponse.getCoapResponse(), is(instanceOf(Response.class)));

        // check observation registry is not null
        Registration currentRegistration = helper.getCurrentRegistration();
        Set<Observation> observations = helper.server.getObservationService().getObservations(currentRegistration);
        assertEquals(1, observations.size());
        Observation obs = observations.iterator().next();
        assertEquals(currentRegistration.getId(), obs.getRegistrationId());
        assertEquals(new LwM2mPath(3, 0), obs.getPath());

        // Check de-registration
        helper.client.stop(true);
        helper.waitForDeregistration(1);
        helper.assertClientNotRegisterered();
        observations = helper.server.getObservationService().getObservations(currentRegistration);
        assertTrue(observations.isEmpty());

        // try to send a new observation
        observeResponse = helper.server.send(currentRegistration, new ObserveRequest(3, 0), 50);
        assertNull(observeResponse);

        // check observationStore is empty
        observations = helper.server.getObservationService().getObservations(currentRegistration);
        assertTrue(observations.isEmpty());
    }

    // TODO not really a registration test
    @Test(expected = IllegalArgumentException.class)
    public void fail_to_create_client_with_same_object_twice() {
        ObjectEnabler objectEnabler = new ObjectEnabler(1, null, new HashMap<Integer, LwM2mInstanceEnabler>(), null);
        ObjectEnabler objectEnabler2 = new ObjectEnabler(1, null, new HashMap<Integer, LwM2mInstanceEnabler>(), null);
        ArrayList<LwM2mObjectEnabler> objects = new ArrayList<>();
        objects.add(objectEnabler);
        objects.add(objectEnabler2);
        helper.client = new LeshanClientBuilder("test").setObjects(objects).build();
    }

    @Test
    public void register_with_additional_attributes() throws InterruptedException {
        // Check registration
        helper.assertClientNotRegisterered();

        // HACK to be able to send a Registration request with additional attributes
        LeshanClient lclient = helper.client;
        lclient.getCoapServer().start();
        Endpoint secureEndpoint = lclient.getCoapServer().getEndpoint(lclient.getSecuredAddress());
        Endpoint nonSecureEndpoint = lclient.getCoapServer().getEndpoint(lclient.getUnsecuredAddress());
        CaliforniumLwM2mRequestSender sender = new CaliforniumLwM2mRequestSender(secureEndpoint, nonSecureEndpoint);

        // Create Request with additional attributes
        Map<String, String> additionalAttributes = new HashMap<>();
        additionalAttributes.put("key1", "value1");
        additionalAttributes.put("imei", "2136872368");
        Link[] objectLinks = Link.parse("</>;rt=\"oma.lwm2m\",</1/0>,</2>,</3/0>".getBytes());
        RegisterRequest registerRequest = new RegisterRequest(helper.getCurrentEndpoint(), null, null, null, null,
                objectLinks, additionalAttributes);

        // Send request
        RegisterResponse resp = sender.send(helper.server.getUnsecuredAddress(), false, registerRequest, 5000l);
        helper.waitForRegistration(1);

        // Check we are registered with the expected attributes
        helper.assertClientRegisterered();
        assertNotNull(helper.last_registration);
        assertEquals(additionalAttributes, helper.last_registration.getAdditionalRegistrationAttributes());
        assertArrayEquals(Link.parse("</>;rt=\"oma.lwm2m\",</1/0>,</2>,</3/0>".getBytes()),
                helper.getCurrentRegistration().getObjectLinks());

        sender.send(helper.server.getUnsecuredAddress(), false, new DeregisterRequest(resp.getRegistrationID()), 5000l);
        lclient.getCoapServer().stop();
    }

    @Test
    public void register_with_invalid_request() throws InterruptedException, IOException {
        // Check registration
        helper.assertClientNotRegisterered();

        // create a register request without the list of supported object
        Request coapRequest = new Request(Code.POST);
        coapRequest.setDestinationContext(new AddressEndpointContext(helper.server.getUnsecuredAddress()));
        coapRequest.getOptions().setContentFormat(ContentFormat.LINK.getCode());
        coapRequest.getOptions().addUriPath("rd");
        coapRequest.getOptions().addUriQuery("ep=" + helper.currentEndpointIdentifier);

        // send request
        CoapEndpoint coapEndpoint = new CoapEndpoint(new InetSocketAddress(0));
        coapEndpoint.start();
        coapEndpoint.sendRequest(coapRequest);

        // check response
        Response response = coapRequest.waitForResponse(1000);
        assertEquals(response.getCode(), org.eclipse.californium.core.coap.CoAP.ResponseCode.BAD_REQUEST);
        coapEndpoint.stop();
    }
}
