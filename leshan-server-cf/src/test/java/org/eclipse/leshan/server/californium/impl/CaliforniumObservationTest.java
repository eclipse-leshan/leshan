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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.californium.core.coap.Request;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mNodeDecoder;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.server.californium.CaliforniumObservationRegistry;
import org.eclipse.leshan.server.californium.CaliforniumRegistrationStore;
import org.eclipse.leshan.server.client.Client;
import org.eclipse.leshan.server.model.StandardModelProvider;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CaliforniumObservationTest {

    Request coapRequest;
    LwM2mPath target;
    CaliforniumObservationRegistry registry;
    CaliforniumRegistrationStore store;

    private CaliforniumTestSupport support = new CaliforniumTestSupport();

    @Before
    public void setUp() throws Exception {
        support.givenASimpleClient();
        store = new InMemoryRegistrationStore();
        registry = new CaliforniumObservationRegistryImpl(store,
                new StandardModelProvider(),
                new DefaultLwM2mNodeDecoder());
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
        int nbCancelled = registry.cancelObservations(support.client, "/3/0/12");
        Assert.assertEquals(1, nbCancelled);

        // check its absence
        observations = registry.getObservations(support.client);
        Assert.assertEquals(1, observations.size());
    }

    @Test
    public void cancel_by_observation() throws UnknownHostException {
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
        if (store.getRegistration(registrationId) == null)
            store.addRegistration(givenASimpleClient(registrationId));
        
        coapRequest = Request.newGet();
        coapRequest.setToken(CaliforniumTestSupport.createToken());
        coapRequest.getOptions().addUriPath(String.valueOf(target.getObjectId()));
        coapRequest.getOptions().addUriPath(String.valueOf(target.getObjectInstanceId()));
        coapRequest.getOptions().addUriPath(String.valueOf(target.getResourceId()));
        coapRequest.setDestination(support.client.getAddress());
        coapRequest.setDestinationPort(support.client.getPort());
        Map<String, String> context = new HashMap<>();
        context.put(CoapRequestBuilder.CTX_REGID, registrationId);
        context.put(CoapRequestBuilder.CTX_LWM2M_PATH, target.toString());
        coapRequest.setUserContext(context);

        store.add(new org.eclipse.californium.core.observe.Observation(coapRequest, null));

        Observation observation = new Observation(coapRequest.getToken(), registrationId, target, null);
        registry.addObservation(observation);

        return observation;
    }

    public Client givenASimpleClient(String registrationId) {
        InetSocketAddress registrationAddress = InetSocketAddress.createUnresolved("localhost", 5683);
        Client.Builder builder;
        try {
            builder = new Client.Builder(registrationId, registrationId + "_ep", InetAddress.getLocalHost(), 10000,
                    registrationAddress);
            return builder.build();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }
}
