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

import static org.junit.Assert.assertTrue;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.link.Link;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.observation.ObservationIdentifier;
import org.eclipse.leshan.core.observation.SingleObservation;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.request.Identity;
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
import org.eclipse.leshan.server.registration.InMemoryRegistrationStore;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationStore;
import org.eclipse.leshan.server.request.LowerLayerConfig;
import org.eclipse.leshan.server.request.UplinkRequestReceiver;
import org.eclipse.leshan.server.security.ServerSecurityInfo;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ObservationServiceTest {

    ObservationServiceImpl observationService;
    RegistrationStore store;
    Registration registration;
    Random r;

    @Before
    public void setUp() throws Exception {
        registration = givenASimpleRegistration();
        store = new InMemoryRegistrationStore();
        r = new Random();
    }

    private Registration givenASimpleRegistration() throws UnknownHostException {
        Registration.Builder builder = new Registration.Builder("4711", "urn:endpoint",
                Identity.unsecure(InetAddress.getLocalHost(), 23452));
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
        Assert.assertEquals(1, observations.size());
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
        Assert.assertEquals(2, observations.size());

        // cancel it
        int nbCancelled = observationService.cancelObservations(registration);
        Assert.assertEquals(2, nbCancelled);

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
        Assert.assertEquals(2, observations.size());

        // cancel it
        int nbCancelled = observationService.cancelObservations(registration, "/3/0/12");
        Assert.assertEquals(1, nbCancelled);

        // check its absence
        observations = observationService.getObservations(registration);
        Assert.assertEquals(1, observations.size());
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
        Assert.assertEquals(2, observations.size());

        // cancel it
        observationService.cancelObservation(observationToCancel);

        // check its absence
        observations = observationService.getObservations(registration);
        Assert.assertEquals(1, observations.size());
    }

    private void createDefaultObservationService() {
        observationService = new ObservationServiceImpl(store, new DummyEndpointsProvider());
    }

    private Observation givenAnObservation(String registrationId, LwM2mPath target) {
        Registration registration = store.getRegistration(registrationId);
        if (registration == null) {
            registration = givenASimpleClient(registrationId);
            store.addRegistration(registration);
        }

        byte[] token = new byte[8];
        r.nextBytes(token);
        SingleObservation observation = new SingleObservation(new ObservationIdentifier(token), registrationId, target,
                ContentFormat.DEFAULT, null, null);
        store.addObservation(registrationId, observation, false);
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

    private class DummyEndpointsProvider implements LwM2mServerEndpointsProvider {
        private final LwM2mServerEndpoint dummyEndpoint = new LwM2mServerEndpoint() {
            @Override
            public <T extends LwM2mResponse> void send(ClientProfile destination, DownlinkRequest<T> request,
                    ResponseCallback<T> responseCallback, ErrorCallback errorCallback,
                    LowerLayerConfig lowerLayerConfig, long timeoutInMs) {
            }

            @Override
            public <T extends LwM2mResponse> T send(ClientProfile destination, DownlinkRequest<T> request,
                    LowerLayerConfig lowerLayerConfig, long timeoutInMs) throws InterruptedException {
                return null;
            }

            @Override
            public URI getURI() {
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
        public LwM2mServerEndpoint getEndpoint(URI uri) {
            return dummyEndpoint;
        }

        @Override
        public void createEndpoints(UplinkRequestReceiver requestReceiver, LwM2mNotificationReceiver observationService,
                ServerEndpointToolbox toolbox, ServerSecurityInfo serverSecurityInfo, LeshanServer server) {
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
}
