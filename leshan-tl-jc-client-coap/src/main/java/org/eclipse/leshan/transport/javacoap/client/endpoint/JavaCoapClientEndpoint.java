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
package org.eclipse.leshan.transport.javacoap.client.endpoint;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.leshan.client.endpoint.ClientEndpointToolbox;
import org.eclipse.leshan.client.endpoint.LwM2mClientEndpoint;
import org.eclipse.leshan.client.servers.LwM2mServer;
import org.eclipse.leshan.core.endpoint.EndpointUri;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.request.UplinkRequest;
import org.eclipse.leshan.core.request.exception.RequestCanceledException;
import org.eclipse.leshan.core.request.exception.SendFailedException;
import org.eclipse.leshan.core.request.exception.TimeoutException.Type;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.core.util.NamedThreadFactory;
import org.eclipse.leshan.transport.javacoap.client.request.ClientCoapMessageTranslator;

import com.mbed.coap.exception.CoapTimeoutException;
import com.mbed.coap.packet.CoapRequest;
import com.mbed.coap.packet.CoapResponse;
import com.mbed.coap.server.CoapServer;

public class JavaCoapClientEndpoint implements LwM2mClientEndpoint {

    private final Protocol supportedProtocol;
    private final String endpointDescription;
    private final CoapServer coapServer;
    private final ClientCoapMessageTranslator translator;
    private final ClientEndpointToolbox toolbox;
    private final LwM2mModel model;
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1,
            new NamedThreadFactory("Leshan Async Request timeout"));

    public JavaCoapClientEndpoint(Protocol protocol, String endpointDescription, CoapServer coapServer,
            ClientCoapMessageTranslator translator, ClientEndpointToolbox toolbox, LwM2mModel model) {
        this.supportedProtocol = protocol;
        this.endpointDescription = endpointDescription;

        this.coapServer = coapServer;
        this.translator = translator;
        this.toolbox = toolbox;
        this.model = model;
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
    public EndpointUri getURI() {
        return toolbox.getUriHandler().createUri(getProtocol().getUriScheme(), coapServer.getLocalSocketAddress());
    }

    @Override
    public void forceReconnection(LwM2mServer server, boolean resume) {

    }

    @Override
    public long getMaxCommunicationPeriodFor(long lifetimeInMs) {
        // See https://github.com/OpenMobileAlliance/OMA_LwM2M_for_Developers/issues/283 to better understand.
        // TODO For DTLS, worst Handshake scenario should be taking into account too.

        int floor = 30000; // value from which we stop to adjust communication period using COAP EXCHANGE LIFETIME.

        // To be sure registration doesn't expired, update request should be send considering all CoAP retransmissions
        // and registration lifetime.
        // See https://tools.ietf.org/html/rfc7252#section-4.8.2
        // long exchange_lifetime = lwm2mendpoint.getConfig().get(CoapConfig.EXCHANGE_LIFETIME, TimeUnit.MILLISECONDS);

        long exchange_lifetime = 247000; // TODO get exchangelifetime from CoapServer
        if (lifetimeInMs - exchange_lifetime >= floor) {
            return lifetimeInMs - exchange_lifetime;
        } else {
            // LOG.warn("Too small lifetime : we advice to not use a lifetime < (COAP EXCHANGE LIFETIME + 30s)");
            // lifetime value is too short, so we do a compromise and we don't remove COAP EXCHANGE LIFETIME completely
            // We distribute the remaining lifetime range [0, exchange_lifetime + floor] on the remaining range
            // [1,floor]s.
            return lifetimeInMs * (floor - 1000) / (exchange_lifetime + floor) + 1000;
        }
    }

    @Override
    public <T extends LwM2mResponse> T send(LwM2mServer server, UplinkRequest<T> request, long timeoutInMs)
            throws InterruptedException {

        // Send LWM2M Request
        CompletableFuture<T> lwm2mResponseFuture = sendLwM2mRequest(server, request);

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
    public <T extends LwM2mResponse> void send(LwM2mServer server, UplinkRequest<T> request,
            ResponseCallback<T> responseCallback, ErrorCallback errorCallback, long timeoutInMs) {

        // Send LWM2M Request
        CompletableFuture<T> lwm2mResponseFuture = sendLwM2mRequest(server, request);

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

    protected <T extends LwM2mResponse> CompletableFuture<T> sendLwM2mRequest(LwM2mServer server,
            UplinkRequest<T> lwm2mRequest) {

        // Create Coap Request to send from LWM2M Request
        CoapRequest coapRequest = translator.createCoapRequest(server, lwm2mRequest, toolbox, model);

        // Send CoAP Request
        CompletableFuture<CoapResponse> coapResponseFuture = coapServer.clientService().apply(coapRequest);

        // On response, create LWM2M Response from CoAP response
        CompletableFuture<T> lwm2mResponseFuture = coapResponseFuture.thenApply(coapResponse -> translator
                .createLwM2mResponse(server, lwm2mRequest, coapRequest, coapResponse, toolbox));
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
}
