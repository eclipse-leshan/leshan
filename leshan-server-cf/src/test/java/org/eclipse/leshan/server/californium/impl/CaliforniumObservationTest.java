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
import static org.junit.Assert.assertFalse;

import java.util.Set;

import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.observation.ObservationListener;
import org.eclipse.leshan.server.impl.ObservationRegistryImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class CaliforniumObservationTest {

    final String reportedValue = "15";
    Request coapRequest;
    LwM2mPath target;

    private CaliforniumTestSupport support = new CaliforniumTestSupport();

    private static LwM2mModel model;

    @BeforeClass
    public static void loadModel() {
        model = new LwM2mModel(ObjectLoader.loadDefault());
    }

    @Before
    public void setUp() throws Exception {
        support.givenASimpleClient();
        target = new LwM2mPath(3, 0, 15);
    }

    @Test
    public void coapNotification_is_forwarded_to_observationListener() {

        ObservationListener listener = new ObservationListener() {
            @Override
            public void newValue(Observation observation, LwM2mNode value) {
                assertEquals(CaliforniumObservationTest.this.reportedValue, ((LwM2mResource) value).getValue());
                assertEquals(3, observation.getPath().getObjectId());
                assertEquals((Integer) 0, observation.getPath().getObjectInstanceId());
                assertEquals((Integer) 15, observation.getPath().getResourceId());
                assertEquals(CaliforniumObservationTest.this.support.client.getRegistrationId(),
                        observation.getRegistrationId());
            }

            @Override
            public void cancelled(Observation observation) {
            }
        };
        givenAnObserveRequest(target);
        CaliforniumObservation observation = new CaliforniumObservation(coapRequest,
                support.client.getRegistrationId(), target, model);
        observation.addListener(listener);
        Response coapResponse = new Response(ResponseCode.CONTENT);
        coapResponse.setPayload(reportedValue);
        observation.onResponse(coapResponse);
    }

    @Test
    public void cancel_Observation_cancel_coapRequest() {

        givenAnObserveRequest(target);
        assertFalse(coapRequest.isCanceled());
        CaliforniumObservation observation = new CaliforniumObservation(coapRequest,
                support.client.getRegistrationId(), target, model);
        observation.cancel();
        Assert.assertTrue(coapRequest.isCanceled());
    }

    @Test
    public void cancelled_Observation_are_removed_from_registry() {
        // create an observation
        givenAnObserveRequest(target);
        CaliforniumObservation observation = new CaliforniumObservation(coapRequest,
                support.client.getRegistrationId(), target, model);
        coapRequest.addMessageObserver(observation);

        // add it to registry
        ObservationRegistryImpl registry = new ObservationRegistryImpl();
        registry.addObservation(observation);

        // check its presence
        Set<Observation> observations = registry.getObservations(support.client);
        Assert.assertFalse(observations.isEmpty());
        Assert.assertFalse(coapRequest.isCanceled());

        // cancel it
        observation.cancel();
        Assert.assertTrue(coapRequest.isCanceled());

        // check its absence
        observations = registry.getObservations(support.client);
        Assert.assertTrue(observations.isEmpty());
    }

    @Test
    public void cancelled_from_registry_observation_are_removed_from_registry() {
        // create an observation
        givenAnObserveRequest(target);
        CaliforniumObservation observation = new CaliforniumObservation(coapRequest,
                support.client.getRegistrationId(), target, model);
        coapRequest.addMessageObserver(observation);

        // add it to registry
        ObservationRegistryImpl registry = new ObservationRegistryImpl();
        registry.addObservation(observation);

        // check its presence
        Set<Observation> observations = registry.getObservations(support.client);
        Assert.assertFalse(observations.isEmpty());
        Assert.assertFalse(coapRequest.isCanceled());

        // cancel it
        registry.cancelObservations(support.client);
        Assert.assertTrue(coapRequest.isCanceled());

        // check its absence
        observations = registry.getObservations(support.client);
        Assert.assertTrue(observations.isEmpty());
    }

    private void givenAnObserveRequest(LwM2mPath target) {
        coapRequest = Request.newGet();
        coapRequest.getOptions().addUriPath(String.valueOf(target.getObjectId()));
        coapRequest.getOptions().addUriPath(String.valueOf(target.getObjectInstanceId()));
        coapRequest.getOptions().addUriPath(String.valueOf(target.getResourceId()));
        coapRequest.setDestination(support.client.getAddress());
        coapRequest.setDestinationPort(support.client.getPort());
    }
}
