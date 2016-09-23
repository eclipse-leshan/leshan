/*******************************************************************************
 * Copyright (c) 2016 Sierra Wireless and others.
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
package org.eclipse.leshan.server.bootstrap;

import static org.junit.Assert.*;

import java.net.InetSocketAddress;
import java.util.concurrent.Executor;

import org.eclipse.leshan.ResponseCode;
import org.eclipse.leshan.core.request.BootstrapDeleteRequest;
import org.eclipse.leshan.core.request.BootstrapFinishRequest;
import org.eclipse.leshan.core.request.BootstrapRequest;
import org.eclipse.leshan.core.request.BootstrapWriteRequest;
import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.response.BootstrapDeleteResponse;
import org.eclipse.leshan.core.response.BootstrapFinishResponse;
import org.eclipse.leshan.core.response.BootstrapResponse;
import org.eclipse.leshan.core.response.BootstrapWriteResponse;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.junit.Test;

public class BootstrapHandlerTest {

    @Test
    public void error_if_not_authorized() {
        final BootstrapSessionManager bsSessionManager = new MockBootstrapSessionManager(false);

        BootstrapHandler bsHandler = new BootstrapHandler(null, null, bsSessionManager);
        BootstrapResponse bootstrapResponse = bsHandler
                .bootstrap(Identity.psk(new InetSocketAddress(4242), "pskdentity"), new BootstrapRequest("enpoint"));
        assertEquals(ResponseCode.BAD_REQUEST, bootstrapResponse.getCode());
    }

    @Test
    public void notifies_at_end_of_successful_bootstrap() {
        final MockBootstrapSessionManager bsSessionManager = new MockBootstrapSessionManager(true);
        final LwM2mBootstrapRequestSender requestSender = new MockRequestSender(true);

        final BootstrapStore bsStore = new BootstrapStore() {
            @Override
            public BootstrapConfig getBootstrap(String endpoint) {
                return new BootstrapConfig();
            }
        };

        BootstrapHandler bsHandler = new BootstrapHandler(bsStore, requestSender, bsSessionManager, new Executor() {

            @Override
            public void execute(Runnable command) {
                command.run();
            }
        });

        bsHandler.bootstrap(Identity.psk(new InetSocketAddress(4242), "pskdentity"), new BootstrapRequest("enpoint"));

        assertTrue(bsSessionManager.endWasCalled());
    }

    @Test
    public void does_not_notifies_at_end_of_failed_bootstrap() {
        final MockBootstrapSessionManager bsSessionManager = new MockBootstrapSessionManager(true);
        final LwM2mBootstrapRequestSender requestSender = new MockRequestSender(false);

        final BootstrapStore bsStore = new BootstrapStore() {
            @Override
            public BootstrapConfig getBootstrap(String endpoint) {
                return new BootstrapConfig();
            }
        };

        BootstrapHandler bsHandler = new BootstrapHandler(bsStore, requestSender, bsSessionManager, new Executor() {

            @Override
            public void execute(Runnable command) {
                command.run();
            }
        });

        bsHandler.bootstrap(Identity.psk(new InetSocketAddress(4242), "pskdentity"), new BootstrapRequest("enpoint"));

        assertFalse(bsSessionManager.endWasCalled());
    }

    private class MockRequestSender implements LwM2mBootstrapRequestSender {

        private boolean success;

        public MockRequestSender(boolean success) {
            this.success = success;
        }

        @Override
        public <T extends LwM2mResponse> T send(String clientEndpoint, InetSocketAddress client, boolean secure,
                DownlinkRequest<T> request, Long timeout) throws InterruptedException {
            return null;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T extends LwM2mResponse> void send(String clientEndpoint, InetSocketAddress client, boolean secure,
                DownlinkRequest<T> request, ResponseCallback<T> responseCallback, ErrorCallback errorCallback) {
            if (request instanceof BootstrapDeleteRequest) {
                ((ResponseCallback<BootstrapDeleteResponse>) responseCallback)
                        .onResponse(BootstrapDeleteResponse.success());
            } else if (request instanceof BootstrapWriteRequest) {
                ((ResponseCallback<BootstrapWriteResponse>) responseCallback)
                        .onResponse(BootstrapWriteResponse.success());
            } else if (request instanceof BootstrapFinishRequest) {
                if (this.success) {
                    ((ResponseCallback<BootstrapFinishResponse>) responseCallback)
                            .onResponse(BootstrapFinishResponse.success());
                } else {
                    ((ResponseCallback<BootstrapFinishResponse>) responseCallback)
                            .onResponse(BootstrapFinishResponse.internalServerError("failed"));
                }
            }
        }
    }

    private class MockBootstrapSessionManager implements BootstrapSessionManager {

        private boolean authorized;
        private BootstrapSession endBsSession = null;

        public MockBootstrapSessionManager(boolean authorized) {
            this.authorized = authorized;
        }

        @Override
        public BootstrapSession begin(String endpoint, Identity clientIdentity) {
            return new BootstrapSession(endpoint, clientIdentity, authorized);
        }

        @Override
        public void end(BootstrapSession bsSession) {
            endBsSession = bsSession;
        }

        public boolean endWasCalled() {
            return this.endBsSession != null;
        }

        @Override
        public void failed(BootstrapSession bsSession, BootstrapFailureCause cause,
                DownlinkRequest<? extends LwM2mResponse> request) {
        }
    }
}
