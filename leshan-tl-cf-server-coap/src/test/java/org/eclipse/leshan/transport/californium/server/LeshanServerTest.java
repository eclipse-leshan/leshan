/*******************************************************************************
 * Copyright (c) 2019 Sierra Wireless and others.
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
package org.eclipse.leshan.transport.californium.server;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.InetSocketAddress;
import java.util.EnumSet;

import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.peer.IpPeer;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.server.LeshanServer;
import org.eclipse.leshan.server.LeshanServerBuilder;
import org.eclipse.leshan.server.queue.PresenceServiceImpl;
import org.eclipse.leshan.server.registration.IRegistration;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.transport.californium.server.endpoint.CaliforniumServerEndpointsProvider;
import org.eclipse.leshan.transport.californium.server.endpoint.CaliforniumServerEndpointsProvider.Builder;
import org.junit.jupiter.api.Test;

class LeshanServerTest {

    @Test
    @SuppressWarnings("java:S2925") // Thread.sleep usage is justified
    void testStartStopStart() {
        assertDoesNotThrow(() -> {
            Builder endpointProviderbuilder = new CaliforniumServerEndpointsProvider.Builder();
            endpointProviderbuilder.addEndpoint(new InetSocketAddress(0), Protocol.COAP);
            LeshanServer server = new LeshanServerBuilder().setEndpointsProviders(endpointProviderbuilder.build())
                    .build();

            server.start();
            Thread.sleep(100);
            server.stop();
            Thread.sleep(100);
            server.start();
        });
    }

    @Test
    @SuppressWarnings("java:S2925") // Thread.sleep usage is justified
    void testStartDestroy() throws InterruptedException {
        // look at nb active thread before.
        int numberOfThreadbefore = Thread.activeCount();

        Builder endpointProviderbuilder = new CaliforniumServerEndpointsProvider.Builder();
        endpointProviderbuilder.addEndpoint(new InetSocketAddress(0), Protocol.COAP);
        LeshanServer server = new LeshanServerBuilder().setEndpointsProviders(endpointProviderbuilder.build()).build();

        server.start();
        Thread.sleep(100);
        // HACK force creation thread creation.
        forceThreadsCreation(server);
        Thread.sleep(100);
        server.destroy();

        // ensure all thread are destroyed
        Thread.sleep(500);
        assertEquals(numberOfThreadbefore, Thread.activeCount(), "All news created threads must be destroyed");
    }

    @Test
    @SuppressWarnings("java:S2925") // Thread.sleep usage is justified
    void testStartStopDestroy() throws InterruptedException {
        // look at nb active thread before.
        int numberOfThreadbefore = Thread.activeCount();

        Builder endpointProviderbuilder = new CaliforniumServerEndpointsProvider.Builder();
        endpointProviderbuilder.addEndpoint(new InetSocketAddress(0), Protocol.COAP);
        LeshanServer server = new LeshanServerBuilder().setEndpointsProviders(endpointProviderbuilder.build()).build();

        server.start();
        Thread.sleep(100);
        // HACK force creation thread creation.
        forceThreadsCreation(server);
        Thread.sleep(100);
        server.stop();
        Thread.sleep(100);
        server.destroy();

        // ensure all thread are destroyed
        Thread.sleep(500);
        assertEquals(numberOfThreadbefore, Thread.activeCount(), "All news created threads must be destroyed");
    }

    @Test
    @SuppressWarnings("java:S2925") // Thread.sleep usage is justified
    void testStartStopDestroyQueueModeDisabled() throws InterruptedException {
        // look at nb active thread before.
        int numberOfThreadbefore = Thread.activeCount();

        Builder endpointProviderbuilder = new CaliforniumServerEndpointsProvider.Builder();
        endpointProviderbuilder.addEndpoint(new InetSocketAddress(0), Protocol.COAP);
        LeshanServer server = new LeshanServerBuilder().setEndpointsProviders(endpointProviderbuilder.build())
                .disableQueueModeSupport().build();
        server.start();
        Thread.sleep(100);
        // HACK force creation thread creation.
        forceThreadsCreation(server);
        Thread.sleep(100);
        server.stop();
        Thread.sleep(100);
        server.destroy();

        // ensure all thread are destroyed
        Thread.sleep(500);
        assertEquals(numberOfThreadbefore, Thread.activeCount(), "All news created threads must be destroyed");
    }

    private void forceThreadsCreation(LeshanServer server) {
        IRegistration reg = new Registration.Builder("id", "endpoint", new IpPeer(new InetSocketAddress(5555)),
                server.getEndpoint(Protocol.COAP).getURI()).bindingMode(EnumSet.of(BindingMode.U, BindingMode.Q))
                        .build();
        // Force timer thread creation of preference service.
        if (server.getPresenceService() != null) {
            ((PresenceServiceImpl) server.getPresenceService()).setAwake(reg);
        }
        // Force time thread creation of CoapAsyncRequestObserver
        server.send(reg, new ReadRequest(3), r -> {
        }, e -> {
        });
    }
}
