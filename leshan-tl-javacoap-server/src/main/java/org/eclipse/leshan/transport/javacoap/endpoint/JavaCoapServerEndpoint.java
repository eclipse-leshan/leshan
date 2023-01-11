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

import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.request.exception.SendFailedException;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.server.endpoint.LwM2mServerEndpoint;
import org.eclipse.leshan.server.endpoint.ServerEndpointToolbox;
import org.eclipse.leshan.server.profile.ClientProfile;
import org.eclipse.leshan.server.request.LowerLayerConfig;

import com.mbed.coap.client.CoapClient;
import com.mbed.coap.client.CoapClientBuilder;
import com.mbed.coap.exception.CoapException;
import com.mbed.coap.packet.CoapRequest;
import com.mbed.coap.packet.CoapResponse;
import com.mbed.coap.server.CoapServer;

public class JavaCoapServerEndpoint implements LwM2mServerEndpoint {

    private final URI endpointUri;
    private final CoapServer coapServer;
    private final ServerCoapMessageTranslator translator;
    private final ServerEndpointToolbox toolbox;

    public JavaCoapServerEndpoint(URI endpointUri, CoapServer coapServer, ServerCoapMessageTranslator translator,
            ServerEndpointToolbox toolbox) {
        this.endpointUri = endpointUri;
        this.coapServer = coapServer;
        this.translator = translator;
        this.toolbox = toolbox;
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

        final CoapRequest coapRequest = translator.createCoapRequest(destination, request, toolbox);

        // create a Coap Client to send request
        CoapClient coapClient = CoapClientBuilder.clientFor(destination.getIdentity().getPeerAddress(), coapServer);

        // Send CoAP request synchronously
        CoapResponse coapResponse = null;
        try {
            coapResponse = coapClient.sendSync(coapRequest);
        } catch (CoapException e) {
            throw new IllegalStateException("Unable to send request");
        }

        return translator.createLwM2mResponse(destination, request, coapRequest, coapResponse, toolbox);

        // TODO send request using code like this ?
        // from
        // https://github.com/open-coap/java-coap/blob/42032086dca3bf0482d3a4461d0431c9502fcf98/example-client/src/main/java/com/mbed/coap/cli/CoapCli.java#L112-L139

//        InetSocketAddress destination = new InetSocketAddress(uri.getHost(), uri.getPort());
//        CoapClient cli = CoapClientBuilder.clientFor(destination, cliServer);
//
//        Thread.sleep(200);
//
//        String uriPath = uri.getPath().isEmpty() ? CoapConstants.WELL_KNOWN_CORE : uri.getPath();
//        try {
//            CoapResponse resp = cli.sendSync(CoapRequest.of(destination, Method.valueOf(method), uriPath)
//                    .query(uri.getQuery() == null ? "" : uri.getQuery())
//                    .token(System.currentTimeMillis() % 0xFFFF)
//                    .proxy(proxyUri)
//                    .blockSize(blockSize)
//                    .payload(payload)
//            );
    }

    @Override
    public <T extends LwM2mResponse> void send(ClientProfile destination, DownlinkRequest<T> request,
            ResponseCallback<T> responseCallback, ErrorCallback errorCallback, LowerLayerConfig lowerLayerConfig,
            long timeoutInMs) {
        final CoapRequest coapRequest = translator.createCoapRequest(destination, request, toolbox);

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
                            toolbox);
                    responseCallback.onResponse(lwM2mResponse);
                });
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
