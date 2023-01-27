/*******************************************************************************
 * Copyright (c) 2022 Sierra Wireless and others.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Token;
import org.eclipse.californium.core.network.serialization.UdpDataParser;
import org.eclipse.californium.core.network.serialization.UdpDataSerializer;
import org.eclipse.californium.elements.AddressEndpointContext;
import org.eclipse.leshan.core.californium.ObserveUtil;
import org.eclipse.leshan.core.endpoint.EndpointUriUtil;
import org.eclipse.leshan.core.link.Link;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.observation.CompositeObservation;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.observation.ObservationIdentifier;
import org.eclipse.leshan.core.observation.SingleObservation;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.request.ObserveCompositeRequest;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.response.ObserveCompositeResponse;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.server.observation.LwM2mNotificationReceiver;
import org.eclipse.leshan.server.profile.ClientProfile;
import org.eclipse.leshan.server.registration.InMemoryRegistrationStore;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class LwM2mObservationStoreTest {
    private final String ep = "urn:endpoint";
    private final int port = 23452;
    private final Long lifetime = 10000L;
    private final String sms = "0171-32423545";
    private final EnumSet<BindingMode> binding = EnumSet.of(BindingMode.U, BindingMode.Q, BindingMode.S);
    private final Link[] objectLinks = new Link[] { new Link("/3") };
    private final String registrationId = "4711";
    private final Token exampleToken = Token.EMPTY;
    private final String examplePath = "/1/2/3";
    private final List<LwM2mPath> examplePaths = Arrays.asList(new LwM2mPath("/1/2/3"), new LwM2mPath("/4/5/6"));

    RegistrationStore store;
    LwM2mObservationStore observationStore;
    InetAddress address;
    Registration registration;

    @BeforeEach
    public void setUp() throws UnknownHostException {
        address = InetAddress.getLocalHost();
        store = new InMemoryRegistrationStore();
        observationStore = new LwM2mObservationStore(store, new LwM2mNotificationReceiver() {
            @Override
            public void onNotification(CompositeObservation observation, ClientProfile profile,
                    ObserveCompositeResponse response) {
            }

            @Override
            public void onNotification(SingleObservation observation, ClientProfile profile, ObserveResponse response) {
            }

            @Override
            public void onError(Observation observation, ClientProfile profile, Exception error) {
            }

            @Override
            public void newObservation(Observation observation, Registration registration) {
            }

            @Override
            public void cancelled(Observation observation) {
            }
        }, new ObservationSerDes(new UdpDataParser(), new UdpDataSerializer()));
    }

    @Test
    public void put_coap_observation_with_valid_request() {
        // given
        givenASimpleRegistration(lifetime);
        store.addRegistration(registration);

        org.eclipse.californium.core.observe.Observation observationToStore = prepareCoapObservation();

        // when
        observationStore.put(exampleToken, observationToStore);

        // then
        org.eclipse.californium.core.observe.Observation observationFetched = observationStore.get(exampleToken);

        assertNotNull(observationFetched);
        assertEquals(observationToStore.toString(), observationFetched.toString());
    }

    @Test
    public void get_observation_from_request() {
        // given
        givenASimpleRegistration(lifetime);
        store.addRegistration(registration);

        org.eclipse.californium.core.observe.Observation observationToStore = prepareCoapObservation();

        // when
        observationStore.put(exampleToken, observationToStore);

        // then
        Observation leshanObservation = store.getObservation(registrationId,
                new ObservationIdentifier(exampleToken.getBytes()));
        assertNotNull(leshanObservation);
        assertTrue(leshanObservation instanceof SingleObservation);
        SingleObservation observation = (SingleObservation) leshanObservation;
        assertEquals(examplePath, observation.getPath().toString());
    }

    @Test
    public void get_composite_observation_from_request() {
        // given
        givenASimpleRegistration(lifetime);
        store.addRegistration(registration);

        org.eclipse.californium.core.observe.Observation observationToStore = prepareCoapCompositeObservation();

        // when
        observationStore.put(exampleToken, observationToStore);

        // then
        Observation leshanObservation = store.getObservation(registrationId,
                new ObservationIdentifier(exampleToken.getBytes()));
        assertNotNull(leshanObservation);
        assertTrue(leshanObservation instanceof CompositeObservation);
        CompositeObservation observation = (CompositeObservation) leshanObservation;
        assertEquals(examplePaths, observation.getPaths());
    }

    @Test
    public void remove_observation() {
        // given
        givenASimpleRegistration(lifetime);
        store.addRegistration(registration);

        org.eclipse.californium.core.observe.Observation observationToStore = prepareCoapCompositeObservation();
        observationStore.put(exampleToken, observationToStore);

        // when
        observationStore.remove(exampleToken);

        // then
        Observation leshanObservation = store.getObservation(registrationId,
                new ObservationIdentifier(exampleToken.getBytes()));
        assertNull(leshanObservation);
    }

    private org.eclipse.californium.core.observe.Observation prepareCoapObservation() {
        ObserveRequest observeRequest = new ObserveRequest(null, examplePath);

        Map<String, String> userContext = ObserveUtil.createCoapObserveRequestContext(ep, registrationId,
                observeRequest);

        Request coapRequest = new Request(CoAP.Code.GET);
        coapRequest.setMID(123);
        coapRequest.setUserContext(userContext);
        coapRequest.setToken(exampleToken);
        coapRequest.setObserve();
        coapRequest.getOptions().setAccept(ContentFormat.DEFAULT.getCode());
        coapRequest.setDestinationContext(new AddressEndpointContext(new InetSocketAddress("localhost", 5683)));

        return new org.eclipse.californium.core.observe.Observation(coapRequest, null);
    }

    private org.eclipse.californium.core.observe.Observation prepareCoapCompositeObservation() {
        ObserveCompositeRequest observeRequest = new ObserveCompositeRequest(null, null, examplePaths);

        Map<String, String> userContext = ObserveUtil.createCoapObserveCompositeRequestContext(ep, registrationId,
                observeRequest);

        Request coapRequest = new Request(CoAP.Code.FETCH);
        coapRequest.setMID(123);
        coapRequest.setUserContext(userContext);
        coapRequest.setToken(exampleToken);
        coapRequest.setObserve();
        coapRequest.getOptions().setAccept(ContentFormat.DEFAULT.getCode());
        coapRequest.setDestinationContext(new AddressEndpointContext(new InetSocketAddress("localhost", 5683)));

        return new org.eclipse.californium.core.observe.Observation(coapRequest, null);
    }

    private void givenASimpleRegistration(Long lifetime) {

        Registration.Builder builder = new Registration.Builder(registrationId, ep, Identity.unsecure(address, port),
                EndpointUriUtil.createUri("coap://localhost:5683"));

        registration = builder.lifeTimeInSec(lifetime).smsNumber(sms).bindingMode(binding).objectLinks(objectLinks)
                .build();
    }
}
