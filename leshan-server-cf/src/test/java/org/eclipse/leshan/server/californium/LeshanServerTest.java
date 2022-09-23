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
package org.eclipse.leshan.server.californium;

import static org.junit.Assert.assertEquals;

import java.net.InetSocketAddress;
import java.util.EnumSet;

import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.server.LeshanServer;
import org.eclipse.leshan.server.LeshanServerBuilder;
import org.eclipse.leshan.server.californium.endpoint.CaliforniumServerEndpointsProvider;
import org.eclipse.leshan.server.californium.endpoint.CaliforniumServerEndpointsProvider.Builder;
import org.eclipse.leshan.server.queue.PresenceServiceImpl;
import org.eclipse.leshan.server.registration.Registration;
import org.junit.Test;

public class LeshanServerTest {

    @Test
    public void testStartStopStart() throws InterruptedException {
        Builder EndpointProviderbuilder = new CaliforniumServerEndpointsProvider.Builder();
        EndpointProviderbuilder.addEndpoint(new InetSocketAddress(0), Protocol.COAP);
        LeshanServer server = new LeshanServerBuilder().setEndpointsProvider(EndpointProviderbuilder.build()).build();

        server.start();
        Thread.sleep(100);
        server.stop();
        Thread.sleep(100);
        server.start();
    }

    @Test
    public void testStartDestroy() throws InterruptedException {
        // look at nb active thread before.
        int numberOfThreadbefore = Thread.activeCount();

        Builder EndpointProviderbuilder = new CaliforniumServerEndpointsProvider.Builder();
        EndpointProviderbuilder.addEndpoint(new InetSocketAddress(0), Protocol.COAP);
        LeshanServer server = new LeshanServerBuilder().setEndpointsProvider(EndpointProviderbuilder.build()).build();

        server.start();
        Thread.sleep(100);
        // HACK force creation thread creation.
        forceThreadsCreation(server);
        Thread.sleep(100);
        server.destroy();

        // ensure all thread are destroyed
        Thread.sleep(500);
        assertEquals("All news created threads must be destroyed", numberOfThreadbefore, Thread.activeCount());
    }

    @Test
    public void testStartStopDestroy() throws InterruptedException {
        // look at nb active thread before.
        int numberOfThreadbefore = Thread.activeCount();

        Builder EndpointProviderbuilder = new CaliforniumServerEndpointsProvider.Builder();
        EndpointProviderbuilder.addEndpoint(new InetSocketAddress(0), Protocol.COAP);
        LeshanServer server = new LeshanServerBuilder().setEndpointsProvider(EndpointProviderbuilder.build()).build();

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
        assertEquals("All news created threads must be destroyed", numberOfThreadbefore, Thread.activeCount());
    }

    private void forceThreadsCreation(LeshanServer server) {
        Registration reg = new Registration.Builder("id", "endpoint", Identity.unsecure(new InetSocketAddress(5555)))
                .bindingMode(EnumSet.of(BindingMode.U, BindingMode.Q))
                .lastEndpointUsed(server.getEndpoint(Protocol.COAP).getURI()).build();
        // Force timer thread creation of preference service.
        ((PresenceServiceImpl) server.getPresenceService()).setAwake(reg);
        // Force time thread creation of CoapAsyncRequestObserver
        server.send(reg, new ReadRequest(3), new ResponseCallback<ReadResponse>() {
            @Override
            public void onResponse(ReadResponse response) {
            }
        }, new ErrorCallback() {
            @Override
            public void onError(Exception e) {
            }
        });
    }
}
