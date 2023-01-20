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

import com.mbed.coap.client.CoapClient;
import com.mbed.coap.client.CoapClientBuilder;
import com.mbed.coap.client.ObservationConsumer;
import com.mbed.coap.exception.CoapException;
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

        // Create Coap Request to send
        CoapRequest coapRequest = translator.createCoapRequest(destination, request, toolbox);

        // Create a Coap Client to send request
        CoapClient coapClient = CoapClientBuilder.clientFor(destination.getIdentity().getPeerAddress(), coapServer);

        // Send CoAP request synchronously
        try {
            // Handle special case of ObserveRequest
            if (request instanceof ObserveRequest) {
                // TODO HACK as we can not get token from coapresponse.
                Opaque token = translator.getTokenGenerator().createToken();
                final CoapRequest newCoapRequest = coapRequest.token(token);

                // Add Callback to Handle notification
                // TODO HACK we don't use sendSync because of observe Handling
                CompletableFuture<CoapResponse> differedCoapResponse = coapClient.send(newCoapRequest);
                differedCoapResponse.thenAccept(r -> ObservationConsumer.consumeFrom(r.next, notification -> {
                    // Handle notification
                    ObserveResponse lwm2mResponse = null;
                    try {
                        // create LWM2M response
                        lwm2mResponse = (ObserveResponse) translator.createLwM2mResponse(destination, request,
                                coapRequest, notification, toolbox, token);
                        SingleObservation observation = lwm2mResponse.getObservation();

                        // check if we have observe relation in store for this notification
                        Observation observeRelation = registrationStore.getObservation(destination.getRegistrationId(),
                                observation.getId());
                        if (observeRelation != null) {
                            // we have an observe relation notify upper layer
                            notificationReceiver.onNotification(lwm2mResponse.getObservation(), destination,
                                    lwm2mResponse);
                            return true;
                        } else {
                            // we haven't observe relation so stop this observation.
                            return false;
                        }
                    } catch (Exception e) {
                        if (lwm2mResponse != null) {
                            notificationReceiver.onError(lwm2mResponse.getObservation(), destination, e);
                        } else {
                            notificationReceiver
                                    .onError(ObservationUtil.createSingleObservation(destination.getRegistrationId(),
                                            (ObserveRequest) request, token, null), destination, e);
                        }
                        return false;
                    }

                }));

                // wait synchronously for CoAP response;
                CoapResponse coapResponse = await(differedCoapResponse);

                // translate CoAP response into LWM2M response
                T lwm2mResponse = translator.createLwM2mResponse(destination, request, coapRequest, coapResponse,
                        toolbox, token);

                // Add Observation to the store if relation is established
                if (lwm2mResponse.isSuccess()) {
                    ObserveResponse observeResponse = (ObserveResponse) lwm2mResponse;
                    // TODO should we handle conflict ?
                    Collection<Observation> previousRelation = registrationStore
                            .addObservation(destination.getRegistrationId(), observeResponse.getObservation(), false);
                    if (!previousRelation.isEmpty()) {
                        // TODO log that a relation is override.
                    }

                    // notify upper layer that new relation is established
                    notificationReceiver.newObservation(observeResponse.getObservation(),
                            destination.getRegistration());
                }

                return lwm2mResponse;
            } else {
                // Common use case : Send CoAP Request
                CoapResponse coapResponse = coapClient.sendSync(coapRequest);
                // translate CoAP response into LWM2M response
                return translator.createLwM2mResponse(destination, request, coapRequest, coapResponse, toolbox, null);
            }
        } catch (CoapException e) {
            throw new IllegalStateException("Unable to send request");
        }
    }

    @Override
    public <T extends LwM2mResponse> void send(ClientProfile destination, DownlinkRequest<T> request,
            ResponseCallback<T> responseCallback, ErrorCallback errorCallback, LowerLayerConfig lowerLayerConfig,
            long timeoutInMs) {
        CoapRequest coapRequest = translator.createCoapRequest(destination, request, toolbox);

        // create a Coap Client to send request
        CoapClient coapClient = CoapClientBuilder.clientFor(destination.getIdentity().getPeerAddress(), coapServer);

        // Send CoAP request asynchronously
        coapClient.send(coapRequest)
                // Handle Exception
                .exceptionally((exception) -> {
                    errorCallback.onError(new SendFailedException(exception));
                    return null;
                })
                // Handle CoAP Response
                .thenAccept((coapResponse) -> {
                    T lwM2mResponse = translator.createLwM2mResponse(destination, request, coapRequest, coapResponse,
                            toolbox, null);
                    responseCallback.onResponse(lwM2mResponse);
                });
    }

    // TODO this is a copy/past from com.mbed.coap.client.CoapClient.await(CompletableFuture<CoapResponse>) we should
    // find a better way.
    private static CoapResponse await(CompletableFuture<CoapResponse> future) throws CoapException {
        try {
            return future.join();
        } catch (CompletionException ex) {
            if (ex.getCause() instanceof CoapException) {
                throw (CoapException) ex.getCause();
            } else {
                throw new CoapException(ex.getCause());
            }
        }
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
