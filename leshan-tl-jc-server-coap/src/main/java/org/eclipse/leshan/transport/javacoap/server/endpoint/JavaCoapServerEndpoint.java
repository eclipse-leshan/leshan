/*******************************************************************************
 * Copyright (c) 2023 Sierra Wireless and others.
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
package org.eclipse.leshan.transport.javacoap.server.endpoint;

import java.net.URI;
import java.util.SortedMap;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.leshan.core.endpoint.EndpointUriUtil;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.request.DownlinkDeviceManagementRequest;
import org.eclipse.leshan.core.request.exception.RequestCanceledException;
import org.eclipse.leshan.core.request.exception.SendFailedException;
import org.eclipse.leshan.core.request.exception.TimeoutException.Type;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.core.util.NamedThreadFactory;
import org.eclipse.leshan.core.util.Validate;
import org.eclipse.leshan.server.endpoint.LwM2mServerEndpoint;
import org.eclipse.leshan.server.endpoint.ServerEndpointToolbox;
import org.eclipse.leshan.server.profile.ClientProfile;
import org.eclipse.leshan.server.request.LowerLayerConfig;

import com.mbed.coap.exception.CoapTimeoutException;
import com.mbed.coap.packet.CoapRequest;
import com.mbed.coap.packet.CoapResponse;
import com.mbed.coap.server.CoapServer;

public class JavaCoapServerEndpoint implements LwM2mServerEndpoint {

    private final Protocol supportedProtocol;
    private final String endpointDescription;
    private final CoapServer coapServer;
    private final ServerCoapMessageTranslator translator;
    private final ServerEndpointToolbox toolbox;

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1,
            new NamedThreadFactory("Leshan Async Request timeout"));

    // A map which contains all ongoing CoAP requests
    // This is used to be able to cancel request
    private final ConcurrentNavigableMap< //
            String, // sessionId#requestId
            CompletableFuture<? extends LwM2mResponse>> // future of the ongoing Coap Request
    ongoingRequests = new ConcurrentSkipListMap<>();

    public JavaCoapServerEndpoint(Protocol protocol, String endpointDescription, CoapServer coapServer,
            ServerCoapMessageTranslator translator, ServerEndpointToolbox toolbox) {
        this.supportedProtocol = protocol;
        this.endpointDescription = endpointDescription;
        this.coapServer = coapServer;
        this.translator = translator;
        this.toolbox = toolbox;
    }

    @Override
    public Protocol getProtocol() {
        return supportedProtocol;
    }

    @Override
    public String getDescription() {
        return endpointDescription;
    }

    @Override
    public URI getURI() {
        return EndpointUriUtil.createUri(getProtocol().getUriScheme(), coapServer.getLocalSocketAddress());
    }

    @Override
    public <T extends LwM2mResponse> T send(ClientProfile destination, DownlinkDeviceManagementRequest<T> request,
            LowerLayerConfig lowerLayerConfig, long timeoutInMs) throws InterruptedException {

        // Send LWM2M Request
        CompletableFuture<T> lwm2mResponseFuture = sendLwM2mRequest(destination, request, lowerLayerConfig);

        // Wait synchronously for LWM2M response
        try {
            return lwm2mResponseFuture.get(timeoutInMs, TimeUnit.MILLISECONDS);
        } catch (CompletionException | ExecutionException | CancellationException exception) {
            if (lwm2mResponseFuture.isCancelled()) {
                throw new RequestCanceledException();
            } else {
                if (exception.getCause() instanceof CoapTimeoutException) {
                    return null;
                } else {
                    throw new SendFailedException("Unable to send request  " + exception.getCause(), exception);
                }
            }
        } catch (TimeoutException e) {
            lwm2mResponseFuture.cancel(true);
            return null;
        }
    }

    @Override
    public <T extends LwM2mResponse> void send(ClientProfile destination, DownlinkDeviceManagementRequest<T> request,
            ResponseCallback<T> responseCallback, ErrorCallback errorCallback, LowerLayerConfig lowerLayerConfig,
            long timeoutInMs) {

        // Send LWM2M Request
        CompletableFuture<T> lwm2mResponseFuture = sendLwM2mRequest(destination, request, lowerLayerConfig);

        // Attach callback
        lwm2mResponseFuture.whenComplete((lwM2mResponse, exception) -> {
            // Handle Exception
            if (exception != null) {
                if (exception instanceof CancellationException) {
                    errorCallback.onError(new RequestCanceledException());
                } else if (exception instanceof TimeoutException) {
                    errorCallback.onError(new org.eclipse.leshan.core.request.exception.TimeoutException(
                            Type.RESPONSE_TIMEOUT, exception.getCause(), "LWM2M response Timeout"));
                } else if (exception instanceof CompletionException
                        && exception.getCause() instanceof CoapTimeoutException) {
                    errorCallback.onError(new org.eclipse.leshan.core.request.exception.TimeoutException(
                            Type.COAP_TIMEOUT, exception.getCause(), "Coap Timeout"));
                } else {
                    errorCallback.onError(new SendFailedException("Unable to send request " + exception.getCause(),
                            exception.getCause()));
                }
            } else {
                // Handle CoAP Response
                responseCallback.onResponse(lwM2mResponse);
            }
        });

        // Handle timeout
        timeoutAfter(lwm2mResponseFuture, timeoutInMs);
    }

    protected <T extends LwM2mResponse> CompletableFuture<T> sendLwM2mRequest(ClientProfile destination,
            DownlinkDeviceManagementRequest<T> lwm2mRequest, LowerLayerConfig lowerLayerConfig) {

        CompletableFuture<T> lwm2mResponseFuture;
        // Create Coap Request to send from LWM2M Request
        CoapRequest coapRequest = translator.createCoapRequest(destination, lwm2mRequest, toolbox);

        // Apply Users customization
        applyUserConfig(lowerLayerConfig, coapRequest);

        // Send CoAP Request
        CompletableFuture<CoapResponse> coapResponseFuture = coapServer.clientService().apply(coapRequest);

        // On response, create LWM2M Response from CoAP response
        lwm2mResponseFuture = coapResponseFuture.thenApply(coapResponse -> translator.createLwM2mResponse(destination,
                lwm2mRequest, coapResponse, coapRequest, toolbox));

        // store ongoing request
        addOngoingRequest(destination.getRegistrationId(), lwm2mResponseFuture);

        return lwm2mResponseFuture;
    }

    public void timeoutAfter(CompletableFuture<?> future, long timeoutInMs) {
        // schedule a timeout task to stop future after given amount of time
        ScheduledFuture<?> timeoutTask = executor.schedule(() -> {
            if (future != null && !future.isDone()) {
                future.completeExceptionally(new TimeoutException());
            }
        }, timeoutInMs, TimeUnit.MILLISECONDS);

        future.whenComplete((r, e) -> {
            // Cancel TimeoutTask just above when future is complete
            if (e == null && timeoutTask != null && !timeoutTask.isDone()) {
                timeoutTask.cancel(false);
            }
        });
    }

    @Override
    public void cancelRequests(String sessionID) {
        Validate.notNull(sessionID);
        SortedMap<String, CompletableFuture<? extends LwM2mResponse>> requests = ongoingRequests
                .subMap(getFloorKey(sessionID), getCeilingKey(sessionID));
        for (CompletableFuture<? extends LwM2mResponse> request : requests.values()) {
            request.cancel(false);
        }
        requests.clear();
    }

    @Override
    public void cancelObservation(Observation observation) {
        // TODO not sure there is something to implement here.
        // Maybe trying to cancel ongoing observe request linked to this observation ?
    }

    private static String getFloorKey(String sessionID) {
        // The key format is sessionid#long, So we need a key which is always before this pattern (in natural order).
        return sessionID + '#';
    }

    private static String getCeilingKey(String sessionID) {
        // The key format is sessionid#long, So we need a key which is always after this pattern (in natural order).
        return sessionID + "#A";
    }

    private static String getKey(String sessionID, long requestId) {
        return sessionID + '#' + requestId;
    }

    private final AtomicLong idGenerator = new AtomicLong(0l);

    private void addOngoingRequest(String sessionID, CompletableFuture<? extends LwM2mResponse> coapRequest) {
        if (sessionID != null) {
            final String key = getKey(sessionID, idGenerator.incrementAndGet());
            ongoingRequests.put(key, coapRequest);
            coapRequest.whenComplete((r, e) -> {
                ongoingRequests.remove(key);
            });
        }
    }

    private void applyUserConfig(LowerLayerConfig lowerLayerConfig, CoapRequest request) {
        // TODO This is probably not so useful because of
        // https://github.com/open-coap/java-coap/issues/27#issuecomment-1514790233
        // But with should help to adapt the code : https://github.com/open-coap/java-coap/pull/68
        if (lowerLayerConfig != null)
            lowerLayerConfig.apply(request);
    }
}
