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
 *     Achim Kraus (Bosch Software Innovations GmbH) - use destination context
 *                                                     instead of address for response
 *******************************************************************************/

package org.eclipse.leshan.integration.tests;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.californium.core.coap.CoAP.Type;
import org.eclipse.californium.core.coap.OptionSet;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.serialization.UdpDataSerializer;
import org.eclipse.californium.elements.Connector;
import org.eclipse.californium.elements.AddressEndpointContext;
import org.eclipse.californium.elements.EndpointContext;
import org.eclipse.californium.elements.RawData;
import org.eclipse.leshan.ResponseCode;
import org.eclipse.leshan.client.californium.LeshanClient;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.node.TimestampedLwM2mNode;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mValueConverter;
import org.eclipse.leshan.core.node.codec.json.LwM2mNodeJsonEncoder;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.server.observation.ObservationListener;
import org.eclipse.leshan.server.registration.Registration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ObserveTest {

    protected IntegrationTestHelper helper = new IntegrationTestHelper();

    @Before
    public void start() {
        helper.initialize();
        helper.createServer();
        helper.server.start();
        helper.createClient();
        helper.client.start();
        helper.waitForRegistration(1);
    }

    @After
    public void stop() {
        helper.client.destroy(false);
        helper.server.destroy();
        helper.dispose();
    }

    @Test
    public void can_observe_resource() throws InterruptedException {
        TestObservationListener listener = new TestObservationListener();
        helper.server.getObservationService().addListener(listener);

        // observe device timezone
        ObserveResponse observeResponse = helper.server.send(helper.getCurrentRegistration(),
                new ObserveRequest(3, 0, 15));
        assertEquals(ResponseCode.CONTENT, observeResponse.getCode());
        assertNotNull(observeResponse.getCoapResponse());
        assertThat(observeResponse.getCoapResponse(), is(instanceOf(Response.class)));

        // an observation response should have been sent
        Observation observation = observeResponse.getObservation();
        assertEquals("/3/0/15", observation.getPath().toString());
        assertEquals(helper.getCurrentRegistration().getId(), observation.getRegistrationId());

        // write device timezone
        LwM2mResponse writeResponse = helper.server.send(helper.getCurrentRegistration(),
                new WriteRequest(3, 0, 15, "Europe/Paris"));

        // verify result
        listener.waitForNotification(2000);
        assertEquals(ResponseCode.CHANGED, writeResponse.getCode());
        assertTrue(listener.receivedNotify().get());
        assertEquals(LwM2mSingleResource.newStringResource(15, "Europe/Paris"), listener.getResponse().getContent());
        assertNotNull(listener.getResponse().getCoapResponse());
        assertThat(listener.getResponse().getCoapResponse(), is(instanceOf(Response.class)));
    }

    @Test
    public void can_observe_instance() throws InterruptedException {
        TestObservationListener listener = new TestObservationListener();
        helper.server.getObservationService().addListener(listener);

        // observe device timezone
        ObserveResponse observeResponse = helper.server.send(helper.getCurrentRegistration(), new ObserveRequest(3, 0));
        assertEquals(ResponseCode.CONTENT, observeResponse.getCode());
        assertNotNull(observeResponse.getCoapResponse());
        assertThat(observeResponse.getCoapResponse(), is(instanceOf(Response.class)));

        // an observation response should have been sent
        Observation observation = observeResponse.getObservation();
        assertEquals("/3/0", observation.getPath().toString());
        assertEquals(helper.getCurrentRegistration().getId(), observation.getRegistrationId());

        // write device timezone
        LwM2mResponse writeResponse = helper.server.send(helper.getCurrentRegistration(),
                new WriteRequest(3, 0, 15, "Europe/Paris"));

        // verify result
        listener.waitForNotification(2000);
        assertEquals(ResponseCode.CHANGED, writeResponse.getCode());
        assertTrue(listener.receivedNotify().get());
        assertTrue(listener.getResponse().getContent() instanceof LwM2mObjectInstance);
        assertNotNull(listener.getResponse().getCoapResponse());
        assertThat(listener.getResponse().getCoapResponse(), is(instanceOf(Response.class)));

        // try to read the object instance for comparing
        ReadResponse readResp = helper.server.send(helper.getCurrentRegistration(), new ReadRequest(3, 0));

        assertEquals(readResp.getContent(), listener.getResponse().getContent());
    }

    @Test
    public void can_observe_object() throws InterruptedException {
        TestObservationListener listener = new TestObservationListener();
        helper.server.getObservationService().addListener(listener);

        // observe device timezone
        ObserveResponse observeResponse = helper.server.send(helper.getCurrentRegistration(), new ObserveRequest(3));
        assertEquals(ResponseCode.CONTENT, observeResponse.getCode());
        assertNotNull(observeResponse.getCoapResponse());
        assertThat(observeResponse.getCoapResponse(), is(instanceOf(Response.class)));

        // an observation response should have been sent
        Observation observation = observeResponse.getObservation();
        assertEquals("/3", observation.getPath().toString());
        assertEquals(helper.getCurrentRegistration().getId(), observation.getRegistrationId());

        // write device timezone
        LwM2mResponse writeResponse = helper.server.send(helper.getCurrentRegistration(),
                new WriteRequest(3, 0, 15, "Europe/Paris"));

        // verify result
        listener.waitForNotification(2000);
        assertEquals(ResponseCode.CHANGED, writeResponse.getCode());
        assertTrue(listener.receivedNotify().get());
        assertTrue(listener.getResponse().getContent() instanceof LwM2mObject);
        assertNotNull(listener.getResponse().getCoapResponse());
        assertThat(listener.getResponse().getCoapResponse(), is(instanceOf(Response.class)));

        // try to read the object for comparing
        ReadResponse readResp = helper.server.send(helper.getCurrentRegistration(), new ReadRequest(3));

        assertEquals(readResp.getContent(), listener.getResponse().getContent());
    }

    @Test
    public void can_handle_error_on_notification() throws InterruptedException {
        TestObservationListener listener = new TestObservationListener();
        helper.server.getObservationService().addListener(listener);

        // observe device timezone
        ObserveResponse observeResponse = helper.server.send(helper.getCurrentRegistration(),
                new ObserveRequest(3, 0, 15));
        assertEquals(ResponseCode.CONTENT, observeResponse.getCode());
        assertNotNull(observeResponse.getCoapResponse());
        assertThat(observeResponse.getCoapResponse(), is(instanceOf(Response.class)));

        // an observation response should have been sent
        Observation observation = observeResponse.getObservation();
        assertEquals("/3/0/15", observation.getPath().toString());
        assertEquals(helper.getCurrentRegistration().getId(), observation.getRegistrationId());

        // *** HACK send a notification with unsupported content format *** //
        byte[] payload = LwM2mNodeJsonEncoder.encode(LwM2mSingleResource.newStringResource(15, "Paris"),
                new LwM2mPath("/3/0/15"), new LwM2mModel(helper.createObjectModels()),
                new DefaultLwM2mValueConverter());
        Response firstCoapResponse = (Response) observeResponse.getCoapResponse();
        sendNotification(getConnector(helper.client), payload, firstCoapResponse, 666); // 666 is not a supported //
                                                                                        // contentFormat.
        // *** Hack End *** //

        // verify result
        listener.waitForNotification(2000);
        assertTrue(listener.receivedNotify().get());
        assertNotNull(listener.getError());
    }

    @Test
    public void can_observe_timestamped_resource() throws InterruptedException {
        TestObservationListener listener = new TestObservationListener();
        helper.server.getObservationService().addListener(listener);

        // observe device timezone
        ObserveResponse observeResponse = helper.server.send(helper.getCurrentRegistration(),
                new ObserveRequest(3, 0, 15));
        assertEquals(ResponseCode.CONTENT, observeResponse.getCode());
        assertNotNull(observeResponse.getCoapResponse());
        assertThat(observeResponse.getCoapResponse(), is(instanceOf(Response.class)));

        // an observation response should have been sent
        Observation observation = observeResponse.getObservation();
        assertEquals("/3/0/15", observation.getPath().toString());
        assertEquals(helper.getCurrentRegistration().getId(), observation.getRegistrationId());

        // *** HACK send time-stamped notification as Leshan client does not support it *** //
        // create time-stamped nodes
        TimestampedLwM2mNode mostRecentNode = new TimestampedLwM2mNode(System.currentTimeMillis(),
                LwM2mSingleResource.newStringResource(15, "Paris"));
        List<TimestampedLwM2mNode> timestampedNodes = new ArrayList<>();
        timestampedNodes.add(mostRecentNode);
        timestampedNodes.add(new TimestampedLwM2mNode(mostRecentNode.getTimestamp() - 2,
                LwM2mSingleResource.newStringResource(15, "Londres")));
        byte[] payload = LwM2mNodeJsonEncoder.encodeTimestampedData(timestampedNodes, new LwM2mPath("/3/0/15"),
                new LwM2mModel(helper.createObjectModels()), new DefaultLwM2mValueConverter());
        Response firstCoapResponse = (Response) observeResponse.getCoapResponse();
        sendNotification(getConnector(helper.client), payload, firstCoapResponse, ContentFormat.JSON_CODE);
        // *** Hack End *** //

        // verify result
        listener.waitForNotification(2000);
        assertTrue(listener.receivedNotify().get());
        assertEquals(mostRecentNode.getNode(), listener.getResponse().getContent());
        assertEquals(timestampedNodes, listener.getResponse().getTimestampedLwM2mNode());
        assertNotNull(listener.getResponse().getCoapResponse());
        assertThat(listener.getResponse().getCoapResponse(), is(instanceOf(Response.class)));
    }

    @Test
    public void can_observe_timestamped_instance() throws InterruptedException {
        TestObservationListener listener = new TestObservationListener();
        helper.server.getObservationService().addListener(listener);

        // observe device timezone
        ObserveResponse observeResponse = helper.server.send(helper.getCurrentRegistration(), new ObserveRequest(3, 0));
        assertEquals(ResponseCode.CONTENT, observeResponse.getCode());
        assertNotNull(observeResponse.getCoapResponse());
        assertThat(observeResponse.getCoapResponse(), is(instanceOf(Response.class)));

        // an observation response should have been sent
        Observation observation = observeResponse.getObservation();
        assertEquals("/3/0", observation.getPath().toString());
        assertEquals(helper.getCurrentRegistration().getId(), observation.getRegistrationId());

        // *** HACK send time-stamped notification as Leshan client does not support it *** //
        // create time-stamped nodes
        TimestampedLwM2mNode mostRecentNode = new TimestampedLwM2mNode(System.currentTimeMillis(),
                new LwM2mObjectInstance(0, LwM2mSingleResource.newStringResource(15, "Paris")));
        List<TimestampedLwM2mNode> timestampedNodes = new ArrayList<>();
        timestampedNodes.add(mostRecentNode);
        timestampedNodes.add(new TimestampedLwM2mNode(mostRecentNode.getTimestamp() - 2,
                new LwM2mObjectInstance(0, LwM2mSingleResource.newStringResource(15, "Londres"))));
        byte[] payload = LwM2mNodeJsonEncoder.encodeTimestampedData(timestampedNodes, new LwM2mPath("/3/0"),
                new LwM2mModel(helper.createObjectModels()), new DefaultLwM2mValueConverter());
        Response firstCoapResponse = (Response) observeResponse.getCoapResponse();
        sendNotification(getConnector(helper.client), payload, firstCoapResponse, ContentFormat.JSON_CODE);
        // *** Hack End *** //

        // verify result
        listener.waitForNotification(2000);
        assertTrue(listener.receivedNotify().get());
        assertEquals(mostRecentNode.getNode(), listener.getResponse().getContent());
        assertEquals(timestampedNodes, listener.getResponse().getTimestampedLwM2mNode());
        assertNotNull(listener.getResponse().getCoapResponse());
        assertThat(listener.getResponse().getCoapResponse(), is(instanceOf(Response.class)));
    }

    @Test
    public void can_observe_timestamped_object() throws InterruptedException {
        TestObservationListener listener = new TestObservationListener();
        helper.server.getObservationService().addListener(listener);

        // observe device timezone
        ObserveResponse observeResponse = helper.server.send(helper.getCurrentRegistration(), new ObserveRequest(3));
        assertEquals(ResponseCode.CONTENT, observeResponse.getCode());
        assertNotNull(observeResponse.getCoapResponse());
        assertThat(observeResponse.getCoapResponse(), is(instanceOf(Response.class)));

        // an observation response should have been sent
        Observation observation = observeResponse.getObservation();
        assertEquals("/3", observation.getPath().toString());
        assertEquals(helper.getCurrentRegistration().getId(), observation.getRegistrationId());

        // *** HACK send time-stamped notification as Leshan client does not support it *** //
        // create time-stamped nodes
        TimestampedLwM2mNode mostRecentNode = new TimestampedLwM2mNode(System.currentTimeMillis(),
                new LwM2mObject(3, new LwM2mObjectInstance(0, LwM2mSingleResource.newStringResource(15, "Paris"))));
        List<TimestampedLwM2mNode> timestampedNodes = new ArrayList<>();
        timestampedNodes.add(mostRecentNode);
        timestampedNodes.add(new TimestampedLwM2mNode(mostRecentNode.getTimestamp() - 2,
                new LwM2mObject(3, new LwM2mObjectInstance(0, LwM2mSingleResource.newStringResource(15, "Londres")))));
        byte[] payload = LwM2mNodeJsonEncoder.encodeTimestampedData(timestampedNodes, new LwM2mPath("/3"),
                new LwM2mModel(helper.createObjectModels()), new DefaultLwM2mValueConverter());

        Response firstCoapResponse = (Response) observeResponse.getCoapResponse();
        sendNotification(getConnector(helper.client), payload, firstCoapResponse, ContentFormat.JSON_CODE);
        // *** Hack End *** //

        // verify result
        listener.waitForNotification(2000);
        assertTrue(listener.receivedNotify().get());
        assertEquals(mostRecentNode.getNode(), listener.getResponse().getContent());
        assertEquals(timestampedNodes, listener.getResponse().getTimestampedLwM2mNode());
        assertNotNull(listener.getResponse().getCoapResponse());
        assertThat(listener.getResponse().getCoapResponse(), is(instanceOf(Response.class)));
    }

    private void sendNotification(Connector connector, byte[] payload, Response firstCoapResponse, int contentFormat) {

        // create observe response
        Response response = new Response(org.eclipse.californium.core.coap.CoAP.ResponseCode.CONTENT);
        response.setType(Type.NON);
        response.setPayload(payload);
        response.setMID(firstCoapResponse.getMID() + 1);
        response.setToken(firstCoapResponse.getToken());
        OptionSet options = new OptionSet().setContentFormat(contentFormat)
                .setObserve(firstCoapResponse.getOptions().getObserve() + 1);
        response.setOptions(options);
        EndpointContext context = new AddressEndpointContext(helper.server.getUnsecuredAddress().getAddress(), helper.server.getUnsecuredAddress().getPort());
        response.setDestinationContext(context);

        // serialize response
        UdpDataSerializer serializer = new UdpDataSerializer();
        RawData data = serializer.serializeResponse(response);

        // send it
        connector.send(data);
    }

    private Connector getConnector(LeshanClient client) {
        CoapEndpoint endpoint = (CoapEndpoint) helper.client.getCoapServer()
                .getEndpoint(helper.client.getUnsecuredAddress());
        return endpoint.getConnector();
    }

    private final class TestObservationListener implements ObservationListener {

        private final CountDownLatch latch = new CountDownLatch(1);
        private final AtomicBoolean receivedNotify = new AtomicBoolean();
        private ObserveResponse response;
        private Exception error;

        @Override
        public void onResponse(Observation observation, Registration registration, ObserveResponse response) {
            receivedNotify.set(true);
            this.response = response;
            this.error = null;
            latch.countDown();
        }

        @Override
        public void onError(Observation observation, Registration registration, Exception error) {
            receivedNotify.set(true);
            this.response = null;
            this.error = error;
            latch.countDown();
        }

        @Override
        public void cancelled(Observation observation) {
            latch.countDown();
        }

        @Override
        public void newObservation(Observation observation, Registration registration) {
        }

        public AtomicBoolean receivedNotify() {
            return receivedNotify;
        }

        public ObserveResponse getResponse() {
            return response;
        }

        public Exception getError() {
            return error;
        }

        public void waitForNotification(long timeout) throws InterruptedException {
            latch.await(timeout, TimeUnit.MILLISECONDS);
        }
    }
}
