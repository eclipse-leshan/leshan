/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
package org.eclipse.leshan.standalone.servlet;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.jetty.continuation.Continuation;
import org.eclipse.jetty.continuation.ContinuationListener;
import org.eclipse.jetty.continuation.ContinuationSupport;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.server.californium.impl.LeshanServer;
import org.eclipse.leshan.server.client.Client;
import org.eclipse.leshan.server.client.ClientRegistryListener;
import org.eclipse.leshan.server.observation.ObservationRegistryListener;
import org.eclipse.leshan.standalone.servlet.json.ClientSerializer;
import org.eclipse.leshan.standalone.servlet.json.LwM2mNodeSerializer;
import org.eclipse.leshan.standalone.servlet.log.CoapMessage;
import org.eclipse.leshan.standalone.servlet.log.CoapMessageListener;
import org.eclipse.leshan.standalone.servlet.log.CoapMessageTracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class EventServlet extends HttpServlet {

    private static final String EVENT_DEREGISTRATION = "DEREGISTRATION";

    private static final String EVENT_UPDATED = "UPDATED";

    private static final String EVENT_REGISTRATION = "REGISTRATION";

    private static final String EVENT_NOTIFICATION = "NOTIFICATION";

    private static final String EVENT_COAP_LOG = "COAPLOG";

    private static final String QUERY_PARAM_ENDPOINT = "ep";

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(EventServlet.class);

    private final Gson gson;

    private final byte[] EVENT = "event: ".getBytes();

    private final byte[] DATA = "data: ".getBytes();

    private final byte[] VOID = ": ".getBytes();

    private static final byte[] TERMINATION = new byte[] { '\r', '\n' };

    private final Set<Continuation> continuations = new ConcurrentHashSet<>();

    private final CoapMessageTracer coapMessageTracer;

    private final LeshanServer server;

    private final ClientRegistryListener clientRegistryListener = new ClientRegistryListener() {

        @Override
        public void registered(Client client) {
            String jClient = EventServlet.this.gson.toJson(client);
            sendEvent(EVENT_REGISTRATION, jClient, client.getEndpoint());
        }

        @Override
        public void updated(Client clientUpdated) {
            String jClient = EventServlet.this.gson.toJson(clientUpdated);
            sendEvent(EVENT_UPDATED, jClient, clientUpdated.getEndpoint());
        };

        @Override
        public void unregistered(Client client) {
            String jClient = EventServlet.this.gson.toJson(client);
            sendEvent(EVENT_DEREGISTRATION, jClient, client.getEndpoint());
        }
    };

    private final ObservationRegistryListener observationRegistryListener = new ObservationRegistryListener() {

        @Override
        public void cancelled(Observation observation) {
        }

        @Override
        public void newValue(Observation observation, LwM2mNode value) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Received notification from [{}] containing value [{}]", observation.getPath(),
                        value.toString());
            }
            Client client = server.getClientRegistry().findByRegistrationId(observation.getRegistrationId());

            if (client != null) {
                String data = new StringBuffer("{\"ep\":\"").append(client.getEndpoint()).append("\",\"res\":\"")
                        .append(observation.getPath().toString()).append("\",\"val\":").append(gson.toJson(value))
                        .append("}").toString();

                sendEvent(EVENT_NOTIFICATION, data, client.getEndpoint());
            }
        }

        @Override
        public void newObservation(Observation observation) {
        }
    };

    public EventServlet(LeshanServer server, int securePort) {
        this.server = server;
        server.getClientRegistry().addListener(this.clientRegistryListener);
        server.getObservationRegistry().addListener(this.observationRegistryListener);

        // add an interceptor to each endpoint to trace all CoAP messages
        coapMessageTracer = new CoapMessageTracer(server.getClientRegistry());
        for (Endpoint endpoint : server.getCoapServer().getEndpoints()) {
            endpoint.addInterceptor(coapMessageTracer);
        }

        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeHierarchyAdapter(Client.class, new ClientSerializer(securePort));
        gsonBuilder.registerTypeHierarchyAdapter(LwM2mNode.class, new LwM2mNodeSerializer());
        gsonBuilder.setDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        this.gson = gsonBuilder.create();
    }

    private synchronized void sendEvent(String event, String data, String endpoint) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Dispatching {} event from endpoint {}", event, endpoint);
        }

        Collection<Continuation> disconnected = new ArrayList<>();

        for (Continuation c : continuations) {
            Object endpointAttribute = c.getAttribute(QUERY_PARAM_ENDPOINT);
            if (endpointAttribute == null || endpointAttribute.equals(endpoint)) {
                try {
                    OutputStream output = c.getServletResponse().getOutputStream();
                    output.write(EVENT);
                    output.write(event.getBytes("UTF-8"));
                    output.write(TERMINATION);
                    output.write(DATA);
                    output.write(data.getBytes("UTF-8"));
                    output.write(TERMINATION);
                    output.write(TERMINATION);
                    output.flush();
                    c.getServletResponse().flushBuffer();
                } catch (IOException e) {
                    LOG.debug("Disconnected SSE client");
                    disconnected.add(c);
                }
            }
        }
        if (!disconnected.isEmpty()) {
            continuations.removeAll(disconnected);
            cleanCoapListener(endpoint);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/event-stream");
        OutputStream output = resp.getOutputStream();
        output.write(VOID);
        output.write("waiting for events".getBytes());
        output.write(TERMINATION);
        output.flush();
        resp.flushBuffer();

        Continuation c = ContinuationSupport.getContinuation(req);
        c.setTimeout(0);
        c.addContinuationListener(new ContinuationListener() {

            @Override
            public void onTimeout(Continuation continuation) {
                LOG.debug("continuation closed");
                continuation.complete();
            }

            @Override
            public void onComplete(Continuation continuation) {
                LOG.debug("continuation completed");
                continuations.remove(continuation);

                String endpoint = (String) continuation.getAttribute(QUERY_PARAM_ENDPOINT);
                if (endpoint != null) {
                    cleanCoapListener(endpoint);
                }
            }
        });

        String endpoint = req.getParameter(QUERY_PARAM_ENDPOINT);
        if (endpoint != null) {
            // mark continuation as notification listener for endpoint
            c.setAttribute(QUERY_PARAM_ENDPOINT, endpoint);
            coapMessageTracer.addListener(endpoint, new ClientCoapListener(endpoint));

        }
        synchronized (this) {
            continuations.add(c);
            c.suspend(resp);
        }
    }

    class ClientCoapListener implements CoapMessageListener {

        private final String endpoint;

        ClientCoapListener(String endpoint) {
            this.endpoint = endpoint;
        }

        @Override
        public void trace(CoapMessage message) {
            String coapLog = EventServlet.this.gson.toJson(message);
            sendEvent(EVENT_COAP_LOG, coapLog, endpoint);
        }

    }

    private void cleanCoapListener(String endpoint) {
        // remove the listener if there is no more continuation for this endpoint
        for (Continuation c : continuations) {
            String cEndpoint = (String) c.getAttribute(QUERY_PARAM_ENDPOINT);
            if (cEndpoint != null && cEndpoint.equals(endpoint)) {
                // still used
                return;
            }
        }
        coapMessageTracer.removeListener(endpoint);
    }
}
