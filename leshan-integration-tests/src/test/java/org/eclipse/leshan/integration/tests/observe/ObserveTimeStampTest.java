/*******************************************************************************
 * Copyright (c) 2020 Sierra Wireless and others.
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
package org.eclipse.leshan.integration.tests.observe;

import static org.eclipse.leshan.integration.tests.util.TestUtil.assertContentFormat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.eclipse.californium.core.coap.Response;
import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.model.StaticModel;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.node.TimestampedLwM2mNode;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mEncoder;
import org.eclipse.leshan.core.node.codec.LwM2mEncoder;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.observation.SingleObservation;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.integration.tests.util.IntegrationTestHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ObserveTimeStampTest {

    @Parameters(name = "{0}")
    public static Collection<?> contentFormats() {
        return Arrays.asList(new Object[][] { //
                { ContentFormat.JSON }, //
                { ContentFormat.SENML_JSON }, //
                { ContentFormat.SENML_CBOR } });
    }

    protected IntegrationTestHelper helper = new IntegrationTestHelper();

    private final ContentFormat contentFormat;
    private final LwM2mEncoder encoder = new DefaultLwM2mEncoder();

    public ObserveTimeStampTest(ContentFormat contentFormat) {
        this.contentFormat = contentFormat;
    }

    @Before
    public void start() {
        helper.initialize();
        helper.createServer();
        helper.server.start();
        helper.createClient();
        helper.client.start();
        helper.waitForRegistrationAtServerSide(1);
    }

    @After
    public void stop() {
        helper.client.destroy(false);
        helper.server.destroy();
        helper.dispose();
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
        SingleObservation observation = observeResponse.getObservation();
        assertEquals("/3/0/15", observation.getPath().toString());
        assertEquals(helper.getCurrentRegistration().getId(), observation.getRegistrationId());
        Set<Observation> observations = helper.server.getObservationService()
                .getObservations(helper.getCurrentRegistration());
        assertTrue("We should have only one observation", observations.size() == 1);
        assertTrue("New observation is not there", observations.contains(observation));

        // *** HACK send time-stamped notification as Leshan client does not support it *** //
        // create time-stamped nodes
        TimestampedLwM2mNode mostRecentNode = new TimestampedLwM2mNode(Instant.ofEpochMilli(System.currentTimeMillis()),
                LwM2mSingleResource.newStringResource(15, "Paris"));
        List<TimestampedLwM2mNode> timestampedNodes = new ArrayList<>();
        timestampedNodes.add(mostRecentNode);
        timestampedNodes.add(new TimestampedLwM2mNode(mostRecentNode.getTimestamp().minusMillis(2),
                LwM2mSingleResource.newStringResource(15, "Londres")));
        byte[] payload = encoder.encodeTimestampedData(timestampedNodes, contentFormat, new LwM2mPath("/3/0/15"),
                new StaticModel(helper.createObjectModels()));
        Response firstCoapResponse = (Response) observeResponse.getCoapResponse();
        TestObserveUtil.sendNotification(helper.getClientConnector(helper.getCurrentRegisteredServer()),
                helper.server.getEndpoint(Protocol.COAP).getURI(), payload, firstCoapResponse, contentFormat);
        // *** Hack End *** //

        // verify result
        listener.waitForNotification(2000);
        assertTrue(listener.receivedNotify().get());
        assertEquals(mostRecentNode.getNode(), listener.getObserveResponse().getContent());
        assertEquals(timestampedNodes, listener.getObserveResponse().getTimestampedLwM2mNode());
        assertContentFormat(contentFormat, listener.getObserveResponse());
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
        SingleObservation observation = observeResponse.getObservation();
        assertEquals("/3/0", observation.getPath().toString());
        assertEquals(helper.getCurrentRegistration().getId(), observation.getRegistrationId());
        Set<Observation> observations = helper.server.getObservationService()
                .getObservations(helper.getCurrentRegistration());
        assertTrue("We should have only one observation", observations.size() == 1);
        assertTrue("New observation is not there", observations.contains(observation));

        // *** HACK send time-stamped notification as Leshan client does not support it *** //
        // create time-stamped nodes
        TimestampedLwM2mNode mostRecentNode = new TimestampedLwM2mNode(Instant.ofEpochMilli(System.currentTimeMillis()),
                new LwM2mObjectInstance(0, LwM2mSingleResource.newStringResource(15, "Paris")));
        List<TimestampedLwM2mNode> timestampedNodes = new ArrayList<>();
        timestampedNodes.add(mostRecentNode);
        timestampedNodes.add(new TimestampedLwM2mNode(mostRecentNode.getTimestamp().minusMillis(2),
                new LwM2mObjectInstance(0, LwM2mSingleResource.newStringResource(15, "Londres"))));
        byte[] payload = encoder.encodeTimestampedData(timestampedNodes, contentFormat, new LwM2mPath("/3/0"),
                new StaticModel(helper.createObjectModels()));
        Response firstCoapResponse = (Response) observeResponse.getCoapResponse();
        TestObserveUtil.sendNotification(helper.getClientConnector(helper.getCurrentRegisteredServer()),
                helper.server.getEndpoint(Protocol.COAP).getURI(), payload, firstCoapResponse, contentFormat);
        // *** Hack End *** //

        // verify result
        listener.waitForNotification(2000);
        assertTrue(listener.receivedNotify().get());
        assertEquals(mostRecentNode.getNode(), listener.getObserveResponse().getContent());
        assertEquals(timestampedNodes, listener.getObserveResponse().getTimestampedLwM2mNode());
        assertContentFormat(contentFormat, listener.getObserveResponse());
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
        SingleObservation observation = observeResponse.getObservation();
        assertEquals("/3", observation.getPath().toString());
        assertEquals(helper.getCurrentRegistration().getId(), observation.getRegistrationId());
        Set<Observation> observations = helper.server.getObservationService()
                .getObservations(helper.getCurrentRegistration());
        assertTrue("We should have only one observation", observations.size() == 1);
        assertTrue("New observation is not there", observations.contains(observation));

        // *** HACK send time-stamped notification as Leshan client does not support it *** //
        // create time-stamped nodes
        TimestampedLwM2mNode mostRecentNode = new TimestampedLwM2mNode(Instant.ofEpochMilli(System.currentTimeMillis()),
                new LwM2mObject(3, new LwM2mObjectInstance(0, LwM2mSingleResource.newStringResource(15, "Paris"))));
        List<TimestampedLwM2mNode> timestampedNodes = new ArrayList<>();
        timestampedNodes.add(mostRecentNode);
        timestampedNodes.add(new TimestampedLwM2mNode(mostRecentNode.getTimestamp().minusMillis(2),
                new LwM2mObject(3, new LwM2mObjectInstance(0, LwM2mSingleResource.newStringResource(15, "Londres")))));
        byte[] payload = encoder.encodeTimestampedData(timestampedNodes, contentFormat, new LwM2mPath("/3"),
                new StaticModel(helper.createObjectModels()));

        Response firstCoapResponse = (Response) observeResponse.getCoapResponse();
        TestObserveUtil.sendNotification(helper.getClientConnector(helper.getCurrentRegisteredServer()),
                helper.server.getEndpoint(Protocol.COAP).getURI(), payload, firstCoapResponse, contentFormat);
        // *** Hack End *** //

        // verify result
        listener.waitForNotification(2000);
        assertTrue(listener.receivedNotify().get());
        assertEquals(mostRecentNode.getNode(), listener.getObserveResponse().getContent());
        assertEquals(timestampedNodes, listener.getObserveResponse().getTimestampedLwM2mNode());
        assertContentFormat(contentFormat, listener.getObserveResponse());
    }
}
