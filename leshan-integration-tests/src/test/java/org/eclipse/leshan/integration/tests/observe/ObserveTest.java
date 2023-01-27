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
 *     Achim Kraus (Bosch Software Innovations GmbH) - replace close() with destroy()
 *     Achim Kraus (Bosch Software Innovations GmbH) - use destination context
 *                                                     instead of address for response
 *******************************************************************************/

package org.eclipse.leshan.integration.tests.observe;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.eclipse.californium.core.coap.Response;
import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.model.StaticModel;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResourceInstance;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.node.codec.LwM2mValueChecker;
import org.eclipse.leshan.core.node.codec.json.LwM2mNodeJsonEncoder;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.observation.SingleObservation;
import org.eclipse.leshan.core.request.CancelObservationRequest;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.request.WriteRequest.Mode;
import org.eclipse.leshan.core.response.CancelObservationResponse;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.util.TestLwM2mId;
import org.eclipse.leshan.integration.tests.util.IntegrationTestHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ObserveTest {

    protected IntegrationTestHelper helper = new IntegrationTestHelper();

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
        SingleObservation observation = observeResponse.getObservation();
        assertEquals("/3/0/15", observation.getPath().toString());
        assertEquals(helper.getCurrentRegistration().getId(), observation.getRegistrationId());
        Set<Observation> observations = helper.server.getObservationService()
                .getObservations(helper.getCurrentRegistration());
        assertEquals(1, observations.size(), "We should have only one observation");
        assertTrue(observations.contains(observation), "New observation is not there");

        // write device timezone
        LwM2mResponse writeResponse = helper.server.send(helper.getCurrentRegistration(),
                new WriteRequest(3, 0, 15, "Europe/Paris"));

        // verify result
        listener.waitForNotification(2000);
        assertEquals(ResponseCode.CHANGED, writeResponse.getCode());
        assertTrue(listener.receivedNotify().get());
        assertEquals(LwM2mSingleResource.newStringResource(15, "Europe/Paris"),
                (listener.getObserveResponse()).getContent());
        assertNotNull(listener.getObserveResponse().getCoapResponse());
        assertThat(listener.getObserveResponse().getCoapResponse(), is(instanceOf(Response.class)));
    }

    @Test
    public void can_observe_resource_instance() throws InterruptedException {
        TestObservationListener listener = new TestObservationListener();
        helper.server.getObservationService().addListener(listener);

        // multi instance string
        String expectedPath = "/" + TestLwM2mId.TEST_OBJECT + "/0/" + TestLwM2mId.MULTIPLE_STRING_VALUE + "/0";
        ObserveResponse observeResponse = helper.server.send(helper.getCurrentRegistration(),
                new ObserveRequest(expectedPath));
        assertEquals(ResponseCode.CONTENT, observeResponse.getCode());
        assertNotNull(observeResponse.getCoapResponse());
        assertThat(observeResponse.getCoapResponse(), is(instanceOf(Response.class)));

        // an observation response should have been sent
        SingleObservation observation = observeResponse.getObservation();
        assertEquals(expectedPath, observation.getPath().toString());
        assertEquals(helper.getCurrentRegistration().getId(), observation.getRegistrationId());
        Set<Observation> observations = helper.server.getObservationService()
                .getObservations(helper.getCurrentRegistration());
        assertTrue(observations.size() == 1, "We should have only on observation");
        assertTrue(observations.contains(observation), "New observation is not there");

        // write a new value
        LwM2mResponse writeResponse = helper.server.send(helper.getCurrentRegistration(), new WriteRequest(Mode.REPLACE,
                ContentFormat.TLV, expectedPath, LwM2mResourceInstance.newStringInstance(0, "a new string")));

        // verify result
        listener.waitForNotification(2000);
        assertEquals(ResponseCode.CHANGED, writeResponse.getCode());
        assertTrue(listener.receivedNotify().get());
        assertEquals(LwM2mResourceInstance.newStringInstance(0, "a new string"),
                listener.getObserveResponse().getContent());
        assertNotNull(listener.getObserveResponse().getCoapResponse());
        assertThat(listener.getObserveResponse().getCoapResponse(), is(instanceOf(Response.class)));
    }

    @Test
    public void can_observe_resource_instance_then_passive_cancel() throws InterruptedException {
        TestObservationListener listener = new TestObservationListener();
        helper.server.getObservationService().addListener(listener);

        // multi instance string
        String expectedPath = "/" + TestLwM2mId.TEST_OBJECT + "/0/" + TestLwM2mId.MULTIPLE_STRING_VALUE + "/0";
        ObserveResponse observeResponse = helper.server.send(helper.getCurrentRegistration(),
                new ObserveRequest(expectedPath));
        assertEquals(ResponseCode.CONTENT, observeResponse.getCode());
        assertNotNull(observeResponse.getCoapResponse());
        assertThat(observeResponse.getCoapResponse(), is(instanceOf(Response.class)));

        // an observation response should have been sent
        SingleObservation observation = observeResponse.getObservation();
        assertEquals(expectedPath, observation.getPath().toString());
        assertEquals(helper.getCurrentRegistration().getId(), observation.getRegistrationId());
        Set<Observation> observations = helper.server.getObservationService()
                .getObservations(helper.getCurrentRegistration());
        assertTrue(observations.size() == 1, "We should have only on observation");
        assertTrue(observations.contains(observation), "New observation is not there");

        // write a new value
        LwM2mResponse writeResponse = helper.server.send(helper.getCurrentRegistration(), new WriteRequest(Mode.REPLACE,
                ContentFormat.TLV, expectedPath, LwM2mResourceInstance.newStringInstance(0, "a new string")));

        // verify result
        listener.waitForNotification(2000);
        assertEquals(ResponseCode.CHANGED, writeResponse.getCode());
        assertTrue(listener.receivedNotify().get());
        assertEquals(LwM2mResourceInstance.newStringInstance(0, "a new string"),
                listener.getObserveResponse().getContent());
        assertNotNull(listener.getObserveResponse().getCoapResponse());

        // cancel observation : passive way
        helper.server.getObservationService().cancelObservation(observation);
        observations = helper.server.getObservationService().getObservations(helper.getCurrentRegistration());
        assertTrue(observations.isEmpty(), "Observation should be removed");

        // write device timezone
        listener.reset();
        writeResponse = helper.server.send(helper.getCurrentRegistration(), new WriteRequest(Mode.REPLACE,
                ContentFormat.TLV, expectedPath, LwM2mResourceInstance.newStringInstance(0, "a another new string")));

        // verify result
        listener.waitForNotification(1000);
        assertEquals(ResponseCode.CHANGED, writeResponse.getCode());
        assertFalse(listener.receivedNotify().get(), "Observation should be cancelled");
    }

    @Test
    public void can_observe_resource_instance_then_active_cancel() throws InterruptedException {
        TestObservationListener listener = new TestObservationListener();
        helper.server.getObservationService().addListener(listener);

        // multi instance string
        String expectedPath = "/" + TestLwM2mId.TEST_OBJECT + "/0/" + TestLwM2mId.MULTIPLE_STRING_VALUE + "/0";
        ObserveResponse observeResponse = helper.server.send(helper.getCurrentRegistration(),
                new ObserveRequest(expectedPath));
        assertEquals(ResponseCode.CONTENT, observeResponse.getCode());
        assertNotNull(observeResponse.getCoapResponse());
        assertThat(observeResponse.getCoapResponse(), is(instanceOf(Response.class)));

        // an observation response should have been sent
        SingleObservation observation = observeResponse.getObservation();
        assertEquals(expectedPath, observation.getPath().toString());
        assertEquals(helper.getCurrentRegistration().getId(), observation.getRegistrationId());
        Set<Observation> observations = helper.server.getObservationService()
                .getObservations(helper.getCurrentRegistration());
        assertTrue(observations.size() == 1, "We should have only on observation");
        assertTrue(observations.contains(observation), "New observation is not there");

        // write a new value
        LwM2mResponse writeResponse = helper.server.send(helper.getCurrentRegistration(), new WriteRequest(Mode.REPLACE,
                ContentFormat.TLV, expectedPath, LwM2mResourceInstance.newStringInstance(0, "a new string")));

        // verify result
        listener.waitForNotification(2000);
        assertEquals(ResponseCode.CHANGED, writeResponse.getCode());
        assertTrue(listener.receivedNotify().get());
        assertEquals(LwM2mResourceInstance.newStringInstance(0, "a new string"),
                listener.getObserveResponse().getContent());
        assertNotNull(listener.getObserveResponse().getCoapResponse());

        // cancel observation : active way
        CancelObservationResponse response = helper.server.send(helper.getCurrentRegistration(),
                new CancelObservationRequest(observation));
        assertTrue(response.isSuccess());
        assertEquals(ResponseCode.CONTENT, response.getCode());
        assertEquals("a new string", ((LwM2mResourceInstance) response.getContent()).getValue());
        // active cancellation does not remove observation from store : it should be done manually using
        // ObservationService().cancelObservation(observation)
        observations = helper.server.getObservationService().getObservations(helper.getCurrentRegistration());
        assertTrue(observations.size() == 1, "We should have only on observation");
        assertTrue(observations.contains(observation), "Observation should still be there");

        // write device timezone
        listener.reset();
        writeResponse = helper.server.send(helper.getCurrentRegistration(), new WriteRequest(Mode.REPLACE,
                ContentFormat.TLV, expectedPath, LwM2mResourceInstance.newStringInstance(0, "a another new string")));

        // verify result
        listener.waitForNotification(1000);
        assertEquals(ResponseCode.CHANGED, writeResponse.getCode());
        assertFalse(listener.receivedNotify().get(), "Observation should be cancelled");
    }

    @Test
    public void can_observe_resource_then_passive_cancel() throws InterruptedException {
        TestObservationListener listener = new TestObservationListener();
        helper.server.getObservationService().addListener(listener);

        // observe device timezone
        ObserveResponse observeResponse = helper.server.send(helper.getCurrentRegistration(),
                new ObserveRequest(3, 0, 15));
        assertEquals(ResponseCode.CONTENT, observeResponse.getCode());
        assertNotNull(observeResponse.getCoapResponse());
        assertThat(observeResponse.getCoapResponse(), is(instanceOf(Response.class)));

        // an observation response should have been sent
        SingleObservation observation = observeResponse.getObservation();
        assertEquals("/3/0/15", observation.getPath().toString());
        assertEquals(helper.getCurrentRegistration().getId(), observation.getRegistrationId());
        Set<Observation> observations = helper.server.getObservationService()
                .getObservations(helper.getCurrentRegistration());
        assertEquals(1, observations.size(), "We should have only one observation");
        assertTrue(observations.contains(observation), "New observation is not there");

        // write device timezone
        LwM2mResponse writeResponse = helper.server.send(helper.getCurrentRegistration(),
                new WriteRequest(3, 0, 15, "Europe/Paris"));

        // verify result
        listener.waitForNotification(2000);
        assertEquals(ResponseCode.CHANGED, writeResponse.getCode());
        assertTrue(listener.receivedNotify().get());
        assertEquals(LwM2mSingleResource.newStringResource(15, "Europe/Paris"),
                listener.getObserveResponse().getContent());
        assertNotNull(listener.getObserveResponse().getCoapResponse());
        assertThat(listener.getObserveResponse().getCoapResponse(), is(instanceOf(Response.class)));

        // cancel observation : passive way
        helper.server.getObservationService().cancelObservation(observation);
        observations = helper.server.getObservationService().getObservations(helper.getCurrentRegistration());
        assertTrue(observations.isEmpty(), "Observation should be removed");

        // write device timezone
        listener.reset();
        writeResponse = helper.server.send(helper.getCurrentRegistration(),
                new WriteRequest(3, 0, 15, "Europe/London"));

        // verify result
        listener.waitForNotification(1000);
        assertEquals(ResponseCode.CHANGED, writeResponse.getCode());
        assertFalse(listener.receivedNotify().get(), "Observation should be cancelled");
    }

    @Test
    public void can_observe_resource_then_active_cancel() throws InterruptedException {
        TestObservationListener listener = new TestObservationListener();
        helper.server.getObservationService().addListener(listener);

        // observe device timezone
        ObserveResponse observeResponse = helper.server.send(helper.getCurrentRegistration(),
                new ObserveRequest(3, 0, 15));
        assertEquals(ResponseCode.CONTENT, observeResponse.getCode());
        assertNotNull(observeResponse.getCoapResponse());
        assertThat(observeResponse.getCoapResponse(), is(instanceOf(Response.class)));

        // an observation response should have been sent
        SingleObservation observation = observeResponse.getObservation();
        assertEquals("/3/0/15", observation.getPath().toString());
        assertEquals(helper.getCurrentRegistration().getId(), observation.getRegistrationId());
        Set<Observation> observations = helper.server.getObservationService()
                .getObservations(helper.getCurrentRegistration());
        assertEquals(1, observations.size(), "We should have only one observation");
        assertTrue(observations.contains(observation), "New observation is not there");

        // write device timezone
        LwM2mResponse writeResponse = helper.server.send(helper.getCurrentRegistration(),
                new WriteRequest(3, 0, 15, "Europe/Paris"));

        // verify result
        listener.waitForNotification(2000);
        assertEquals(ResponseCode.CHANGED, writeResponse.getCode());
        assertTrue(listener.receivedNotify().get());
        assertEquals(LwM2mSingleResource.newStringResource(15, "Europe/Paris"),
                listener.getObserveResponse().getContent());
        assertNotNull(listener.getObserveResponse().getCoapResponse());
        assertThat(listener.getObserveResponse().getCoapResponse(), is(instanceOf(Response.class)));

        // cancel observation : active way
        CancelObservationResponse response = helper.server.send(helper.getCurrentRegistration(),
                new CancelObservationRequest(observation));
        assertTrue(response.isSuccess());
        assertEquals(ResponseCode.CONTENT, response.getCode());
        assertEquals("Europe/Paris", ((LwM2mSingleResource) response.getContent()).getValue());
        // active cancellation does not remove observation from store : it should be done manually using
        // ObservationService().cancelObservation(observation)
        observations = helper.server.getObservationService().getObservations(helper.getCurrentRegistration());
        assertEquals(1, observations.size(), "We should have only one observation");
        assertTrue(observations.contains(observation), "Observation should still be there");

        // write device timezone
        listener.reset();
        writeResponse = helper.server.send(helper.getCurrentRegistration(),
                new WriteRequest(3, 0, 15, "Europe/London"));

        // verify result
        listener.waitForNotification(1000);
        assertEquals(ResponseCode.CHANGED, writeResponse.getCode());
        assertFalse(listener.receivedNotify().get(), "Observation should be cancelled");
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
        SingleObservation observation = observeResponse.getObservation();
        assertEquals("/3/0", observation.getPath().toString());
        assertEquals(helper.getCurrentRegistration().getId(), observation.getRegistrationId());
        Set<Observation> observations = helper.server.getObservationService()
                .getObservations(helper.getCurrentRegistration());
        assertEquals(1, observations.size(), "We should have only one observation");
        assertTrue(observations.contains(observation), "New observation is not there");

        // write device timezone
        LwM2mResponse writeResponse = helper.server.send(helper.getCurrentRegistration(),
                new WriteRequest(3, 0, 15, "Europe/Paris"));

        // verify result
        listener.waitForNotification(2000);
        assertEquals(ResponseCode.CHANGED, writeResponse.getCode());
        assertTrue(listener.receivedNotify().get());
        assertTrue(listener.getObserveResponse().getContent() instanceof LwM2mObjectInstance);
        assertNotNull(listener.getObserveResponse().getCoapResponse());
        assertThat(listener.getObserveResponse().getCoapResponse(), is(instanceOf(Response.class)));

        // try to read the object instance for comparing
        ReadResponse readResp = helper.server.send(helper.getCurrentRegistration(), new ReadRequest(3, 0));

        assertEquals(readResp.getContent(), listener.getObserveResponse().getContent());
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
        SingleObservation observation = observeResponse.getObservation();
        assertEquals("/3", observation.getPath().toString());
        assertEquals(helper.getCurrentRegistration().getId(), observation.getRegistrationId());
        Set<Observation> observations = helper.server.getObservationService()
                .getObservations(helper.getCurrentRegistration());
        assertEquals(1, observations.size(), "We should have only one observation");
        assertTrue(observations.contains(observation), "New observation is not there");

        // write device timezone
        LwM2mResponse writeResponse = helper.server.send(helper.getCurrentRegistration(),
                new WriteRequest(3, 0, 15, "Europe/Paris"));

        // verify result
        listener.waitForNotification(2000);
        assertEquals(ResponseCode.CHANGED, writeResponse.getCode());
        assertTrue(listener.receivedNotify().get());
        assertTrue(listener.getObserveResponse().getContent() instanceof LwM2mObject);
        assertNotNull(listener.getObserveResponse().getCoapResponse());
        assertThat(listener.getObserveResponse().getCoapResponse(), is(instanceOf(Response.class)));

        // try to read the object for comparing
        ReadResponse readResp = helper.server.send(helper.getCurrentRegistration(), new ReadRequest(3));

        assertEquals(readResp.getContent(), listener.getObserveResponse().getContent());
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
        SingleObservation observation = observeResponse.getObservation();
        assertEquals("/3/0/15", observation.getPath().toString());
        assertEquals(helper.getCurrentRegistration().getId(), observation.getRegistrationId());
        Set<Observation> observations = helper.server.getObservationService()
                .getObservations(helper.getCurrentRegistration());
        assertEquals(1, observations.size(), "We should have only one observation");
        assertTrue(observations.contains(observation), "New observation is not there");

        // *** HACK send a notification with unsupported content format *** //
        byte[] payload = new LwM2mNodeJsonEncoder().encode(LwM2mSingleResource.newStringResource(15, "Paris"),
                new LwM2mPath("/3/0/15"), new StaticModel(helper.createObjectModels()), new LwM2mValueChecker());
        Response firstCoapResponse = (Response) observeResponse.getCoapResponse();
        // 666 is not a supported content format.
        TestObserveUtil.sendNotification(helper.getClientConnector(helper.getCurrentRegisteredServer()),
                helper.server.getEndpoint(Protocol.COAP).getURI(), payload, firstCoapResponse,
                ContentFormat.fromCode(666));
        // *** Hack End *** //

        // verify result
        listener.waitForNotification(2000);
        assertTrue(listener.receivedNotify().get());
        assertNotNull(listener.getError());
    }

}
