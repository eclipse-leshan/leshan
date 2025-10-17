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
package org.eclipse.leshan.transport.californium.server.observation;

import static org.eclipse.leshan.core.util.TestToolBox.uriHandler;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.eclipse.leshan.core.endpoint.EndpointUri;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.link.Link;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.observation.CompositeObservation;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.observation.ObservationIdentifier;
import org.eclipse.leshan.core.observation.SingleObservation;
import org.eclipse.leshan.core.peer.IpPeer;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.DownlinkDeviceManagementRequest;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.server.LeshanServer;
import org.eclipse.leshan.server.endpoint.LwM2mServerEndpoint;
import org.eclipse.leshan.server.endpoint.LwM2mServerEndpointsProvider;
import org.eclipse.leshan.server.endpoint.ServerEndpointToolbox;
import org.eclipse.leshan.server.observation.LwM2mNotificationReceiver;
import org.eclipse.leshan.server.observation.ObservationServiceImpl;
import org.eclipse.leshan.server.profile.ClientProfile;
import org.eclipse.leshan.server.registration.IRegistration;
import org.eclipse.leshan.server.registration.InMemoryRegistrationStore;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationStore;
import org.eclipse.leshan.server.request.LowerLayerConfig;
import org.eclipse.leshan.server.request.UplinkDeviceManagementRequestReceiver;
import org.eclipse.leshan.server.security.DefaultAuthorizer;
import org.eclipse.leshan.servers.security.ServerSecurityInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

public class ObservationServiceTest {

    ObservationServiceImpl observationService;
    RegistrationStore store;
    IRegistration registration;
    Random r;
    private final EndpointUri endpointUri = uriHandler.createUri("coap://localhost:5683");

    @BeforeEach
    public void setUp() throws Exception {
        registration = givenASimpleRegistration();
        store = new InMemoryRegistrationStore();
        r = new Random();
    }

    private IRegistration givenASimpleRegistration() throws UnknownHostException {
        Registration.Builder builder = new Registration.Builder("4711", "urn:endpoint",
                new IpPeer(new InetSocketAddress(InetAddress.getLocalHost(), 23452)),
                uriHandler.createUri("coap://localhost:5683"));
        return builder.lifeTimeInSec(10000L).bindingMode(EnumSet.of(BindingMode.U))
                .objectLinks(new Link[] { new Link("/3") }).build();
    }

    @Test
    public void observe_twice_cancels_first() {
        createDefaultObservationService();

        givenAnObservation(registration.getId(), new LwM2mPath(3, 0, 12));
        givenAnObservation(registration.getId(), new LwM2mPath(3, 0, 12));

        // check the presence of only one observation.
        Set<Observation> observations = observationService.getObservations(registration);
        assertEquals(1, observations.size());
    }

    @Test
    public void cancel_by_client() {
        createDefaultObservationService();

        // create some observations
        givenAnObservation(registration.getId(), new LwM2mPath(3, 0, 13));
        givenAnObservation(registration.getId(), new LwM2mPath(3, 0, 12));

        givenAnObservation("anotherClient", new LwM2mPath(3, 0, 12));

        // check its presence
        Set<Observation> observations = observationService.getObservations(registration);
        assertEquals(2, observations.size());

        // cancel it
        int nbCancelled = observationService.cancelObservations(registration);
        assertEquals(2, nbCancelled);

        // check its absence
        observations = observationService.getObservations(registration);
        assertTrue(observations.isEmpty());
    }

    @Test
    public void cancel_by_path() {
        createDefaultObservationService();

        // create some observations
        givenAnObservation(registration.getId(), new LwM2mPath(3, 0, 13));
        givenAnObservation(registration.getId(), new LwM2mPath(3, 0, 12));
        givenAnObservation(registration.getId(), new LwM2mPath(3, 0, 12));

        givenAnObservation("anotherClient", new LwM2mPath(3, 0, 12));

        // check its presence
        Set<Observation> observations = observationService.getObservations(registration);
        assertEquals(2, observations.size());

        // cancel it
        int nbCancelled = observationService.cancelObservations(registration, "/3/0/12");
        assertEquals(1, nbCancelled);

        // check its absence
        observations = observationService.getObservations(registration);
        assertEquals(1, observations.size());
    }

    @Test
    public void cancel_by_observation() {
        createDefaultObservationService();

        // create some observations
        givenAnObservation(registration.getId(), new LwM2mPath(3, 0, 13));
        givenAnObservation(registration.getId(), new LwM2mPath(3, 0, 12));
        givenAnObservation("anotherClient", new LwM2mPath(3, 0, 12));

        Observation observationToCancel = givenAnObservation(registration.getId(), new LwM2mPath(3, 0, 12));

        // check its presence
        Set<Observation> observations = observationService.getObservations(registration);
        assertEquals(2, observations.size());

        // cancel it
        observationService.cancelObservation(observationToCancel);

        // check its absence
        observations = observationService.getObservations(registration);
        assertEquals(1, observations.size());
    }

    private void createDefaultObservationService() {
        observationService = new ObservationServiceImpl(store, new DummyEndpointsProvider(),
                new DefaultAuthorizer(null));
    }

    private Observation givenAnObservation(String registrationId, LwM2mPath target) {
        Registration registration = store.getRegistration(registrationId);
        if (registration == null) {
            registration = givenASimpleClient(registrationId);
            store.addRegistration(registration);
        }

        byte[] token = new byte[8];
        r.nextBytes(token);
        SingleObservation observation = new SingleObservation(new ObservationIdentifier(endpointUri, token),
                registrationId, target, ContentFormat.DEFAULT, null, null);
        store.addObservation(registrationId, observation, false);
        return observation;
    }

    private Registration givenASimpleClient(String registrationId) {
        Registration.Builder builder;
        try {
            builder = new Registration.Builder(registrationId, registrationId + "_ep",
                    new IpPeer(new InetSocketAddress(InetAddress.getLocalHost(), 10000)), endpointUri);
            return builder.build();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    private class DummyEndpointsProvider implements LwM2mServerEndpointsProvider {
        private final LwM2mServerEndpoint dummyEndpoint = new LwM2mServerEndpoint() {
            @Override
            public <T extends LwM2mResponse> void send(ClientProfile destination,
                    DownlinkDeviceManagementRequest<T> request, ResponseCallback<T> responseCallback,
                    ErrorCallback errorCallback, LowerLayerConfig lowerLayerConfig, long timeoutInMs) {
            }

            @Override
            public <T extends LwM2mResponse> T send(ClientProfile destination,
                    DownlinkDeviceManagementRequest<T> request, LowerLayerConfig lowerLayerConfig, long timeoutInMs)
                    throws InterruptedException {
                return null;
            }

            @Override
            public EndpointUri getURI() {
                return null;
            }

            @Override
            public String getDescription() {
                return null;
            }

            @Override
            public Protocol getProtocol() {
                return null;
            }

            @Override
            public void cancelRequests(String sessionID) {

            }

            @Override
            public void cancelObservation(Observation observation) {
            }
        };

        @Override
        public List<LwM2mServerEndpoint> getEndpoints() {
            return Arrays.asList(dummyEndpoint);
        }

        @Override
        public LwM2mServerEndpoint getEndpoint(EndpointUri uri) {
            return dummyEndpoint;
        }

        @Override
        public void createEndpoints(UplinkDeviceManagementRequestReceiver requestReceiver,
                LwM2mNotificationReceiver observationService, ServerEndpointToolbox toolbox,
                ServerSecurityInfo serverSecurityInfo, LeshanServer server) {
        }

        @Override
        public void start() {
        }

        @Override
        public void stop() {
        }

        @Override
        public void destroy() {
        }
    }

    @Test
    public void assertEqualsHashcode() {
        EqualsVerifier.forClass(Observation.class).withRedefinedSubclass(CompositeObservation.class)
                .withIgnoredFields("protocolData").verify();
        EqualsVerifier.forClass(Observation.class).withRedefinedSubclass(SingleObservation.class)
                .withIgnoredFields("protocolData").verify();
    }
}
