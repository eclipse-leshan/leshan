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
package org.eclipse.leshan.transport.californium.bsserver;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.InetSocketAddress;

import org.eclipse.leshan.bsserver.BootstrapConfig;
import org.eclipse.leshan.bsserver.BootstrapConfigStore;
import org.eclipse.leshan.bsserver.BootstrapHandler;
import org.eclipse.leshan.bsserver.BootstrapHandlerFactory;
import org.eclipse.leshan.bsserver.BootstrapSession;
import org.eclipse.leshan.bsserver.BootstrapSessionListener;
import org.eclipse.leshan.bsserver.BootstrapSessionManager;
import org.eclipse.leshan.bsserver.DefaultBootstrapHandler;
import org.eclipse.leshan.bsserver.LeshanBootstrapServer;
import org.eclipse.leshan.bsserver.LeshanBootstrapServerBuilder;
import org.eclipse.leshan.bsserver.request.BootstrapDownlinkRequestSender;
import org.eclipse.leshan.core.endpoint.EndpointUri;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.peer.IpPeer;
import org.eclipse.leshan.core.request.BootstrapRequest;
import org.eclipse.leshan.core.response.BootstrapResponse;
import org.eclipse.leshan.core.response.SendableResponse;
import org.eclipse.leshan.servers.ServerEndpointNameProvider;
import org.eclipse.leshan.transport.californium.bsserver.endpoint.CaliforniumBootstrapServerEndpointsProvider;
import org.junit.jupiter.api.Test;

class LeshanBootstrapServerTest {

    private BootstrapHandler bsHandler;

    private LeshanBootstrapServer createBootstrapServer() {
        LeshanBootstrapServerBuilder builder = new LeshanBootstrapServerBuilder();
        builder.setBootstrapHandlerFactory(new BootstrapHandlerFactory() {

            @Override
            public BootstrapHandler create(BootstrapDownlinkRequestSender sender,
                    BootstrapSessionManager sessionManager, ServerEndpointNameProvider endpointNameProvider,
                    BootstrapSessionListener listener) {
                bsHandler = new DefaultBootstrapHandler(sender, sessionManager, endpointNameProvider, listener);
                return bsHandler;
            }
        });
        builder.setConfigStore(new BootstrapConfigStore() {

            @Override
            public BootstrapConfig get(BootstrapSession session) {
                BootstrapConfig config = new BootstrapConfig();
                config.toDelete.add("/");
                return config;
            }
        });
        builder.setEndpointsProviders(new CaliforniumBootstrapServerEndpointsProvider());
        return builder.build();

    }

    @Test
    @SuppressWarnings("java:S2925") // Thread.sleep usage is justified
    void testStartStopStart() {
        assertDoesNotThrow(() -> {
            LeshanBootstrapServer server = createBootstrapServer();

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

        LeshanBootstrapServer server = createBootstrapServer();
        server.start();
        Thread.sleep(100);
        // HACK force creation thread creation.
        forceThreadsCreation(server.getEndpoint(Protocol.COAP).getURI());
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

        LeshanBootstrapServer server = createBootstrapServer();
        server.start();
        Thread.sleep(100);
        // HACK force creation thread creation.
        forceThreadsCreation(server.getEndpoint(Protocol.COAP).getURI());
        Thread.sleep(100);
        server.stop();
        Thread.sleep(100);
        server.destroy();

        // ensure all thread are destroyed
        Thread.sleep(500);
        assertEquals(numberOfThreadbefore, Thread.activeCount(), "All news created threads must be destroyed");
    }

    private void forceThreadsCreation(EndpointUri endpointURI) {
        SendableResponse<BootstrapResponse> bootstrap = bsHandler.bootstrap(new IpPeer(new InetSocketAddress(5683)),
                new BootstrapRequest("test"), endpointURI);
        bootstrap.sent();
    }
}
