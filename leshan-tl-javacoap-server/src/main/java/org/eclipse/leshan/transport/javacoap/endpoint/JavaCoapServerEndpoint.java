/*******************************************************************************
 * Copyright (c) 2022 Sierra Wireless and others.
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
package org.eclipse.leshan.transport.javacoap.endpoint;

import java.net.URI;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.observation.SingleObservation;
import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.exception.SendFailedException;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.server.endpoint.LwM2mServerEndpoint;
import org.eclipse.leshan.server.endpoint.ServerEndpointToolbox;
import org.eclipse.leshan.server.observation.LwM2mNotificationReceiver;
import org.eclipse.leshan.server.profile.ClientProfile;
import org.eclipse.leshan.server.registration.RegistrationStore;
import org.eclipse.leshan.server.request.LowerLayerConfig;
import org.eclipse.leshan.transport.javacoap.observation.ObservationUtil;

import com.mbed.coap.client.ObservationConsumer;
import com.mbed.coap.packet.CoapRequest;
import com.mbed.coap.packet.CoapResponse;
import com.mbed.coap.packet.Opaque;
import com.mbed.coap.server.CoapServer;

public class JavaCoapServerEndpoint implements LwM2mServerEndpoint {

    private final URI endpointUri;
    private final CoapServer coapServer;
    private final ServerCoapMessageTranslator translator;
    private final ServerEndpointToolbox toolbox;
    private final LwM2mNotificationReceiver notificationReceiver;
    private final RegistrationStore registrationStore;

    public JavaCoapServerEndpoint(URI endpointUri, CoapServer coapServer, ServerCoapMessageTranslator translator,
            ServerEndpointToolbox toolbox, LwM2mNotificationReceiver notificationReceiver,
            RegistrationStore registrationStore) {
        this.endpointUri = endpointUri;
        this.coapServer = coapServer;
        this.translator = translator;
        this.toolbox = toolbox;
        this.notificationReceiver = notificationReceiver;
        this.registrationStore = registrationStore;

    }

    @Override
    public Protocol getProtocol() {
        return Protocol.COAP;
    }

    @Override
    public URI getURI() {
        return endpointUri;
    }

    @Override
    public <T extends LwM2mResponse> T send(ClientProfile destination, DownlinkRequest<T> request,
            LowerLayerConfig lowerLayerConfig, long timeoutInMs) throws InterruptedException {

        // Send LWM2M Request
        CompletableFuture<T> lwm2mResponseFuture = sendLwM2mRequest(destination, request, lowerLayerConfig);

        // Wait synchronously for LWM2M response
        try {
            return lwm2mResponseFuture.get(timeoutInMs, TimeUnit.MILLISECONDS);
        } catch (CompletionException | ExecutionException exception) {
            // TODO we probably need to enhance this (better translate java-coap exceptions to leshan ones)
            throw new SendFailedException("Unable to send request", exception);
        } catch (TimeoutException e) {
            lwm2mResponseFuture.cancel(true);
            return null;
        }
    }

    @Override
    public <T extends LwM2mResponse> void send(ClientProfile destination, DownlinkRequest<T> request,
            ResponseCallback<T> responseCallback, ErrorCallback errorCallback, LowerLayerConfig lowerLayerConfig,
            long timeoutInMs) {

        // Send LWM2M Request
        CompletableFuture<T> lwm2mResponseFuture = sendLwM2mRequest(destination, request, lowerLayerConfig);

        // Attach callback
        lwm2mResponseFuture
                // Handle Exception
                .exceptionally((exception) -> {
                    // TODO we probably need to enhance this (better translate java-coap exceptions to leshan ones)
                    errorCallback.onError(new SendFailedException("Unable to send request", exception));
                    return null;
                })
                // Handle CoAP Response
                .thenAccept((lwM2mResponse) -> {
                    responseCallback.onResponse(lwM2mResponse);
                });

        // TODO handle timeout (cancel future)
    }

    protected <T extends LwM2mResponse> CompletableFuture<T> sendLwM2mRequest(ClientProfile destination,
            DownlinkRequest<T> lwm2mRequest, LowerLayerConfig lowerLayerConfig) {

        if (lwm2mRequest instanceof ObserveRequest) {
            return sendObserveRequest(destination, lwm2mRequest, lowerLayerConfig);
        } else {
            // Create Coap Request to send from LWM2M Request
            CoapRequest coapRequest = translator.createCoapRequest(destination, lwm2mRequest, toolbox);

            // Send CoAP Request
            CompletableFuture<CoapResponse> coapResponseFuture = coapServer.clientService().apply(coapRequest);

            // On response, create LWM2M Response from CoAP response
            CompletableFuture<T> lwm2mResponseFuture = coapResponseFuture.thenApply(coapResponse -> translator
                    .createLwM2mResponse(destination, lwm2mRequest, coapResponse, toolbox, null));

            return lwm2mResponseFuture;
        }

    }

    protected <T extends LwM2mResponse> CompletableFuture<T> sendObserveRequest(ClientProfile destination,
            DownlinkRequest<T> lwm2mRequest, LowerLayerConfig lowerLayerConfig) {
        // Create Coap Request to send from LWM2M Request
        CoapRequest coapRequest = translator.createCoapRequest(destination, lwm2mRequest, toolbox);

        // TODO HACK as we can not get token from coap response.
        Opaque token = translator.getTokenGenerator().createToken();
        final CoapRequest hackedCoapRequest = coapRequest.token(token);

        // Send CoAP Request
        CompletableFuture<CoapResponse> coapResponseFuture = coapServer.clientService().apply(hackedCoapRequest);

        // Handle Notifications
        // --------------------
        coapResponseFuture.thenAccept(r -> ObservationConsumer.consumeFrom(r.next, notification -> {
            ObserveResponse lwm2mResponse = null;
            try {
                // Create LWM2M response
                lwm2mResponse = (ObserveResponse) translator.createLwM2mResponse(destination, lwm2mRequest,
                        notification, toolbox, token);
                SingleObservation observation = lwm2mResponse.getObservation();

                // Check if we have observe relation in store for this notification
                Observation observeRelation = registrationStore.getObservation(destination.getRegistrationId(),
                        observation.getId());
                if (observeRelation != null) {
                    // We have an observe relation notify upper layer
                    notificationReceiver.onNotification(lwm2mResponse.getObservation(), destination, lwm2mResponse);
                    return true;
                } else {
                    // We haven't observe relation so stop this observation.
                    return false;
                }
            } catch (Exception e) {
                if (lwm2mResponse != null) {
                    notificationReceiver.onError(lwm2mResponse.getObservation(), destination, e);
                } else {
                    notificationReceiver
                            .onError(ObservationUtil.createSingleObservation(destination.getRegistrationId(),
                                    (ObserveRequest) lwm2mRequest, token, null), destination, e);
                }
                return false;
            }

        }));

        // On response, create LWM2M Response from CoAP response
        CompletableFuture<T> lwm2mResponseFuture = coapResponseFuture.thenApply(coapResponse -> translator
                .createLwM2mResponse(destination, lwm2mRequest, coapResponse, toolbox, token));

        // Handle Observation Relation
        // ----------------------------
        // Add Observation to the store if relation is established
        lwm2mResponseFuture.thenAccept(lwm2mResponse -> {
            if (lwm2mResponse.isSuccess()) {
                ObserveResponse observeResponse = (ObserveResponse) lwm2mResponse;
                // TODO should we handle conflict ?
                Collection<Observation> previousRelation = registrationStore
                        .addObservation(destination.getRegistrationId(), observeResponse.getObservation(), false);
                if (!previousRelation.isEmpty()) {
                    // TODO log that a relation is override.
                }
                // notify upper layer that new relation is established
                notificationReceiver.newObservation(observeResponse.getObservation(), destination.getRegistration());
            }
        });

        return lwm2mResponseFuture;

    }

    @Override
    public void cancelRequests(String sessionID) {
        // TODO not implemented yet
    }

    @Override
    public void cancelObservation(Observation observation) {
        // TODO not implemented yet
    }
}
