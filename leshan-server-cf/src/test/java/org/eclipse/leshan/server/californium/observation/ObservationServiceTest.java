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
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.server.californium.observation;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Set;

import org.eclipse.californium.core.coap.Request;
import org.eclipse.leshan.core.californium.EndpointContextUtil;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mNodeDecoder;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.server.californium.CaliforniumTestSupport;
import org.eclipse.leshan.server.californium.registration.CaliforniumRegistrationStore;
import org.eclipse.leshan.server.californium.registration.InMemoryRegistrationStore;
import org.eclipse.leshan.server.model.StandardModelProvider;
import org.eclipse.leshan.server.registration.Registration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ObservationServiceTest {

    Request coapRequest;
    LwM2mPath target;
    ObservationServiceImpl observationService;
    CaliforniumRegistrationStore store;

    private CaliforniumTestSupport support = new CaliforniumTestSupport();

    @Before
    public void setUp() throws Exception {
        support.givenASimpleClient();
        store = new InMemoryRegistrationStore();
        observationService = new ObservationServiceImpl(store, new StandardModelProvider(),
                new DefaultLwM2mNodeDecoder());
    }

    @Test
    public void observe_twice_cancels_first() {
        givenAnObservation(support.registration.getId(), new LwM2mPath(3, 0, 12));
        givenAnObservation(support.registration.getId(), new LwM2mPath(3, 0, 12));

        // check the presence of only one observation.
        Set<Observation> observations = observationService.getObservations(support.registration);
        Assert.assertEquals(1, observations.size());
    }

    @Test
    public void cancel_by_client() {
        // create some observations
        givenAnObservation(support.registration.getId(), new LwM2mPath(3, 0, 13));
        givenAnObservation(support.registration.getId(), new LwM2mPath(3, 0, 12));

        givenAnObservation("anotherClient", new LwM2mPath(3, 0, 12));

        // check its presence
        Set<Observation> observations = observationService.getObservations(support.registration);
        Assert.assertEquals(2, observations.size());

        // cancel it
        int nbCancelled = observationService.cancelObservations(support.registration);
        Assert.assertEquals(2, nbCancelled);

        // check its absence
        observations = observationService.getObservations(support.registration);
        Assert.assertTrue(observations.isEmpty());
    }

    @Test
    public void cancel_by_path() {
        // create some observations
        givenAnObservation(support.registration.getId(), new LwM2mPath(3, 0, 13));
        givenAnObservation(support.registration.getId(), new LwM2mPath(3, 0, 12));
        givenAnObservation(support.registration.getId(), new LwM2mPath(3, 0, 12));

        givenAnObservation("anotherClient", new LwM2mPath(3, 0, 12));

        // check its presence
        Set<Observation> observations = observationService.getObservations(support.registration);
        Assert.assertEquals(2, observations.size());

        // cancel it
        int nbCancelled = observationService.cancelObservations(support.registration, "/3/0/12");
        Assert.assertEquals(1, nbCancelled);

        // check its absence
        observations = observationService.getObservations(support.registration);
        Assert.assertEquals(1, observations.size());
    }

    @Test
    public void cancel_by_observation() throws UnknownHostException {
        // create some observations
        givenAnObservation(support.registration.getId(), new LwM2mPath(3, 0, 13));
        givenAnObservation(support.registration.getId(), new LwM2mPath(3, 0, 12));
        givenAnObservation("anotherClient", new LwM2mPath(3, 0, 12));

        Observation observationToCancel = givenAnObservation(support.registration.getId(), new LwM2mPath(3, 0, 12));

        // check its presence
        Set<Observation> observations = observationService.getObservations(support.registration);
        Assert.assertEquals(2, observations.size());

        // cancel it
        observationService.cancelObservation(observationToCancel);

        // check its absence
        observations = observationService.getObservations(support.registration);
        Assert.assertEquals(1, observations.size());
    }

    private Observation givenAnObservation(String registrationId, LwM2mPath target) {
        Registration registration = store.getRegistration(registrationId);
        if (registration == null) {
            registration = givenASimpleClient(registrationId);
            store.addRegistration(registration);
        }

        coapRequest = Request.newGet();
        coapRequest.setToken(CaliforniumTestSupport.createToken());
        coapRequest.getOptions().addUriPath(String.valueOf(target.getObjectId()));
        coapRequest.getOptions().addUriPath(String.valueOf(target.getObjectInstanceId()));
        coapRequest.getOptions().addUriPath(String.valueOf(target.getResourceId()));
        coapRequest.setObserve();
        coapRequest
                .setDestinationContext(EndpointContextUtil.extractContext(support.registration.getIdentity(), false));
        Map<String, String> context = ObserveUtil.createCoapObserveRequestContext(registration.getEndpoint(),
                registrationId, new ObserveRequest(target.toString()));
        coapRequest.setUserContext(context);

        store.put(coapRequest.getToken(), new org.eclipse.californium.core.observe.Observation(coapRequest, null));

        Observation observation = ObserveUtil.createLwM2mObservation(coapRequest);
        observationService.addObservation(registration, observation);

        return observation;
    }

    public Registration givenASimpleClient(String registrationId) {
        Registration.Builder builder;
        try {
            builder = new Registration.Builder(registrationId, registrationId + "_ep",
                    Identity.unsecure(InetAddress.getLocalHost(), 10000));
            return builder.build();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }
}
