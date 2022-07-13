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
 *     Michał Wadowski (Orange) - Add Observe-Composite feature.
 *******************************************************************************/
package org.eclipse.leshan.server.californium.observation;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Set;

import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.leshan.core.californium.EndpointContextUtil;
import org.eclipse.leshan.core.californium.ObserveUtil;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mDecoder;
import org.eclipse.leshan.core.observation.CompositeObservation;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.observation.SingleObservation;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.request.ObserveCompositeRequest;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.response.AbstractLwM2mResponse;
import org.eclipse.leshan.core.response.ObserveCompositeResponse;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.server.californium.CaliforniumTestSupport;
import org.eclipse.leshan.server.californium.DummyDecoder;
import org.eclipse.leshan.server.californium.registration.CaliforniumRegistrationStore;
import org.eclipse.leshan.server.californium.registration.InMemoryRegistrationStore;
import org.eclipse.leshan.server.model.StandardModelProvider;
import org.eclipse.leshan.server.observation.ObservationListener;
import org.eclipse.leshan.server.registration.Registration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ObservationServiceTest {

    Request coapRequest;
    ObservationServiceImpl observationService;
    CaliforniumRegistrationStore store;

    private final CaliforniumTestSupport support = new CaliforniumTestSupport();

    @Before
    public void setUp() throws Exception {
        support.givenASimpleClient();
        store = new InMemoryRegistrationStore();
    }

    @Test
    public void observe_twice_cancels_first() {
        createDefaultObservationService();

        givenAnObservation(support.registration.getId(), new LwM2mPath(3, 0, 12));
        givenAnObservation(support.registration.getId(), new LwM2mPath(3, 0, 12));

        // check the presence of only one observation.
        Set<Observation> observations = observationService.getObservations(support.registration);
        Assert.assertEquals(1, observations.size());
    }

    @Test
    public void cancel_by_client() {
        createDefaultObservationService();

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
        assertTrue(observations.isEmpty());
    }

    @Test
    public void cancel_by_path() {
        createDefaultObservationService();

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
    public void cancel_by_observation() {
        createDefaultObservationService();

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

    @Test
    public void on_notification_observe_response() {
        // given
        createDummyDecoderObservationService();

        givenAnObservation(support.registration.getId(), new LwM2mPath("/1/2/3"));

        Response coapResponse = new Response(CoAP.ResponseCode.CONTENT);
        coapResponse.setToken(coapRequest.getToken());

        CatchResponseObservationListener listener = new CatchResponseObservationListener();

        observationService.addListener(listener);

        // when
        observationService.onNotification(coapRequest, coapResponse);

        // then
        assertNotNull(listener.observeResponse);
        assertNotNull(listener.observation);
        assertTrue(listener.observeResponse instanceof ObserveResponse);
        assertTrue(listener.observation instanceof SingleObservation);
    }

    @Test
    public void on_notification_composite_observe_response() {
        // given
        createDummyDecoderObservationService();

        givenAnCompositeObservation(support.registration.getId(), new LwM2mPath("/1/2/3"));

        Response coapResponse = new Response(CoAP.ResponseCode.CONTENT);
        coapResponse.setToken(coapRequest.getToken());

        CatchResponseObservationListener listener = new CatchResponseObservationListener();

        observationService.addListener(listener);

        // when
        observationService.onNotification(coapRequest, coapResponse);

        // then
        assertNotNull(listener.observeResponse);
        assertNotNull(listener.observation);
        assertTrue(listener.observeResponse instanceof ObserveCompositeResponse);
        assertTrue(listener.observation instanceof CompositeObservation);
    }

    private void createDummyDecoderObservationService() {
        observationService = new ObservationServiceImpl(store, new StandardModelProvider(), new DummyDecoder());
    }

    private void createDefaultObservationService() {
        observationService = new ObservationServiceImpl(store, new StandardModelProvider(), new DefaultLwM2mDecoder());
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

        SingleObservation observation = ObserveUtil.createLwM2mObservation(coapRequest);
        observationService.addObservation(registration, observation);

        return observation;
    }

    private Observation givenAnCompositeObservation(String registrationId, LwM2mPath target) {
        Registration registration = store.getRegistration(registrationId);
        if (registration == null) {
            registration = givenASimpleClient(registrationId);
            store.addRegistration(registration);
        }

        coapRequest = Request.newFetch();
        coapRequest.setToken(CaliforniumTestSupport.createToken());
        coapRequest.setObserve();
        coapRequest
                .setDestinationContext(EndpointContextUtil.extractContext(support.registration.getIdentity(), false));
        Map<String, String> context = ObserveUtil.createCoapObserveCompositeRequestContext(registration.getEndpoint(),
                registrationId, new ObserveCompositeRequest(null, null, target.toString()));
        coapRequest.setUserContext(context);

        store.put(coapRequest.getToken(), new org.eclipse.californium.core.observe.Observation(coapRequest, null));

        CompositeObservation observation = ObserveUtil.createLwM2mCompositeObservation(coapRequest);

        return observation;
    }

    private Registration givenASimpleClient(String registrationId) {
        Registration.Builder builder;
        try {
            builder = new Registration.Builder(registrationId, registrationId + "_ep",
                    Identity.unsecure(InetAddress.getLocalHost(), 10000));
            return builder.build();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    private static class CatchResponseObservationListener implements ObservationListener {

        AbstractLwM2mResponse observeResponse;
        Observation observation;

        @Override
        public void newObservation(Observation observation, Registration registration) {

        }

        @Override
        public void cancelled(Observation observation) {

        }

        @Override
        public void onResponse(SingleObservation observation, Registration registration, ObserveResponse response) {
            this.observeResponse = response;
            this.observation = observation;
        }

        @Override
        public void onResponse(CompositeObservation observation, Registration registration,
                ObserveCompositeResponse response) {
            this.observeResponse = response;
            this.observation = observation;
        }

        @Override
        public void onError(Observation observation, Registration registration, Exception error) {

        }
    }
}
