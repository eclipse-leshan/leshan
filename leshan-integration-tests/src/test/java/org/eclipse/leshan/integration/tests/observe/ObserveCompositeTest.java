/*******************************************************************************
 * Copyright (c) 2021 Orange.
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
 *     Michał Wadowski (Orange) - Add Observe-Composite feature.
 *******************************************************************************/
package org.eclipse.leshan.integration.tests.observe;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.californium.core.coap.Response;
import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.observation.CompositeObservation;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.request.CancelCompositeObservationRequest;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.ObserveCompositeRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.WriteCompositeRequest;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.response.CancelCompositeObservationResponse;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ObserveCompositeResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteCompositeResponse;
import org.eclipse.leshan.integration.tests.util.IntegrationTestHelper;
import org.eclipse.leshan.server.registration.Registration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ObserveCompositeTest {

    protected IntegrationTestHelper helper = new IntegrationTestHelper();
    private Registration currentRegistration;
    private TestObservationListener listener;

    @BeforeEach
    public void start() {
        helper.initialize();
        helper.createServer();
        helper.server.start();
        helper.createClient();
        helper.client.start();
        helper.waitForRegistrationAtServerSide(1);

        currentRegistration = helper.getCurrentRegistration();
        listener = new TestObservationListener();
        helper.server.getObservationService().addListener(listener);
    }

    @AfterEach
    public void stop() {
        helper.client.destroy(false);
        helper.server.destroy();
        helper.dispose();
    }

    @Test
    public void can_composite_observe_on_single_resource() throws InterruptedException {
        // Send ObserveCompositeRequest
        ObserveCompositeResponse observeResponse = helper.server.send(currentRegistration,
                new ObserveCompositeRequest(ContentFormat.SENML_JSON, ContentFormat.SENML_JSON, "/3/0/15"));

        // Assert that ObserveCompositeResponse is valid
        assertEquals(ResponseCode.CONTENT, observeResponse.getCode());
        assertNotNull(observeResponse.getCoapResponse());
        assertThat(observeResponse.getCoapResponse(), is(instanceOf(Response.class)));

        CompositeObservation observation = observeResponse.getObservation();

        // Assert that CompositeObservation contains expected paths
        assertNotNull(observation);
        assertEquals(1, observation.getPaths().size());
        assertEquals("/3/0/15", observation.getPaths().get(0).toString());

        // Assert that there is one valid observation
        assertEquals(helper.getCurrentRegistration().getId(), observation.getRegistrationId());
        Set<Observation> observations = helper.server.getObservationService()
                .getObservations(helper.getCurrentRegistration());
        assertEquals(1, observations.size(), "We should have only one observation");
        assertTrue(observations.contains(observation), "New observation is not there");

        // Write single example value
        LwM2mResponse writeResponse = helper.server.send(helper.getCurrentRegistration(),
                new WriteRequest(3, 0, 15, "Europe/Paris"));
        listener.waitForNotification(2000);
        assertEquals(ResponseCode.CHANGED, writeResponse.getCode());

        // Assert that response contains expected paths
        assertTrue(listener.receivedNotify().get());
        Map<LwM2mPath, LwM2mNode> content = listener.getObserveCompositeResponse().getContent();
        assertEquals(1, content.size());
        assertTrue(content.containsKey(new LwM2mPath("/3/0/15")));

        // Assert that listener response contains expected values
        assertEquals(LwM2mSingleResource.newStringResource(15, "Europe/Paris"), content.get(new LwM2mPath("/3/0/15")));

        // Assert that listener has Response
        assertNotNull(listener.getObserveCompositeResponse().getCoapResponse());
        assertThat(listener.getObserveCompositeResponse().getCoapResponse(), is(instanceOf(Response.class)));
    }

    @Test
    public void should_not_get_response_if_modified_other_resource_than_observed() throws InterruptedException {
        // Send ObserveCompositeRequest
        ObserveCompositeResponse observeResponse = helper.server.send(currentRegistration,
                new ObserveCompositeRequest(ContentFormat.SENML_JSON, ContentFormat.SENML_JSON, "/3/0/14"));

        // Assert that ObserveCompositeResponse is valid
        assertEquals(ResponseCode.CONTENT, observeResponse.getCode());
        assertNotNull(observeResponse.getCoapResponse());
        assertThat(observeResponse.getCoapResponse(), is(instanceOf(Response.class)));

        CompositeObservation observation = observeResponse.getObservation();

        // Assert that CompositeObservation contains expected paths
        assertNotNull(observation);
        assertEquals(1, observation.getPaths().size());
        assertEquals("/3/0/14", observation.getPaths().get(0).toString());

        // Assert that there is one valid observation
        assertEquals(helper.getCurrentRegistration().getId(), observation.getRegistrationId());
        Set<Observation> observations = helper.server.getObservationService()
                .getObservations(helper.getCurrentRegistration());
        assertEquals(1, observations.size(), "We should have only one observation");
        assertTrue(observations.contains(observation), "New observation is not there");

        // Write single example value
        LwM2mResponse writeResponse = helper.server.send(helper.getCurrentRegistration(),
                new WriteRequest(3, 0, 15, "Europe/Paris"));
        listener.waitForNotification(2000);
        assertEquals(ResponseCode.CHANGED, writeResponse.getCode());

        // Assert that listener has no response
        assertFalse(listener.receivedNotify().get());
    }

    @Test
    public void can_composite_observe_on_multiple_resources() throws InterruptedException {
        // Send ObserveCompositeRequest
        ObserveCompositeResponse observeResponse = helper.server.send(currentRegistration,
                new ObserveCompositeRequest(ContentFormat.SENML_JSON, ContentFormat.SENML_JSON, "/3/0/15", "/3/0/14"));

        // Assert that ObserveCompositeResponse is valid
        assertEquals(ResponseCode.CONTENT, observeResponse.getCode());
        assertNotNull(observeResponse.getCoapResponse());
        assertThat(observeResponse.getCoapResponse(), is(instanceOf(Response.class)));

        LwM2mNode previousOffset = observeResponse.getContent("/3/0/14");

        CompositeObservation observation = observeResponse.getObservation();

        // Assert that CompositeObservation contains expected paths
        assertNotNull(observation);
        assertEquals(2, observation.getPaths().size());
        assertEquals("/3/0/15", observation.getPaths().get(0).toString());
        assertEquals("/3/0/14", observation.getPaths().get(1).toString());

        // Assert that there is one valid observation
        assertEquals(helper.getCurrentRegistration().getId(), observation.getRegistrationId());
        Set<Observation> observations = helper.server.getObservationService()
                .getObservations(helper.getCurrentRegistration());
        assertEquals(1, observations.size(), "We should have only one observation");
        assertTrue(observations.contains(observation), "New observation is not there");

        // Write single example value
        LwM2mResponse writeResponse = helper.server.send(helper.getCurrentRegistration(),
                new WriteRequest(3, 0, 15, "Europe/Paris"));
        listener.waitForNotification(2000);
        assertEquals(ResponseCode.CHANGED, writeResponse.getCode());

        // Assert that response contains exactly the same paths
        assertTrue(listener.receivedNotify().get());
        Map<LwM2mPath, LwM2mNode> content = listener.getObserveCompositeResponse().getContent();
        assertEquals(2, content.size());
        assertTrue(content.containsKey(new LwM2mPath("/3/0/15")));
        assertTrue(content.containsKey(new LwM2mPath("/3/0/14")));

        // Assert that listener response contains expected values
        assertEquals(LwM2mSingleResource.newStringResource(new LwM2mPath("/3/0/15").getResourceId(), "Europe/Paris"),
                content.get(new LwM2mPath("/3/0/15")));
        assertEquals(previousOffset, content.get(new LwM2mPath("/3/0/14")));

        // Assert that listener has Response
        assertNotNull(listener.getObserveCompositeResponse().getCoapResponse());
        assertThat(listener.getObserveCompositeResponse().getCoapResponse(), is(instanceOf(Response.class)));
    }

    @Test
    public void can_composite_observe_on_multiple_resources_with_write_composite() throws InterruptedException {
        // Send ObserveCompositeRequest
        ObserveCompositeResponse observeResponse = helper.server.send(currentRegistration,
                new ObserveCompositeRequest(ContentFormat.SENML_JSON, ContentFormat.SENML_JSON, "/3/0/15", "/3/0/14"));

        // Assert that ObserveCompositeResponse is valid
        assertEquals(ResponseCode.CONTENT, observeResponse.getCode());
        assertNotNull(observeResponse.getCoapResponse());
        assertThat(observeResponse.getCoapResponse(), is(instanceOf(Response.class)));

        CompositeObservation observation = observeResponse.getObservation();

        // Assert that CompositeObservation contains expected paths
        assertNotNull(observation);
        assertEquals(2, observation.getPaths().size());
        assertEquals("/3/0/15", observation.getPaths().get(0).toString());
        assertEquals("/3/0/14", observation.getPaths().get(1).toString());

        // Assert that there is one valid observation
        assertEquals(helper.getCurrentRegistration().getId(), observation.getRegistrationId());
        Set<Observation> observations = helper.server.getObservationService()
                .getObservations(helper.getCurrentRegistration());
        assertEquals(1, observations.size(), "We should have only one observation");
        assertTrue(observations.contains(observation), "New observation is not there");

        // Write example composite values
        Map<String, Object> nodes = new HashMap<>();
        nodes.put("/3/0/15", "Europe/Paris");
        nodes.put("/3/0/14", "+11");
        WriteCompositeResponse writeResponse = helper.server.send(helper.getCurrentRegistration(),
                new WriteCompositeRequest(ContentFormat.SENML_JSON, nodes));
        listener.waitForNotification(2000);
        assertEquals(ResponseCode.CHANGED, writeResponse.getCode());

        // Assert that response contains expected paths
        assertTrue(listener.receivedNotify().get());
        Map<LwM2mPath, LwM2mNode> content = listener.getObserveCompositeResponse().getContent();
        assertEquals(2, content.size());
        assertTrue(content.containsKey(new LwM2mPath("/3/0/15")));
        assertTrue(content.containsKey(new LwM2mPath("/3/0/14")));

        // Assert that listener response contains expected values
        assertEquals(LwM2mSingleResource.newStringResource(new LwM2mPath("/3/0/15").getResourceId(), "Europe/Paris"),
                content.get(new LwM2mPath("/3/0/15")));
        assertEquals(LwM2mSingleResource.newStringResource(new LwM2mPath("/3/0/14").getResourceId(), "+11"),
                content.get(new LwM2mPath("/3/0/14")));

        // Assert that listener has Response
        assertNotNull(listener.getObserveCompositeResponse().getCoapResponse());
        assertThat(listener.getObserveCompositeResponse().getCoapResponse(), is(instanceOf(Response.class)));
    }

    @Test
    public void can_observe_instance() throws InterruptedException {
        // Send ObserveCompositeRequest
        ObserveCompositeResponse observeResponse = helper.server.send(currentRegistration,
                new ObserveCompositeRequest(ContentFormat.SENML_JSON, ContentFormat.SENML_JSON, "/3/0"));

        // Assert that ObserveCompositeResponse is valid
        assertEquals(ResponseCode.CONTENT, observeResponse.getCode());
        assertNotNull(observeResponse.getCoapResponse());
        assertThat(observeResponse.getCoapResponse(), is(instanceOf(Response.class)));

        CompositeObservation observation = observeResponse.getObservation();

        // Assert that CompositeObservation contains expected paths
        assertNotNull(observation);
        assertEquals(1, observation.getPaths().size());
        assertEquals("/3/0", observation.getPaths().get(0).toString());

        // Assert that there is one valid observation
        assertEquals(helper.getCurrentRegistration().getId(), observation.getRegistrationId());
        Set<Observation> observations = helper.server.getObservationService()
                .getObservations(helper.getCurrentRegistration());
        assertEquals(1, observations.size(), "We should have only one observation");
        assertTrue(observations.contains(observation), "New observation is not there");

        // Write single example value
        LwM2mResponse writeResponse = helper.server.send(helper.getCurrentRegistration(),
                new WriteRequest(3, 0, 15, "Europe/Paris"));
        listener.waitForNotification(2000);
        assertEquals(ResponseCode.CHANGED, writeResponse.getCode());

        // Assert that response contains expected paths
        assertTrue(listener.receivedNotify().get());
        Map<LwM2mPath, LwM2mNode> content = listener.getObserveCompositeResponse().getContent();
        assertEquals(1, content.size());
        assertTrue(content.containsKey(new LwM2mPath("/3/0")));

        // Assert that listener response equals to ReadResponse
        ReadResponse readResp = helper.server.send(helper.getCurrentRegistration(),
                new ReadRequest(ContentFormat.SENML_JSON, "/3/0"));
        assertEquals(readResp.getContent(), content.get(new LwM2mPath("/3/0")));
    }

    @Test
    public void can_observe_object() throws InterruptedException {
        // Send ObserveCompositeRequest
        ObserveCompositeResponse observeResponse = helper.server.send(currentRegistration,
                new ObserveCompositeRequest(ContentFormat.SENML_JSON, ContentFormat.SENML_JSON, "/3"));

        // Assert that ObserveCompositeResponse is valid
        assertEquals(ResponseCode.CONTENT, observeResponse.getCode());
        assertNotNull(observeResponse.getCoapResponse());
        assertThat(observeResponse.getCoapResponse(), is(instanceOf(Response.class)));

        CompositeObservation observation = observeResponse.getObservation();

        // Assert that CompositeObservation contains expected paths
        assertNotNull(observation);
        assertEquals(1, observation.getPaths().size());
        assertEquals("/3", observation.getPaths().get(0).toString());

        // Assert that there is one valid observation
        assertEquals(helper.getCurrentRegistration().getId(), observation.getRegistrationId());
        Set<Observation> observations = helper.server.getObservationService()
                .getObservations(helper.getCurrentRegistration());
        assertEquals(1, observations.size(), "We should have only one observation");
        assertTrue(observations.contains(observation), "New observation is not there");

        // Write single example value
        LwM2mResponse writeResponse = helper.server.send(helper.getCurrentRegistration(),
                new WriteRequest(3, 0, 15, "Europe/Paris"));
        listener.waitForNotification(2000);
        assertEquals(ResponseCode.CHANGED, writeResponse.getCode());

        // Assert that response contains expected paths
        assertTrue(listener.receivedNotify().get());
        Map<LwM2mPath, LwM2mNode> content = listener.getObserveCompositeResponse().getContent();
        assertEquals(1, content.size());
        assertTrue(content.containsKey(new LwM2mPath("/3")));

        // Assert that listener response equals to ReadResponse
        ReadResponse readResp = helper.server.send(helper.getCurrentRegistration(),
                new ReadRequest(ContentFormat.SENML_JSON, "/3"));
        assertEquals(readResp.getContent(), content.get(new LwM2mPath("/3")));
    }

    @Test
    public void can_passive_cancel_composite_observation() throws InterruptedException {
        // Send ObserveCompositeRequest
        ObserveCompositeResponse observeCompositeResponse = helper.server.send(currentRegistration,
                new ObserveCompositeRequest(ContentFormat.SENML_JSON, ContentFormat.SENML_JSON, "/3/0/15"));

        CompositeObservation observation = observeCompositeResponse.getObservation();

        // Write single example value
        LwM2mResponse writeResponse = helper.server.send(helper.getCurrentRegistration(),
                new WriteRequest(3, 0, 15, "Europe/Paris"));
        listener.waitForNotification(2000);
        assertEquals(ResponseCode.CHANGED, writeResponse.getCode());

        // cancel observation : passive way
        helper.server.getObservationService().cancelObservation(observation);
        Set<Observation> observations = helper.server.getObservationService()
                .getObservations(helper.getCurrentRegistration());
        assertTrue(observations.isEmpty(), "Observation should be removed");

        // write device timezone
        listener.reset();

        // Write single value
        writeResponse = helper.server.send(helper.getCurrentRegistration(),
                new WriteRequest(3, 0, 15, "Europe/London"));
        listener.waitForNotification(2000);
        assertEquals(ResponseCode.CHANGED, writeResponse.getCode());

        assertFalse(listener.receivedNotify().get(), "Observation should be cancelled");
    }

    @Test
    public void can_active_cancel_composite_observation() throws InterruptedException {
        // Send ObserveCompositeRequest
        ObserveCompositeResponse observeCompositeResponse = helper.server.send(currentRegistration,
                new ObserveCompositeRequest(ContentFormat.SENML_JSON, ContentFormat.SENML_JSON, "/3/0/15"));

        CompositeObservation observation = observeCompositeResponse.getObservation();

        // Write single example value
        LwM2mResponse writeResponse = helper.server.send(helper.getCurrentRegistration(),
                new WriteRequest(3, 0, 15, "Europe/Paris"));
        listener.waitForNotification(2000);
        assertEquals(ResponseCode.CHANGED, writeResponse.getCode());

        // cancel observation : active way
        CancelCompositeObservationResponse response = helper.server.send(helper.getCurrentRegistration(),
                new CancelCompositeObservationRequest(observation));
        assertTrue(response.isSuccess());
        assertEquals(ResponseCode.CONTENT, response.getCode());

        // Assert that response contains exactly the same paths
        assertTrue(listener.receivedNotify().get());
        Map<LwM2mPath, LwM2mNode> content = listener.getObserveCompositeResponse().getContent();
        assertEquals(1, content.size());
        assertTrue(content.containsKey(new LwM2mPath("/3/0/15")));

        // Assert that listener response contains exact values
        assertEquals(LwM2mSingleResource.newStringResource(new LwM2mPath("/3/0/15").getResourceId(), "Europe/Paris"),
                content.get(new LwM2mPath("/3/0/15")));

        // active cancellation does not remove observation from store : it should be done manually using
        // ObservationService().cancelObservation(observation)

        // Assert that there is one valid observation
        assertEquals(helper.getCurrentRegistration().getId(), observation.getRegistrationId());
        Set<Observation> observations = helper.server.getObservationService()
                .getObservations(helper.getCurrentRegistration());
        assertEquals(1, observations.size(), "We should have only one observation");
        assertTrue(observations.contains(observation), "New observation is not there");

        // Write device timezone
        listener.reset();

        writeResponse = helper.server.send(helper.getCurrentRegistration(),
                new WriteRequest(3, 0, 15, "Europe/London"));
        listener.waitForNotification(2000);
        assertEquals(ResponseCode.CHANGED, writeResponse.getCode());

        assertFalse(listener.receivedNotify().get(), "Observation should be cancelled");
    }

}
