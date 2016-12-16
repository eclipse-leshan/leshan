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
package org.eclipse.leshan.server.cluster;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.californium.core.Utils;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.server.LwM2mServer;
import org.eclipse.leshan.server.client.Registration;
import org.eclipse.leshan.server.client.RegistrationService;
import org.eclipse.leshan.server.cluster.serialization.DownlinkRequestSerDes;
import org.eclipse.leshan.server.cluster.serialization.ResponseSerDes;
import org.eclipse.leshan.server.observation.ObservationService;
import org.eclipse.leshan.server.observation.ObservationListener;
import org.eclipse.leshan.server.response.ResponseListener;
import org.eclipse.leshan.util.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;
import redis.clients.util.Pool;

/**
 * Handle Request/Response Redis API.</br>
 * Send LWM2M Request to a registered LWM2M client when JSON Request Message is received on redis {@code LESHAN_REQ}
 * channel.</br>
 * Send JSON Response Message on redis {@code LESHAN_RESP} channel when LWM2M Response is received from LWM2M Client.
 */
public class RedisRequestResponseHandler {

    private static final Logger LOG = LoggerFactory.getLogger(RedisRequestResponseHandler.class);
    private static final String REQUEST_CHANNEL = "LESHAN_REQ";
    private static final String RESPONSE_CHANNEL = "LESHAN_RESP";

    private final LwM2mServer server;
    private final Pool<Jedis> pool;
    private final RegistrationService registrationService;
    private final ExecutorService executorService;
    private final RedisTokenHandler tokenHandler;
    private final ObservationService observationService;
    private final Map<KeyId, String> observatioIdToTicket = new ConcurrentHashMap<>();

    public RedisRequestResponseHandler(Pool<Jedis> p, LwM2mServer server, RegistrationService registrationService,
            RedisTokenHandler tokenHandler, ObservationService observationService) {
        // Listen LWM2M response
        this.server = server;
        this.registrationService = registrationService;
        this.observationService = observationService;
        this.tokenHandler = tokenHandler;
        this.executorService = Executors.newCachedThreadPool(
                new NamedThreadFactory(String.format("Redis %s channel writer", RESPONSE_CHANNEL)));

        // Listen LWM2M notification from client
        this.observationService.addListener(new ObservationListener() {

            @Override
            public void newValue(Observation observation, ObserveResponse response) {
                handleNotification(observation, response.getContent());
            }

            @Override
            public void newObservation(Observation observation) {
            }

            @Override
            public void cancelled(Observation observation) {
                observatioIdToTicket.remove(new KeyId(observation.getId()));
            }
        });

        // Listen LWM2M response from client
        this.server.addResponseListener(new ResponseListener() {
            
            @Override
            public void onResponse(Registration registration, String requestTicket, LwM2mResponse response) {
                handleResponse(registration.getEndpoint(), requestTicket, response);
            }

            @Override
            public void onError(Registration registration, String requestTicket, Exception exception) {
                handlerError(registration.getEndpoint(), requestTicket, exception);
            }

        });

        // Listen redis "send request" channel
        this.pool = p;
        new Thread(new Runnable() {
            @Override
            public void run() {
                do {
                    try (Jedis j = pool.getResource()) {
                        j.subscribe(new JedisPubSub() {
                            public void onMessage(String channel, final String message) {
                                handleSendRequestMessage(message);
                            };
                        }, REQUEST_CHANNEL);
                    } catch (RuntimeException e) {
                        LOG.warn("Redis SUBSCRIBE interrupted.", e);
                    }

                    // wait & re-launch
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                    }
                    LOG.warn("Relaunch Redis SUBSCRIBE.");
                } while (true);
            }
        }, String.format("Redis %s channel reader", REQUEST_CHANNEL)).start();

    }

    private void handleResponse(String clientEndpoint, final String ticket, final LwM2mResponse response) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    sendResponse(ticket, response);
                } catch (RuntimeException t) {
                    LOG.error("Unable to send response.", t);
                    sendError(ticket,
                            String.format("Expected error while sending LWM2M response.(%s)", t.getMessage()));
                }
            }
        });
    }

    private void handleNotification(final Observation observation, final LwM2mNode value) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                String ticket = observatioIdToTicket.get(new KeyId(observation.getId()));
                try {
                    sendNotification(ticket, value);
                } catch (RuntimeException t) {
                    LOG.error("Unable to send Notification.", t);
                    sendError(ticket,
                            String.format("Expected error while sending LWM2M Notification.(%s)", t.getMessage()));
                }
            }
        });
    }

    private void handlerError(String clientEndpoint, final String ticket, final Exception exception) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    sendError(ticket, exception.getMessage());
                } catch (RuntimeException t) {
                    LOG.error("Unable to send error message.", t);
                }
            }
        });
    }

    private void handleSendRequestMessage(final String message) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                sendRequest(message);
            }
        });
    }

    private void sendRequest(final String message) {
        // Parse JSON and extract ticket
        String ticket;
        JsonObject jMessage;
        try {
            jMessage = (JsonObject) Json.parse(message);
            ticket = jMessage.getString("ticket", null);
        } catch (RuntimeException t) {
            LOG.error(String.format("Unexpected exception pending request message handling.\n", message), t);
            return;
        }

        // Now if an error occurred we can prevent message sender
        try {
            // Check if we must handle this request
            String endpoint = jMessage.getString("ep", null);
            if (!isResponsibleFor(endpoint))
                return;

            // Get the registration for this endpoint
            Registration destination = registrationService.getByEndpoint(endpoint);
            if (destination == null) {
                sendError(ticket, String.format("No registration for this endpoint %s.", endpoint));
            }

            // Deserialize Request
            DownlinkRequest<?> request = DownlinkRequestSerDes.deserialize((JsonObject) jMessage.get("req"));

            // Ack we will handle this request
            sendAck(ticket);

            // Send it
            server.send(destination, ticket, request);
        } catch (RuntimeException t) {
            String errorMessage = String.format("Unexpected exception pending request message handling.(%s:%s)",
                    t.toString(), t.getMessage());
            LOG.error(errorMessage, t);
            sendError(ticket, errorMessage);
        }

    }

    private boolean isResponsibleFor(String endpoint) {
        return tokenHandler.isResponsible(endpoint);
    }

    private void sendAck(String ticket) {
        try (Jedis j = pool.getResource()) {
            JsonObject m = Json.object();
            m.add("ticket", ticket);
            m.add("ack", true);
            j.publish(RESPONSE_CHANNEL, m.toString());
        }
    }

    private void sendError(String ticket, String message) {
        try (Jedis j = pool.getResource()) {
            JsonObject m = Json.object();
            m.add("ticket", ticket);

            JsonObject err = Json.object();
            err.add("errorMessage", message);

            m.add("err", err);
            j.publish(RESPONSE_CHANNEL, m.toString());
        }

    }

    private void sendNotification(String ticket, LwM2mNode value) {
        try (Jedis j = pool.getResource()) {
            JsonObject m = Json.object();
            m.add("ticket", ticket);
            m.add("rep", ResponseSerDes.jSerialize(ObserveResponse.success(value)));
            j.publish(RESPONSE_CHANNEL, m.toString());
        }
    }

    private void sendResponse(String ticket, LwM2mResponse response) {
        if (response instanceof ObserveResponse) {
            Observation observation = ((ObserveResponse) response).getObservation();
            observatioIdToTicket.put(new KeyId(observation.getId()), ticket);
        }
        try (Jedis j = pool.getResource()) {
            JsonObject m = Json.object();
            m.add("ticket", ticket);
            m.add("rep", ResponseSerDes.jSerialize(response));
            j.publish(RESPONSE_CHANNEL, m.toString());
        }
    }

    public static final class KeyId {

        protected final byte[] id;
        private final int hash;

        public KeyId(byte[] token) {
            if (token == null)
                throw new NullPointerException();
            this.id = token;
            this.hash = Arrays.hashCode(token);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof KeyId))
                return false;
            KeyId key = (KeyId) o;
            return Arrays.equals(id, key.id);
        }

        @Override
        public String toString() {
            return new StringBuilder("KeyId[").append(Utils.toHexString(id)).append("]").toString();
        }
    }
}
