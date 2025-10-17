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
package org.eclipse.leshan.demo.server.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jetty.ee10.servlets.EventSource;
import org.eclipse.jetty.ee10.servlets.EventSourceServlet;
import org.eclipse.leshan.core.LwM2m.Version;
import org.eclipse.leshan.core.link.Link;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.TimestampedLwM2mNodes;
import org.eclipse.leshan.core.observation.CompositeObservation;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.observation.SingleObservation;
import org.eclipse.leshan.core.request.SendRequest;
import org.eclipse.leshan.core.response.ObserveCompositeResponse;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.demo.server.servlet.json.JacksonLinkSerializer;
import org.eclipse.leshan.demo.server.servlet.json.JacksonLwM2mNodeSerializer;
import org.eclipse.leshan.demo.server.servlet.json.JacksonRegistrationSerializer;
import org.eclipse.leshan.demo.server.servlet.json.JacksonRegistrationUpdateSerializer;
import org.eclipse.leshan.demo.server.servlet.json.JacksonVersionSerializer;
import org.eclipse.leshan.demo.server.servlet.log.CoapMessage;
import org.eclipse.leshan.demo.server.servlet.log.CoapMessageListener;
import org.eclipse.leshan.demo.server.servlet.log.CoapMessageTracer;
import org.eclipse.leshan.server.LeshanServer;
import org.eclipse.leshan.server.endpoint.LwM2mServerEndpoint;
import org.eclipse.leshan.server.observation.ObservationListener;
import org.eclipse.leshan.server.queue.PresenceListener;
import org.eclipse.leshan.server.registration.IRegistration;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationListener;
import org.eclipse.leshan.server.registration.RegistrationUpdate;
import org.eclipse.leshan.server.send.SendListener;
import org.eclipse.leshan.transport.californium.server.endpoint.CaliforniumServerEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.servlet.http.HttpServletRequest;
import jline.internal.Log;

public class EventServlet extends EventSourceServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(EventServlet.class);

    private static final String EVENT_DEREGISTRATION = "DEREGISTRATION";
    private static final String EVENT_UPDATED = "UPDATED";
    private static final String EVENT_REGISTRATION = "REGISTRATION";
    private static final String EVENT_AWAKE = "AWAKE";
    private static final String EVENT_SLEEPING = "SLEEPING";
    private static final String EVENT_NOTIFICATION = "NOTIFICATION";
    private static final String EVENT_SEND = "SEND";
    private static final String EVENT_COAP_LOG = "COAPLOG";

    private static final String QUERY_PARAM_ENDPOINT = "ep";

    private final transient ObjectMapper mapper;
    private final transient CoapMessageTracer coapMessageTracer;
    private final transient Set<LeshanEventSource> eventSources = Collections
            .newSetFromMap(new ConcurrentHashMap<LeshanEventSource, Boolean>());
    private final transient RegistrationListener registrationListener = new RegistrationListener() {

        @Override
        public void registered(IRegistration registration, IRegistration previousReg,
                Collection<Observation> previousObservations) {
            String jReg = null;
            try {
                jReg = EventServlet.this.mapper.writeValueAsString(registration);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException(e);
            }
            sendEvent(EVENT_REGISTRATION, jReg, registration.getEndpoint());
        }

        @Override
        public void updated(RegistrationUpdate update, IRegistration updatedRegistration,
                IRegistration previousRegistration) {
            RegUpdate regUpdate = new RegUpdate();
            regUpdate.registration = updatedRegistration;
            regUpdate.update = update;
            String jReg = null;
            try {
                jReg = EventServlet.this.mapper.writeValueAsString(regUpdate);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException(e);
            }
            sendEvent(EVENT_UPDATED, jReg, updatedRegistration.getEndpoint());
        }

        @Override
        public void unregistered(IRegistration registration, Collection<Observation> observations, boolean expired,
                IRegistration newReg) {
            String jReg = null;
            try {
                jReg = EventServlet.this.mapper.writeValueAsString(registration);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException(e);
            }
            sendEvent(EVENT_DEREGISTRATION, jReg, registration.getEndpoint());
        }

    };

    public final transient PresenceListener presenceListener = new PresenceListener() {

        @Override
        public void onSleeping(IRegistration registration) {
            String data = new StringBuilder("{\"ep\":\"").append(registration.getEndpoint()).append("\"}").toString();

            sendEvent(EVENT_SLEEPING, data, registration.getEndpoint());
        }

        @Override
        public void onAwake(IRegistration registration) {
            String data = new StringBuilder("{\"ep\":\"").append(registration.getEndpoint()).append("\"}").toString();
            sendEvent(EVENT_AWAKE, data, registration.getEndpoint());
        }
    };

    private final transient ObservationListener observationListener = new ObservationListener() {

        @Override
        public void cancelled(Observation observation) {
            // no event sent on cancelled observation for now
        }

        @Override
        public void onResponse(SingleObservation observation, IRegistration registration, ObserveResponse response) {

            if (LOG.isDebugEnabled()) {
                LOG.debug("Received notification from [{}] containing value [{}]", observation.getPath(),
                        response.getContent());
            }
            String jsonContent = null;
            try {
                jsonContent = mapper.writeValueAsString(response.getContent());
            } catch (JsonProcessingException e) {
                throw new IllegalStateException(e);
            }

            if (registration != null) {
                String data = new StringBuilder("{\"ep\":\"") //
                        .append(registration.getEndpoint()) //
                        .append("\",\"kind\":\"single\"") //
                        .append(",\"res\":\"") //
                        .append(observation.getPath()).append("\",\"val\":") //
                        .append(jsonContent) //
                        .append("}") //
                        .toString();

                sendEvent(EVENT_NOTIFICATION, data, registration.getEndpoint());
            }
        }

        @Override
        public void onResponse(CompositeObservation observation, IRegistration registration,
                ObserveCompositeResponse response) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Received composite notificationfrom [{}] containing value [{}]", registration,
                        response.getContent());
            }
            String jsonContent = null;
            String jsonListOfPath = null;
            try {
                jsonContent = mapper.writeValueAsString(response.getContent());
                List<String> paths = new ArrayList<>();
                for (LwM2mPath path : response.getObservation().getPaths()) {
                    paths.add(path.toString());
                }
                jsonListOfPath = mapper.writeValueAsString(paths);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException(e);
            }

            if (registration != null) {
                String data = new StringBuilder("{\"ep\":\"") //
                        .append(registration.getEndpoint()) //
                        .append("\",\"kind\":\"composite\"") //
                        .append(",\"val\":") //
                        .append(jsonContent) //
                        .append(",\"paths\":") //
                        .append(jsonListOfPath) //
                        .append("}") //
                        .toString();

                sendEvent(EVENT_NOTIFICATION, data, registration.getEndpoint());
            }
        }

        @Override
        public void onError(Observation observation, IRegistration registration, Exception error) {
            if (LOG.isWarnEnabled()) {
                LOG.warn(String.format("Unable to handle notification of [%s:%s]", observation.getRegistrationId(),
                        getObservationPaths(observation)), error);
            }
        }

        @Override
        public void newObservation(Observation observation, IRegistration registration) {
            // no event sent on new observation for now
        }

        private String getObservationPaths(final Observation observation) {
            String path = null;
            if (observation instanceof SingleObservation) {
                path = ((SingleObservation) observation).getPath().toString();
            } else if (observation instanceof CompositeObservation) {
                path = ((CompositeObservation) observation).getPaths().toString();
            }
            return path;
        }
    };

    private final transient SendListener sendListener = new SendListener() {

        @Override
        public void dataReceived(IRegistration registration, TimestampedLwM2mNodes data, SendRequest request) {

            if (LOG.isDebugEnabled()) {
                LOG.debug("Received Send request from [{}] containing value [{}]", registration, data);
            }

            if (registration != null) {
                try {
                    String jsonContent = EventServlet.this.mapper.writeValueAsString(data.getMostRecentNodes());

                    String eventData = new StringBuilder("{\"ep\":\"") //
                            .append(registration.getEndpoint()) //
                            .append("\",\"val\":") //
                            .append(jsonContent) //
                            .append("}") //
                            .toString(); //

                    sendEvent(EVENT_SEND, eventData, registration.getEndpoint());
                } catch (JsonProcessingException e) {
                    Log.warn(String.format("Error while processing json [%s] : [%s]", data, e.getMessage()));
                }
            }
        }

        @Override
        public void onError(IRegistration registration, String errorMessage, Exception error) {
            if (LOG.isWarnEnabled()) {
                LOG.warn(String.format("Unable to handle Send Request from [%s] : %s.", registration, errorMessage),
                        error);
            }
        }
    };

    public EventServlet(LeshanServer server) {
        server.getRegistrationService().addListener(this.registrationListener);
        server.getObservationService().addListener(this.observationListener);
        server.getPresenceService().addListener(this.presenceListener);
        server.getSendService().addListener(this.sendListener);

        // add an interceptor to each endpoint to trace all CoAP messages
        coapMessageTracer = new CoapMessageTracer(server.getRegistrationService());
        for (LwM2mServerEndpoint endpoint : server.getEndpoints()) {
            if (endpoint instanceof CaliforniumServerEndpoint)
                ((CaliforniumServerEndpoint) endpoint).getCoapEndpoint().addInterceptor(coapMessageTracer);
        }

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        SimpleModule module = new SimpleModule();
        module.addSerializer(Link.class, new JacksonLinkSerializer());
        module.addSerializer(Registration.class, new JacksonRegistrationSerializer(server.getPresenceService()));
        module.addSerializer(RegistrationUpdate.class, new JacksonRegistrationUpdateSerializer());
        module.addSerializer(LwM2mNode.class, new JacksonLwM2mNodeSerializer());
        module.addSerializer(Version.class, new JacksonVersionSerializer());
        objectMapper.registerModule(module);
        this.mapper = objectMapper;
    }

    public synchronized void sendEvent(String event, String data, String endpoint) {
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

    @Override
    protected EventSource newEventSource(HttpServletRequest req) {
        String endpoint = req.getParameter(QUERY_PARAM_ENDPOINT);
        return new LeshanEventSource(endpoint);
    }

    private class LeshanEventSource implements EventSource {

        private final String endpoint;
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
                LOG.warn("Unable to send event {}", event, e);
                onClose();
            }
        }

        public String getEndpoint() {
            return endpoint;
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
    }

    @SuppressWarnings("unused")
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    private class RegUpdate {
        private IRegistration registration;
        private RegistrationUpdate update;
    }
}
