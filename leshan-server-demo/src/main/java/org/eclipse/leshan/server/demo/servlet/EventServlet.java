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
 *     Orange - keep one JSON dependency
 *******************************************************************************/
package org.eclipse.leshan.server.demo.servlet;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.jetty.servlets.EventSource;
import org.eclipse.jetty.servlets.EventSourceServlet;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.observation.CompositeObservation;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.observation.SingleObservation;
import org.eclipse.leshan.core.request.SendRequest;
import org.eclipse.leshan.core.response.ObserveCompositeResponse;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.server.californium.LeshanServer;
import org.eclipse.leshan.server.demo.servlet.json.JacksonLwM2mNodeSerializer;
import org.eclipse.leshan.server.demo.servlet.json.JacksonRegistrationSerializer;
import org.eclipse.leshan.server.demo.servlet.log.CoapMessage;
import org.eclipse.leshan.server.demo.servlet.log.CoapMessageListener;
import org.eclipse.leshan.server.demo.servlet.log.CoapMessageTracer;
import org.eclipse.leshan.server.observation.ObservationListener;
import org.eclipse.leshan.server.queue.PresenceListener;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationListener;
import org.eclipse.leshan.server.registration.RegistrationUpdate;
import org.eclipse.leshan.server.send.SendListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jline.internal.Log;

public class EventServlet extends EventSourceServlet {

    private static final String EVENT_DEREGISTRATION = "DEREGISTRATION";

    private static final String EVENT_UPDATED = "UPDATED";

    private static final String EVENT_REGISTRATION = "REGISTRATION";

    private static final String EVENT_AWAKE = "AWAKE";

    private static final String EVENT_SLEEPING = "SLEEPING";

    private static final String EVENT_NOTIFICATION = "NOTIFICATION";

    private static final String EVENT_SEND = "SEND";

    private static final String EVENT_COAP_LOG = "COAPLOG";

    private static final String QUERY_PARAM_ENDPOINT = "ep";

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(EventServlet.class);

    private final ObjectMapper mapper;

    private final CoapMessageTracer coapMessageTracer;

    private Set<LeshanEventSource> eventSources = Collections
            .newSetFromMap(new ConcurrentHashMap<LeshanEventSource, Boolean>());

    private final RegistrationListener registrationListener = new RegistrationListener() {

        @Override
        public void registered(Registration registration, Registration previousReg,
                Collection<Observation> previousObsersations) {
            String jReg = null;
            try {
                jReg = EventServlet.this.mapper.writeValueAsString(registration);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            sendEvent(EVENT_REGISTRATION, jReg, registration.getEndpoint());
        }

        @Override
        public void updated(RegistrationUpdate update, Registration updatedRegistration,
                Registration previousRegistration) {
            RegUpdate regUpdate = new RegUpdate();
            regUpdate.registration = updatedRegistration;
            regUpdate.update = update;
            String jReg = null;
            try {
                jReg = EventServlet.this.mapper.writeValueAsString(regUpdate);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            sendEvent(EVENT_UPDATED, jReg, updatedRegistration.getEndpoint());
        }

        @Override
        public void unregistered(Registration registration, Collection<Observation> observations, boolean expired,
                Registration newReg) {
            String jReg = null;
            try {
                jReg = EventServlet.this.mapper.writeValueAsString(registration);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            sendEvent(EVENT_DEREGISTRATION, jReg, registration.getEndpoint());
        }

    };

    public final PresenceListener presenceListener = new PresenceListener() {

        @Override
        public void onSleeping(Registration registration) {
            String data = new StringBuilder("{\"ep\":\"").append(registration.getEndpoint()).append("\"}").toString();

            sendEvent(EVENT_SLEEPING, data, registration.getEndpoint());
        }

        @Override
        public void onAwake(Registration registration) {
            String data = new StringBuilder("{\"ep\":\"").append(registration.getEndpoint()).append("\"}").toString();
            sendEvent(EVENT_AWAKE, data, registration.getEndpoint());
        }
    };

    private final ObservationListener observationListener = new ObservationListener() {

        @Override
        public void cancelled(Observation observation) {
        }

        @Override
        public void onResponse(SingleObservation observation, Registration registration, ObserveResponse response) {
            String path = getObservationPaths(observation);
            LwM2mNode content = response.getContent();
            String stringContent = content.toString();
            String jsonContent = null;
            try {
                jsonContent = mapper.writeValueAsString(content);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

            onResponseCommon(registration, path, stringContent, jsonContent);
        }

        @Override
        public void onResponse(CompositeObservation observation, Registration registration,
                ObserveCompositeResponse response) {
            String path = getObservationPaths(observation);

            Map<LwM2mPath, LwM2mNode> content = response.getContent();
            String stringContent = content.toString();
            String jsonContent = null;
            try {
                jsonContent = mapper.writeValueAsString(content);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

            onResponseCommon(registration, path, stringContent, jsonContent);
        }

        private void onResponseCommon(Registration registration, String path, String stringContent,
                String jsonContent) {

            if (LOG.isDebugEnabled()) {
                LOG.debug("Received notification from [{}] containing value [{}]", path, stringContent);
            }

            if (registration != null) {
                String data = new StringBuilder("{\"ep\":\"") //
                        .append(registration.getEndpoint()) // 
                        .append("\",\"res\":\"") //
                        .append(path).append("\",\"val\":") //
                        .append(jsonContent) //
                        .append("}") //
                        .toString();

                sendEvent(EVENT_NOTIFICATION, data, registration.getEndpoint());
            }
        }

        @Override
        public void onError(Observation observation, Registration registration, Exception error) {
            if (LOG.isWarnEnabled()) {
                LOG.warn(String.format("Unable to handle notification of [%s:%s]", observation.getRegistrationId(),
                        getObservationPaths(observation)), error);
            }
        }

        @Override
        public void newObservation(Observation observation, Registration registration) {
        }
    };

    private final SendListener sendListener = new SendListener() {

        @Override
        public void dataReceived(Registration registration, Map<String, LwM2mNode> data, SendRequest request) {

            if (LOG.isDebugEnabled()) {
                LOG.debug("Received Send request from [{}] containing value [{}]", registration, data.toString());
            }

            if (registration != null) {
                String jsonContent = gson.toJson(data);
                String eventData = new StringBuilder("{\"ep\":\"").append(registration.getEndpoint())
                        .append("\",\"val\":").append(jsonContent).append("}").toString();

                sendEvent(EVENT_SEND, eventData, registration.getEndpoint());
            }
        }
    };

    private String getObservationPaths(final Observation observation) {
        String path = null;
        if (observation instanceof SingleObservation) {
            path = ((SingleObservation) observation).getPath().toString();
        } else if (observation instanceof CompositeObservation) {
            path = ((CompositeObservation) observation).getPaths().toString();
        }
        return path;
    }

    public EventServlet(LeshanServer server, int securePort) {
        server.getRegistrationService().addListener(this.registrationListener);
        server.getObservationService().addListener(this.observationListener);
        server.getPresenceService().addListener(this.presenceListener);
        server.getSendService().addListener(this.sendListener);

        // add an interceptor to each endpoint to trace all CoAP messages
        coapMessageTracer = new CoapMessageTracer(server.getRegistrationService());
        for (Endpoint endpoint : server.coap().getServer().getEndpoints()) {
            endpoint.addInterceptor(coapMessageTracer);
        }

        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        SimpleModule module = new SimpleModule();
        module.addSerializer(Registration.class, new JacksonRegistrationSerializer(server.getPresenceService()));
        module.addSerializer(LwM2mNode.class, new JacksonLwM2mNodeSerializer());
        mapper.registerModule(module);
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX"));
        this.mapper = mapper;
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

    class ClientCoapListener implements CoapMessageListener {

        private final String endpoint;

        ClientCoapListener(String endpoint) {
            this.endpoint = endpoint;
        }

        @Override
        public void trace(CoapMessage message) {
            try {
                ObjectNode coapLog = EventServlet.this.mapper.valueToTree(message);
                coapLog.put("ep", this.endpoint);
                sendEvent(EVENT_COAP_LOG, EventServlet.this.mapper.writeValueAsString(coapLog), endpoint);
            } catch (JsonProcessingException e) {
                Log.warn(String.format("Error while processing json [%s] : [%s]", message.toString(), e.getMessage()));
                sendEvent(EVENT_COAP_LOG, message.toString(), endpoint);
            }
        }

    }

    private void cleanCoapListener(String endpoint) {
        // remove the listener if there is no more eventSources for this endpoint
        for (LeshanEventSource eventSource : eventSources) {
            if (eventSource.getEndpoint() == null || eventSource.getEndpoint().equals(endpoint)) {
                return;
            }
        }
        coapMessageTracer.removeListener(endpoint);
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
            if (endpoint != null) {
                coapMessageTracer.addListener(endpoint, new ClientCoapListener(endpoint));
            }
        }

        @Override
        public void onClose() {
            cleanCoapListener(endpoint);
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

    @SuppressWarnings("unused")
    private class RegUpdate {
        public Registration registration;
        public RegistrationUpdate update;
    }
}
