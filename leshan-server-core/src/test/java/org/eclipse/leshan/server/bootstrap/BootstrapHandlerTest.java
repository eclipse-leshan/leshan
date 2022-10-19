/*******************************************************************************
 * Copyright (c) 2016 Sierra Wireless and others.
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
package org.eclipse.leshan.server.bootstrap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.endpoint.EndpointUriUtil;
import org.eclipse.leshan.core.request.BootstrapDeleteRequest;
import org.eclipse.leshan.core.request.BootstrapDownlinkRequest;
import org.eclipse.leshan.core.request.BootstrapFinishRequest;
import org.eclipse.leshan.core.request.BootstrapRequest;
import org.eclipse.leshan.core.request.BootstrapWriteRequest;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.request.exception.RequestCanceledException;
import org.eclipse.leshan.core.response.BootstrapDeleteResponse;
import org.eclipse.leshan.core.response.BootstrapFinishResponse;
import org.eclipse.leshan.core.response.BootstrapResponse;
import org.eclipse.leshan.core.response.BootstrapWriteResponse;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.core.response.SendableResponse;
import org.eclipse.leshan.server.bootstrap.BootstrapHandlerTest.MockRequestSender.Mode;
import org.eclipse.leshan.server.bootstrap.request.BootstrapDownlinkRequestSender;
import org.junit.Test;

public class BootstrapHandlerTest {

    private final URI endpointUsed = EndpointUriUtil.createUri("coap://localhost:5683");

    @Test
    public void error_if_not_authorized() {
        // prepare bootstrapHandler with a session manager which does not authorized any session
        BootstrapSessionManager bsSessionManager = new MockBootstrapSessionManager(false,
                new InMemoryBootstrapConfigStore());
        BootstrapHandler bsHandler = new DefaultBootstrapHandler(new MockRequestSender(Mode.ALWAYS_SUCCESS),
                bsSessionManager, new BootstrapSessionDispatcher());

        // Try to bootstrap
        BootstrapResponse response = bsHandler.bootstrap(Identity.psk(new InetSocketAddress(4242), "pskdentity"),
                new BootstrapRequest("endpoint"), endpointUsed).getResponse();

        // Ensure bootstrap session is refused
        assertEquals(ResponseCode.BAD_REQUEST, response.getCode());
    }

    @Test
    public void bootstrap_success() throws InvalidConfigurationException {
        // prepare a bootstrap handler with a session manager which authorize all session
        // and a sender which "obtains" always successful response.
        // and a config store with and an empty config for the expected endpoint
        BootstrapDownlinkRequestSender requestSender = new MockRequestSender(Mode.ALWAYS_SUCCESS);
        EditableBootstrapConfigStore bsStore = new InMemoryBootstrapConfigStore();
        bsStore.add("endpoint", new BootstrapConfig());
        MockBootstrapSessionManager bsSessionManager = new MockBootstrapSessionManager(true, bsStore);

        BootstrapHandler bsHandler = new DefaultBootstrapHandler(requestSender, bsSessionManager,
                new BootstrapSessionDispatcher());

        // Try to bootstrap
        SendableResponse<BootstrapResponse> sendableResponse = bsHandler.bootstrap(
                Identity.psk(new InetSocketAddress(4242), "pskdentity"), new BootstrapRequest("endpoint"),
                endpointUsed);
        sendableResponse.sent();

        // Ensure bootstrap finished
        assertTrue(bsSessionManager.endWasCalled());
        assertFalse(bsSessionManager.failedWasCalled());
    }

    @Test
    public void bootstrap_failed_because_of_sent_failure() throws InvalidConfigurationException {
        // prepare a bootstrap handler with a session manager which authorize all session
        // and a sender which always failed to send request.
        // and a config store with and an empty config for the expected endpoint
        BootstrapDownlinkRequestSender requestSender = new MockRequestSender(Mode.ALWAYS_FAILURE);
        EditableBootstrapConfigStore bsStore = new InMemoryBootstrapConfigStore();
        bsStore.add("endpoint", new BootstrapConfig());
        MockBootstrapSessionManager bsSessionManager = new MockBootstrapSessionManager(true, bsStore);
        BootstrapHandler bsHandler = new DefaultBootstrapHandler(requestSender, bsSessionManager,
                new BootstrapSessionDispatcher());

        // Try to bootstrap
        SendableResponse<BootstrapResponse> sendableResponse = bsHandler.bootstrap(
                Identity.psk(new InetSocketAddress(4242), "pskdentity"), new BootstrapRequest("endpoint"),
                endpointUsed);
        sendableResponse.sent();

        // Ensure bootstrap failed
        assertFalse(bsSessionManager.endWasCalled());
        assertTrue(bsSessionManager.failedWasCalled());
        assertEquals(BootstrapFailureCause.FINISH_FAILED, bsSessionManager.lastFailureCause);
    }

    @Test
    public void two_bootstrap_at_the_same_time_not_allowed()
            throws InvalidConfigurationException, InterruptedException {
        // prepare a bootstrap handler with a session manager which authorize all session
        // and a sender which never get response.
        // and a config store with and an empty config for the expected endpoint
        MockRequestSender requestSender = new MockRequestSender(Mode.NO_RESPONSE);
        EditableBootstrapConfigStore bsStore = new InMemoryBootstrapConfigStore();
        bsStore.add("endpoint", new BootstrapConfig());
        MockBootstrapSessionManager bsSessionManager = new MockBootstrapSessionManager(true, bsStore);
        BootstrapHandler bsHandler = new DefaultBootstrapHandler(requestSender, bsSessionManager,
                new BootstrapSessionDispatcher(), DefaultBootstrapHandler.DEFAULT_TIMEOUT);

        // First bootstrap : which will not end (because of sender)
        SendableResponse<BootstrapResponse> first_response = bsHandler.bootstrap(
                Identity.psk(new InetSocketAddress(4242), "pskdentity"), new BootstrapRequest("endpoint"),
                endpointUsed);
        first_response.sent();
        // Ensure bootstrap is accepted and not finished
        BootstrapSession firstSession = bsSessionManager.lastSession;
        assertTrue(first_response.getResponse().isSuccess());
        assertNotNull(firstSession);
        assertFalse(bsSessionManager.endWasCalled());
        assertFalse(bsSessionManager.failedWasCalled());

        // Second bootstrap : for the same endpoint it must be accepted and previous one should be cancelled
        bsSessionManager.reset();
        requestSender.setMode(Mode.ALWAYS_SUCCESS);
        SendableResponse<BootstrapResponse> second_response = bsHandler.bootstrap(
                Identity.psk(new InetSocketAddress(4243), "pskdentity"), new BootstrapRequest("endpoint"),
                endpointUsed);
        second_response.sent();
        // ensure last session is accepted
        assertTrue(second_response.getResponse().isSuccess());
        assertTrue(bsSessionManager.endWasCalled());
        assertFalse(bsSessionManager.failedWasCalled());
        // and previous one cancelled
        assertFalse(bsSessionManager.endWasCalled(firstSession));
        assertTrue(bsSessionManager.failedWasCalled(firstSession, BootstrapFailureCause.CANCELLED));
    }

    public static class MockRequestSender implements BootstrapDownlinkRequestSender {

        public enum Mode {
            ALWAYS_SUCCESS, ALWAYS_FAILURE, NO_RESPONSE
        };

        private Mode mode;
        private ErrorCallback errorCallback;

        public MockRequestSender(Mode mode) {
            this.mode = mode;
        }

        @Override
        public <T extends LwM2mResponse> T send(BootstrapSession session, BootstrapDownlinkRequest<T> request,
                long timeout) throws InterruptedException {
            // Not Implemented
            return null;
        }

        public void setMode(Mode mode) {
            this.mode = mode;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T extends LwM2mResponse> void send(BootstrapSession session, BootstrapDownlinkRequest<T> request,
                long timeout, ResponseCallback<T> responseCallback, ErrorCallback errorCallback) {
            // no response, no callback call
            if (mode == Mode.NO_RESPONSE) {
                this.errorCallback = errorCallback;
                return;
            }

            if (mode == Mode.ALWAYS_SUCCESS) {
                if (request instanceof BootstrapDeleteRequest) {
                    ((ResponseCallback<BootstrapDeleteResponse>) responseCallback)
                            .onResponse(BootstrapDeleteResponse.success());
                } else if (request instanceof BootstrapWriteRequest) {
                    ((ResponseCallback<BootstrapWriteResponse>) responseCallback)
                            .onResponse(BootstrapWriteResponse.success());
                } else if (request instanceof BootstrapFinishRequest) {
                    ((ResponseCallback<BootstrapFinishResponse>) responseCallback)
                            .onResponse(BootstrapFinishResponse.success());
                }
            } else if (mode == Mode.ALWAYS_FAILURE) {
                if (request instanceof BootstrapDeleteRequest) {
                    ((ResponseCallback<BootstrapDeleteResponse>) responseCallback)
                            .onResponse(BootstrapDeleteResponse.internalServerError("delete failed"));
                } else if (request instanceof BootstrapWriteRequest) {
                    ((ResponseCallback<BootstrapWriteResponse>) responseCallback)
                            .onResponse(BootstrapWriteResponse.internalServerError("write failed"));
                } else if (request instanceof BootstrapFinishRequest) {
                    ((ResponseCallback<BootstrapFinishResponse>) responseCallback)
                            .onResponse(BootstrapFinishResponse.internalServerError("finished failed"));
                }
            }
        }

        @Override
        public void cancelOngoingRequests(BootstrapSession destination) {
            // we cancel just the last ongoing request.
            if (errorCallback != null) {
                errorCallback.onError(new RequestCanceledException("cancelled"));
            }
        }
    }

    private static class MockBootstrapSessionManager extends DefaultBootstrapSessionManager {

        private final boolean authorized;
        private BootstrapSession lastSession;
        private BootstrapFailureCause lastFailureCause;
        private final List<BootstrapSession> endedSession = new ArrayList<BootstrapSession>();
        private final Map<BootstrapSession, BootstrapFailureCause> failureCauses = new HashMap<>();

        public MockBootstrapSessionManager(boolean authorized, BootstrapConfigStore store) {
            super(null, store);
            this.authorized = authorized;
        }

        @Override
        public BootstrapSession begin(BootstrapRequest request, Identity clientIdentity, URI endpointUsed) {
            lastSession = new DefaultBootstrapSession(request, clientIdentity, authorized, endpointUsed);
            return lastSession;
        }

        public boolean endWasCalled() {
            return endedSession.contains(lastSession);
        }

        public boolean endWasCalled(BootstrapSession session) {
            return endedSession.contains(session);
        }

        public boolean failedWasCalled() {
            return failureCauses.get(lastSession) != null && this.failureCauses.get(lastSession) == lastFailureCause;
        }

        public boolean failedWasCalled(BootstrapSession session, BootstrapFailureCause cause) {
            return failureCauses.get(session) == cause;
        }

        @Override
        public void end(BootstrapSession bsSession) {
            super.end(bsSession);
            endedSession.add(bsSession);
        }

        @Override
        public void failed(BootstrapSession bsSession, BootstrapFailureCause cause) {
            super.end(bsSession);
            lastFailureCause = cause;
            failureCauses.put(bsSession, cause);
        }

        public void reset() {
            lastSession = null;
            lastFailureCause = null;
        }
    }
}
