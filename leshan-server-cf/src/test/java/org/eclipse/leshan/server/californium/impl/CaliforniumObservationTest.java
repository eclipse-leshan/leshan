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

import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.observe.InMemoryObservationStore;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mNodeDecoder;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.server.californium.CaliforniumObservationRegistry;
import org.eclipse.leshan.server.impl.ClientRegistryImpl;
import org.eclipse.leshan.server.model.StandardModelProvider;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CaliforniumObservationTest {

    Request coapRequest;
    LwM2mPath target;
    CaliforniumObservationRegistry registry;

    private CaliforniumTestSupport support = new CaliforniumTestSupport();

    @Before
    public void setUp() throws Exception {
        support.givenASimpleClient();
        registry = new CaliforniumObservationRegistryImpl(new InMemoryObservationStore(), new ClientRegistryImpl(),
                new StandardModelProvider(), new DefaultLwM2mNodeDecoder());
    }

    @Test
    public void observe_twice_cancels_first() {
        givenAnObservation(support.client.getRegistrationId(), new LwM2mPath(3, 0, 12));
        givenAnObservation(support.client.getRegistrationId(), new LwM2mPath(3, 0, 12));

        // check the presence of only one observation.
        Set<Observation> observations = registry.getObservations(support.client);
        Assert.assertEquals(1, observations.size());
    }

    @Test
    public void cancel_by_client() {
        // create some observations and add it to registry
        givenAnObservation(support.client.getRegistrationId(), new LwM2mPath(3, 0, 13));
        givenAnObservation(support.client.getRegistrationId(), new LwM2mPath(3, 0, 12));

        givenAnObservation("anotherClient", new LwM2mPath(3, 0, 12));

        // check its presence
        Set<Observation> observations = registry.getObservations(support.client);
        Assert.assertEquals(2, observations.size());

        // cancel it
        int nbCancelled = registry.cancelObservations(support.client);
        Assert.assertEquals(2, nbCancelled);

        // check its absence
        observations = registry.getObservations(support.client);
        Assert.assertTrue(observations.isEmpty());
    }

    @Test
    public void cancel_by_path() {
        // create some observations and add it to registry
        givenAnObservation(support.client.getRegistrationId(), new LwM2mPath(3, 0, 13));
        givenAnObservation(support.client.getRegistrationId(), new LwM2mPath(3, 0, 12));
        givenAnObservation(support.client.getRegistrationId(), new LwM2mPath(3, 0, 12));

        givenAnObservation("anotherClient", new LwM2mPath(3, 0, 12));

        // check its presence
        Set<Observation> observations = registry.getObservations(support.client);
        Assert.assertEquals(2, observations.size());

        // cancel it
        registry.cancelObservation(support.client, "/3/0/12");

        // check its absence
        observations = registry.getObservations(support.client);
        Assert.assertEquals(1, observations.size());
    }

    @Test
    public void cancel_by_observation() {
        // create some observations and add it to registry
        givenAnObservation(support.client.getRegistrationId(), new LwM2mPath(3, 0, 13));
        givenAnObservation(support.client.getRegistrationId(), new LwM2mPath(3, 0, 12));
        givenAnObservation("anotherClient", new LwM2mPath(3, 0, 12));

        Observation observationToCancel = givenAnObservation(support.client.getRegistrationId(),
                new LwM2mPath(3, 0, 12));

        // check its presence
        Set<Observation> observations = registry.getObservations(support.client);
        Assert.assertEquals(2, observations.size());

        // cancel it
        registry.cancelObservation(observationToCancel);

        // check its absence
        observations = registry.getObservations(support.client);
        Assert.assertEquals(1, observations.size());
    }

    private Observation givenAnObservation(String registrationId, LwM2mPath target) {

        coapRequest = Request.newGet();
        coapRequest.setToken(createToken());
        coapRequest.getOptions().addUriPath(String.valueOf(target.getObjectId()));
        coapRequest.getOptions().addUriPath(String.valueOf(target.getObjectInstanceId()));
        coapRequest.getOptions().addUriPath(String.valueOf(target.getResourceId()));
        coapRequest.setDestination(support.client.getAddress());
        coapRequest.setDestinationPort(support.client.getPort());
        Observation observation = new Observation(coapRequest.getToken(), registrationId, target);
        registry.addObservation(observation);
        return observation;
    }

    private byte[] createToken() {
        Random random = ThreadLocalRandom.current();
        byte[] token;
        token = new byte[random.nextInt(8) + 1];
        // random value
        random.nextBytes(token);
        return token;
    }
}
