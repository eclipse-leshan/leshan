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
 *******************************************************************************/
package org.eclipse.leshan.server.bootstrap.demo.servlet;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.servlets.EventSource;
import org.eclipse.jetty.servlets.EventSourceServlet;
import org.eclipse.leshan.core.request.BootstrapDeleteRequest;
import org.eclipse.leshan.core.request.BootstrapDiscoverRequest;
import org.eclipse.leshan.core.request.BootstrapDownlinkRequest;
import org.eclipse.leshan.core.request.BootstrapRequest;
import org.eclipse.leshan.core.request.BootstrapWriteRequest;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.response.BootstrapDiscoverResponse;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.server.bootstrap.BootstrapFailureCause;
import org.eclipse.leshan.server.bootstrap.BootstrapSession;
import org.eclipse.leshan.server.bootstrap.BootstrapSessionListener;
import org.eclipse.leshan.server.bootstrap.LeshanBootstrapServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class EventServlet extends EventSourceServlet {
    private static final long serialVersionUID = 1L;

    private static final String EVENT_BOOTSTRAP_SESSION = "BSSESSION";
    private static final String QUERY_PARAM_ENDPOINT = "ep";

    private static final Logger LOG = LoggerFactory.getLogger(EventServlet.class);

    private final ObjectMapper objectMapper;
    private final Set<LeshanEventSource> eventSources = Collections
            .newSetFromMap(new ConcurrentHashMap<LeshanEventSource, Boolean>());

    @SuppressWarnings("unused")
    private class BootstrapEvent {
        public String name;
        public String endpoint;
        public Date time;
        public String message;

        public BootstrapEvent(String name, String endpoint, String message) {
            this.name = name;
            this.endpoint = endpoint;
            this.time = new Date();
            this.message = message;
        }
    }

    private final BootstrapSessionListener sessionListener = new BootstrapSessionListener() {

        @Override
        public void sessionInitiated(BootstrapRequest request, Identity clientIdentity) {
            try {
                String endpointName = request.getEndpointName();
                StringBuilder b = new StringBuilder();
                b.append("Bootstrap Request from ");
                b.append(clientIdentity.getPeerAddress());
                if (request.getPreferredContentFormat() != null) {
                    b.append("\n");
                    b.append("Preferred Content Format:  ");
                    b.append(request.getPreferredContentFormat().toString());
                }
                if (!request.getAdditionalAttributes().isEmpty()) {
                    b.append("\n");
                    b.append("Additional attributes: ");
                    b.append(request.getAdditionalAttributes().toString());
                }

                sendEvent(EVENT_BOOTSTRAP_SESSION,
                        objectMapper.writeValueAsString(new BootstrapEvent("new session", endpointName, b.toString())),
                        endpointName);
            } catch (JsonProcessingException e) {
                LOG.error("Bootstrap Event JSON serialization failed", e);
            }
        }

        @Override
        public void unAuthorized(BootstrapRequest request, Identity clientIdentity) {
            try {
                String endpointName = request.getEndpointName();
                StringBuilder b = new StringBuilder();
                b.append(clientIdentity);
                b.append(" is not allowed to connect.");
                b.append("\n");
                b.append("(probably bad credentials)");

                sendEvent(EVENT_BOOTSTRAP_SESSION,
                        objectMapper.writeValueAsString(new BootstrapEvent("unauthorized", endpointName, b.toString())),
                        endpointName);
            } catch (JsonProcessingException e) {
                LOG.error("Bootstrap Event JSON serialization failed", e);
            }
        }

        @Override
        public void authorized(BootstrapSession session) {
            try {
                String endpointName = session.getEndpoint();
                StringBuilder b = new StringBuilder();
                b.append(session.getIdentity());
                b.append(" is allowed to connect.");

                sendEvent(EVENT_BOOTSTRAP_SESSION,
                        objectMapper.writeValueAsString(new BootstrapEvent("authorized", endpointName, b.toString())),
                        endpointName);
            } catch (JsonProcessingException e) {
                LOG.error("Bootstrap Event JSON serialization failed", e);
            }
        }

        @Override
        public void noConfig(BootstrapSession session) {
            try {
                String endpointName = session.getEndpoint();
                StringBuilder b = new StringBuilder();
                b.append("No config to apply to this client.");

                sendEvent(EVENT_BOOTSTRAP_SESSION,
                        objectMapper.writeValueAsString(new BootstrapEvent("no config", endpointName, b.toString())),
                        endpointName);
            } catch (JsonProcessingException e) {
                LOG.error("Bootstreap Event JSON serialization failed", e);
            }
        }

        @Override
        public void sendRequest(BootstrapSession session, BootstrapDownlinkRequest<? extends LwM2mResponse> request) {
            try {
                if (request instanceof BootstrapDiscoverRequest) {
                    String endpointName = session.getEndpoint();
                    StringBuilder b = new StringBuilder();
                    b.append("Send DISCOVER request on ");
                    b.append(request.getPath().toString());
                    sendEvent(EVENT_BOOTSTRAP_SESSION, objectMapper.writeValueAsString(
                            new BootstrapEvent("send discover", endpointName, b.toString())), endpointName);
                } else if (request instanceof BootstrapDeleteRequest) {
                    String endpointName = session.getEndpoint();
                    StringBuilder b = new StringBuilder();
                    b.append("Send DELETE request on ");
                    b.append(request.getPath().toString());
                    sendEvent(EVENT_BOOTSTRAP_SESSION, objectMapper.writeValueAsString(
                            new BootstrapEvent("send delete", endpointName, b.toString())), endpointName);
                } else if (request instanceof BootstrapWriteRequest) {
                    String endpointName = session.getEndpoint();
                    StringBuilder b = new StringBuilder();
                    b.append("Send WRITE request on ");
                    b.append(request.getPath().toString());
                    b.append(" using ");
                    b.append(((BootstrapWriteRequest) request).getContentFormat());
                    b.append('\n');
                    ((BootstrapWriteRequest) request).getNode().appendPrettyNode(b, request.getPath());
                    sendEvent(EVENT_BOOTSTRAP_SESSION, objectMapper.writeValueAsString(
                            new BootstrapEvent("send write", endpointName, b.toString())), endpointName);
                } else {
                    String endpointName = session.getEndpoint();
                    StringBuilder b = new StringBuilder();
                    b.append("Send request: ");
                    b.append(request.toString());
                    sendEvent(EVENT_BOOTSTRAP_SESSION, objectMapper.writeValueAsString(
                            new BootstrapEvent("send request", endpointName, b.toString())), endpointName);
                }

            } catch (JsonProcessingException e) {
                LOG.error("Bootstrap Event JSON serialization failed", e);
            }
        }

        @Override
        public void onResponseSuccess(BootstrapSession session,
                BootstrapDownlinkRequest<? extends LwM2mResponse> request, LwM2mResponse response) {
            try {
                if (request instanceof BootstrapDiscoverResponse) {
                    String endpointName = session.getEndpoint();
                    StringBuilder b = new StringBuilder();
                    b.append("Receive DISCOVER reponse\n");
                    b.append(response.getCode().toString());
                    b.append(((BootstrapDiscoverResponse) request).getObjectLinks().toString());
                    sendEvent(EVENT_BOOTSTRAP_SESSION,
                            objectMapper.writeValueAsString(
                                    new BootstrapEvent("receive success response", endpointName, b.toString())),
                            endpointName);
                } else {
                    String endpointName = session.getEndpoint();
                    StringBuilder b = new StringBuilder();
                    b.append("Receive ");
                    b.append(response.getCode().toString());
                    sendEvent(EVENT_BOOTSTRAP_SESSION,
                            objectMapper.writeValueAsString(
                                    new BootstrapEvent("receive success response", endpointName, b.toString())),
                            endpointName);
                }
            } catch (JsonProcessingException e) {
                LOG.error("Bootstrap Event JSON serialization failed", e);
            }
        }

        @Override
        public void onResponseError(BootstrapSession session, BootstrapDownlinkRequest<? extends LwM2mResponse> request,
                LwM2mResponse response) {
            try {
                String endpointName = session.getEndpoint();
                StringBuilder b = new StringBuilder();
                b.append("Receive ");
                b.append(response.getCode().toString());
                if (response.getErrorMessage() != null) {
                    b.append("\n");
                    b.append(response.getErrorMessage());
                }
                sendEvent(EVENT_BOOTSTRAP_SESSION,
                        objectMapper.writeValueAsString(
                                new BootstrapEvent("receive error response", endpointName, b.toString())),
                        endpointName);
            } catch (JsonProcessingException e) {
                LOG.error("Bootstrap Event JSON serialization failed", e);
            }
        }

        @Override
        public void onRequestFailure(BootstrapSession session,
                BootstrapDownlinkRequest<? extends LwM2mResponse> request, Throwable cause) {
            try {
                String endpointName = session.getEndpoint();
                StringBuilder b = new StringBuilder();
                b.append("Request was not send because of failure : \n");
                appendError(cause, b);
                sendEvent(EVENT_BOOTSTRAP_SESSION, objectMapper.writeValueAsString(
                        new BootstrapEvent("request failure", endpointName, b.toString())), endpointName);
            } catch (JsonProcessingException e) {
                LOG.error("Bootstrap Event JSON serialization failed", e);
            }
        }

        public void appendError(Throwable e, StringBuilder b) {
            if (e.getMessage() != null) {
                b.append(e.getMessage());
            } else {
                b.append(e.getClass().getSimpleName());
            }
            if (e.getCause() != null) {
                b.append("\ncaused by : ");
                this.appendError(e.getCause(), b);
            }
        }

        @Override
        public void end(BootstrapSession session) {
            try {
                String endpointName = session.getEndpoint();
                StringBuilder b = new StringBuilder();
                b.append("Bootstrap session finished with success");

                sendEvent(EVENT_BOOTSTRAP_SESSION,
                        objectMapper.writeValueAsString(new BootstrapEvent("finished", endpointName, b.toString())),
                        endpointName);
            } catch (JsonProcessingException e) {
                LOG.error("Bootstrap Event JSON serialization failed", e);
            }
        }

        @Override
        public void failed(BootstrapSession session, BootstrapFailureCause cause) {
            try {
                String endpointName = session.getEndpoint();
                StringBuilder b = new StringBuilder();
                b.append("Bootstrap session failed : ");
                b.append(cause.toString());

                sendEvent(EVENT_BOOTSTRAP_SESSION,
                        objectMapper.writeValueAsString(new BootstrapEvent("failed", endpointName, b.toString())),
                        endpointName);
            } catch (JsonProcessingException e) {
                LOG.error("Bootstrap Event JSON serialization failed", e);
            }
        }

    };

    public EventServlet(LeshanBootstrapServer server) {
        server.addListener(sessionListener);
        objectMapper = new ObjectMapper();
    }

    private synchronized void sendEvent(String event, String data, String endpoint) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Dispatching {} event from endpoint {}", event, endpoint);
        }

        for (LeshanEventSource eventSource : eventSources) {
            if (eventSource.getEndpoint() == null || eventSource.getEndpoint().equals(endpoint)) {
                eventSource.sentEvent(event, data);
            }
        }
    }

    @Override
    protected EventSource newEventSource(HttpServletRequest req) {
        String endpoint = req.getParameter(QUERY_PARAM_ENDPOINT);
        return new LeshanEventSource(endpoint);
    }

    private class LeshanEventSource implements EventSource {

        private String endpoint;
        private Emitter emitter;

        public LeshanEventSource(String endpoint) {
            this.endpoint = endpoint;
        }

        @Override
        public void onOpen(Emitter emitter) throws IOException {
            this.emitter = emitter;
            eventSources.add(this);
        }

        @Override
        public void onClose() {
            eventSources.remove(this);
        }

        public void sentEvent(String event, String data) {
            try {
                emitter.event(event, data);
            } catch (IOException e) {
                e.printStackTrace();
                onClose();
            }
        }

        public String getEndpoint() {
            return endpoint;
        }
    }
}
