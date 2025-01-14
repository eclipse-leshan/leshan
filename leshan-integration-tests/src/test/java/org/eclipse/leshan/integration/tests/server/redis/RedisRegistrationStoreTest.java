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
 *     Michał Wadowski (Orange) - Improved compliance with rfc6690.
 *******************************************************************************/
package org.eclipse.leshan.integration.tests.server.redis;

import static org.eclipse.leshan.core.util.TestToolBox.uriHandler;
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
import org.eclipse.leshan.core.endpoint.EndpointUri;
import org.eclipse.leshan.core.link.Link;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.observation.CompositeObservation;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.observation.ObservationIdentifier;
import org.eclipse.leshan.core.observation.SingleObservation;
import org.eclipse.leshan.core.peer.IpPeer;
import org.eclipse.leshan.core.peer.LwM2mPeer;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.ObserveCompositeRequest;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.response.ObserveCompositeResponse;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.integration.tests.util.RedisTestUtil;
import org.eclipse.leshan.server.endpoint.EffectiveEndpointUriProvider;
import org.eclipse.leshan.server.observation.LwM2mNotificationReceiver;
import org.eclipse.leshan.server.profile.ClientProfile;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationStore;
import org.eclipse.leshan.transport.californium.ObserveUtil;
import org.eclipse.leshan.transport.californium.server.observation.LwM2mObservationStore;
import org.eclipse.leshan.transport.californium.server.observation.ObservationSerDes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RedisRegistrationStoreTest {

    private final String ep = "urn:endpoint";
    private final int port = 23452;
    private final Long lifetime = 10000L;
    private final String sms = "0171-32423545";
    private final EnumSet<BindingMode> binding = EnumSet.of(BindingMode.U, BindingMode.Q, BindingMode.S);
    private final Link[] objectLinks = new Link[] { new Link("/3") };
    private final String registrationId = "4711";
    private final Token aToken = Token.EMPTY;
    private final EndpointUri endpointUri = uriHandler.createUri("coap://localhost:5683");
    private final ObservationIdentifier anObservationId = new ObservationIdentifier(endpointUri, aToken.getBytes());

    RegistrationStore store;
    LwM2mObservationStore observationStore;
    InetAddress address;
    Registration registration;

    @BeforeEach
    public void setUp() throws UnknownHostException {
        address = InetAddress.getLocalHost();
        store = RedisTestUtil.createRedisRegistrationStore();
        observationStore = new LwM2mObservationStore(new EffectiveEndpointUriProvider() {
            @Override
            public EndpointUri getEndpointUri() {
                return endpointUri;
            }
        }, store, new LwM2mNotificationReceiver() {

            @Override
            public void onNotification(CompositeObservation observation, LwM2mPeer sender, ClientProfile profile,
                    ObserveCompositeResponse response) {
            }

            @Override
            public void onNotification(SingleObservation observation, LwM2mPeer sender, ClientProfile profile,
                    ObserveResponse response) {
            }

            @Override
            public void onError(Observation observation, LwM2mPeer sender, ClientProfile profile, Exception error) {
            }

            @Override
            public void newObservation(Observation observation, Registration registration) {
            }

            @Override
            public void cancelled(Observation observation) {
            }
        }, new ObservationSerDes(new UdpDataParser(), new UdpDataSerializer()));
    }

    @AfterEach
    public void stop() {
        store.removeRegistration(registrationId);
    }

    @Test
    public void get_observation_from_request() {
        // given
        String examplePath = "/1/2/3";

        givenASimpleRegistration(lifetime);
        store.addRegistration(registration);

        org.eclipse.californium.core.observe.Observation observationToStore = prepareCoapObservationOnSingle(
                examplePath);

        // when
        observationStore.put(aToken, observationToStore);

        // then
        Observation leshanObservation = store.getObservation(registrationId, anObservationId);
        assertNotNull(leshanObservation);
        assertTrue(leshanObservation instanceof SingleObservation);
        SingleObservation observation = (SingleObservation) leshanObservation;
        assertEquals(examplePath, observation.getPath().toString());
    }

    @Test
    public void get_composite_observation_from_request() {
        // given
        List<LwM2mPath> examplePaths = Arrays.asList(new LwM2mPath("/1/2/3"), new LwM2mPath("/4/5/6"));

        givenASimpleRegistration(lifetime);
        store.addRegistration(registration);

        org.eclipse.californium.core.observe.Observation observationToStore = prepareCoapObservationOnComposite(
                examplePaths);

        // when
        observationStore.put(aToken, observationToStore);

        // then
        Observation leshanObservation = store.getObservation(registrationId, anObservationId);
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

        org.eclipse.californium.core.observe.Observation observationToStore = prepareCoapObservationOnSingle("/1/2/3");
        observationStore.put(aToken, observationToStore);

        // when
        observationStore.remove(aToken);

        // then
        Observation leshanObservation = store.getObservation(registrationId,
                new ObservationIdentifier(endpointUri, aToken.getBytes()));
        assertNull(leshanObservation);
    }

    private void givenASimpleRegistration(Long lifetime) {
        Registration.Builder builder = new Registration.Builder(registrationId, ep,
                new IpPeer(new InetSocketAddress(address, port)), uriHandler.createUri("coap://localhost:5683"));

        registration = builder.lifeTimeInSec(lifetime).smsNumber(sms).bindingMode(binding).objectLinks(objectLinks)
                .build();
    }

    private org.eclipse.californium.core.observe.Observation prepareCoapObservationOnSingle(String path) {
        ObserveRequest observeRequest = new ObserveRequest(null, path);

        Map<String, String> userContext = ObserveUtil.createCoapObserveRequestContext(ep, registrationId,
                observeRequest);

        return prepareCoapObservation(new Request(CoAP.Code.GET), userContext);
    }

    private org.eclipse.californium.core.observe.Observation prepareCoapObservationOnComposite(List<LwM2mPath> paths) {
        ObserveCompositeRequest observeRequest = new ObserveCompositeRequest(null, null, paths);

        Map<String, String> userContext = ObserveUtil.createCoapObserveCompositeRequestContext(ep, registrationId,
                observeRequest);

        return prepareCoapObservation(new Request(CoAP.Code.FETCH), userContext);
    }

    private org.eclipse.californium.core.observe.Observation prepareCoapObservation(Request coapRequest,
            Map<String, String> userContext) {
        coapRequest.setUserContext(userContext);
        coapRequest.setToken(aToken);
        coapRequest.setObserve();
        coapRequest.getOptions().setAccept(ContentFormat.DEFAULT.getCode());
        coapRequest.setMID(1);

        coapRequest.setDestinationContext(new AddressEndpointContext(new InetSocketAddress(address, port)));

        return new org.eclipse.californium.core.observe.Observation(coapRequest, null);
    }
}
