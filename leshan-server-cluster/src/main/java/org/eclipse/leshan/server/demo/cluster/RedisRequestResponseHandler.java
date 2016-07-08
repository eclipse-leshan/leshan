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
package org.eclipse.leshan.server.demo.cluster;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.server.LwM2mServer;
import org.eclipse.leshan.server.client.Client;
import org.eclipse.leshan.server.client.ClientRegistry;
import org.eclipse.leshan.server.demo.cluster.serialization.DownlinkRequestSerDes;
import org.eclipse.leshan.server.demo.cluster.serialization.ResponseSerDes;
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
 * Send LWM2M Request to a LWM2M client when JSON Request Message is received on redis {@code LESHAN_REQ} channel.</br>
 * Send JSON Response Message on redis {@code LESHAN_RESP} channel when LWM2M Response is received from LWM2M Client.
 */
public class RedisRequestResponseHandler {

    private static final Logger LOG = LoggerFactory.getLogger(RedisRequestResponseHandler.class);
    private static final String REQUEST_CHANNEL = "LESHAN_REQ";
    private static final String RESPONSE_CHANNEL = "LESHAN_RESP";

    private final LwM2mServer server;
    private final Pool<Jedis> pool;
    private final ClientRegistry clientRegistry;
    private final ExecutorService excutorService;

    public RedisRequestResponseHandler(Pool<Jedis> p, LwM2mServer server, ClientRegistry clientRegistry) {
        // Listen LWM2M response
        this.server = server;
        this.clientRegistry = clientRegistry;
        this.excutorService = Executors.newCachedThreadPool(
                new NamedThreadFactory(String.format("Redis %s channel reader", RESPONSE_CHANNEL)));

        // Listen LWM2M response from client
        this.server.addResponseListener(new ResponseListener() {
            @Override
            public void onResponse(String clientEndpoint, String requestTicket, LwM2mResponse response) {
                handleResponse(clientEndpoint, requestTicket, response);
            }

            @Override
            public void onError(String clientEndpoint, String requestTicket, Exception exception) {
                handlerError(clientEndpoint, requestTicket, exception);
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
                    } catch (Throwable e) {
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
        excutorService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    sendResponse(ticket, response);
                } catch (Throwable t) {
                    LOG.error("Unable to send response.", t);
                    sendError(ticket,
                            String.format("Expected error while sending LWM2M response.(%s)", t.getMessage()));
                }
            }
        });
    }

    private void handlerError(String clientEndpoint, final String ticket, final Exception exception) {
        excutorService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    sendError(ticket, exception.getMessage());
                } catch (Throwable t) {
                    LOG.error("Unable to send error message.", t);
                }
            }
        });
    }

    private void handleSendRequestMessage(final String message) {
        excutorService.submit(new Runnable() {
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
        } catch (Throwable t) {
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
            Client destination = clientRegistry.get(endpoint);
            if (destination == null) {
                sendError(ticket, String.format("No registration for this endpoint %s.", endpoint));
            }

            // Deserialize Request
            DownlinkRequest<?> request = DownlinkRequestSerDes.deserialize((JsonObject) jMessage.get("req"));

            // Ack we will handle this request
            sendAck(ticket);

            // Send it
            server.send(destination, ticket, request);
        } catch (Throwable t) {
            String errorMessage = String.format("Unexpected exception pending request message handling.(%s:%s)",
                    t.toString(), t.getMessage());
            LOG.error(errorMessage, t);
            sendError(ticket, errorMessage);
        }

    }

    private boolean isResponsibleFor(String endpoint) {
        // TODO implement this
        // return true if this cluster instance handle this endpoint
        return true;
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

    private void sendResponse(String ticket, LwM2mResponse response) {
        try (Jedis j = pool.getResource()) {
            JsonObject m = Json.object();
            m.add("ticket", ticket);
            m.add("rep", ResponseSerDes.jSerialize(response));
            j.publish(RESPONSE_CHANNEL, m.toString());
        }

    }
}
