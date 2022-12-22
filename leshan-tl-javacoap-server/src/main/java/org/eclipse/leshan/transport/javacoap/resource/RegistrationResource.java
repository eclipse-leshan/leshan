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
package org.eclipse.leshan.transport.javacoap.resource;

import static java.util.concurrent.CompletableFuture.completedFuture;

import java.net.URI;
import java.text.ParseException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;

import org.eclipse.leshan.core.link.Link;
import org.eclipse.leshan.core.link.LinkParseException;
import org.eclipse.leshan.core.link.LinkParser;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.request.RegisterRequest;
import org.eclipse.leshan.core.request.UpdateRequest;
import org.eclipse.leshan.core.response.RegisterResponse;
import org.eclipse.leshan.core.response.SendableResponse;
import org.eclipse.leshan.core.response.UpdateResponse;
import org.eclipse.leshan.server.request.UplinkRequestReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mbed.coap.packet.CoapRequest;
import com.mbed.coap.packet.CoapResponse;
import com.mbed.coap.packet.Code;

public class RegistrationResource extends LwM2mCoapResource {

    private static final Logger LOG = LoggerFactory.getLogger(RegistrationResource.class);

    private static final String QUERY_PARAM_ENDPOINT = "ep=";

    private static final String QUERY_PARAM_BINDING_MODE = "b=";

    private static final String QUERY_PARAM_LWM2M_VERSION = "lwm2m=";

    private static final String QUERY_PARAM_SMS = "sms=";

    private static final String QUERY_PARAM_LIFETIME = "lt=";

    private static final String QUERY_PARAM_QUEUEMMODE = "Q"; // since LWM2M 1.1

    public static final String RESOURCE_NAME = "rd";
    public static final String RESOURCE_URI = "/" + RESOURCE_NAME + "/*";

    private final UplinkRequestReceiver receiver;
    private final LinkParser linkParser;
    private final URI endpointUsed;

    public RegistrationResource(UplinkRequestReceiver receiver, LinkParser linkParser, URI endpointUsed) {
        super(RESOURCE_URI);
        this.receiver = receiver;
        this.linkParser = linkParser;
        this.endpointUsed = endpointUsed;
    }

    @Override
    public CompletableFuture<CoapResponse> handlePOST(CoapRequest coapRequest) {
        LOG.trace("POST received : {}", coapRequest);

        // The LWM2M spec (section 8.2) mandates the usage of confirmable messages
        // TODO how to check is request is confirmable message ?
//        if (!CON.equals(coapRequest.getType())) {
//            handleInvalidRequest(coapRequest, "CON CoAP type expected");
//            return;
//        }

        // validate URI
        String uriAsString = coapRequest.options().getUriPath();
        if (uriAsString == null) {
            return handleInvalidRequest(coapRequest, "Bad URI");
        }
        List<String> uri = Arrays.asList(uriAsString.split("/"));
        if (uri.size() == 0 || !RESOURCE_NAME.equals(uri.get(0))) {
            return handleInvalidRequest(coapRequest, "Bad URI");
        }

        // Handle Register or Registration Update
        if (uri.size() == 1) {
            return handleRegister(coapRequest);
        } else if (uri.size() == 2) {
            return handleUpdate(coapRequest, uri.get(1));
        } else {
            return handleInvalidRequest(coapRequest, "Bad URI");
        }
    }

    @Override
    public CompletableFuture<CoapResponse> handleDELETE(CoapRequest coapRequest) {
        LOG.trace("DELETE received : {}", coapRequest);

        // validate URI
        String uriAsString = coapRequest.options().getUriPath();
        if (uriAsString == null) {
            return handleInvalidRequest(coapRequest, "Bad URI");
        }
        List<String> uri = Arrays.asList(uriAsString.split("/"));
        if (uri.size() == 0 || !RESOURCE_NAME.equals(uri.get(0))) {
            return handleInvalidRequest(coapRequest, "Bad URI");
        }

        if (uri != null && uri.size() == 2 && RESOURCE_NAME.equals(uri.get(0))) {
            return handleDeregister(coapRequest, uri.get(1));
        } else {
            return handleInvalidRequest(coapRequest, "Bad URI");
        }
    }

    protected CompletableFuture<CoapResponse> handleRegister(CoapRequest coapRequest) {
        // Get identity
        // --------------------------------
        Identity sender = getForeignPeerIdentity(coapRequest);

        // Create LwM2m request from CoAP request
        // --------------------------------
        // We don't check content media type is APPLICATION LINK FORMAT for now as this is the only format we can expect
        String endpoint = null;
        Long lifetime = null;
        String smsNumber = null;
        String lwVersion = null;
        EnumSet<BindingMode> binding = null;
        Boolean queueMode = null;

        // Get object Links
        Link[] objectLinks;
        try {
            objectLinks = linkParser.parseCoreLinkFormat(coapRequest.getPayload().getBytes());
        } catch (LinkParseException e) {
            return handleInvalidRequest(coapRequest, e.getMessage() != null ? e.getMessage() : "Invalid Links", e);
        }

        Map<String, String> additionalParams = new HashMap<>();

        // Get parameters
        try {
            for (Entry<String, String> entry : coapRequest.options().getUriQueryMap().entrySet()) {
                if (entry.getKey().equals(QUERY_PARAM_ENDPOINT)) {
                    endpoint = entry.getValue();
                } else if (entry.getKey().equals(QUERY_PARAM_LIFETIME)) {
                    lifetime = Long.valueOf(entry.getValue());
                } else if (entry.getKey().equals(QUERY_PARAM_SMS)) {
                    smsNumber = entry.getValue();
                } else if (entry.getKey().equals(QUERY_PARAM_LWM2M_VERSION)) {
                    lwVersion = entry.getValue();
                } else if (entry.getKey().equals(QUERY_PARAM_BINDING_MODE)) {
                    binding = BindingMode.parse(entry.getValue());
                } else if (entry.getKey().equals(QUERY_PARAM_QUEUEMMODE)) {
                    queueMode = true;
                } else {
                    additionalParams.put(entry.getKey(), entry.getValue());
                }
            }
        } catch (NumberFormatException | ParseException e) {
            return handleInvalidRequest(coapRequest, e.getMessage() != null ? e.getMessage() : "Uri Query", e);
        }

        // Create request
        RegisterRequest registerRequest = new RegisterRequest(endpoint, lifetime, lwVersion, binding, queueMode,
                smsNumber, objectLinks, additionalParams, coapRequest);

        // Handle request
        // -------------------------------
        // TODO get endpoint URI waiting we hardcode it
        // URI endpointUsed = exchange.advanced().getEndpoint().getUri();

        final SendableResponse<RegisterResponse> sendableResponse = receiver.requestReceived(sender, null,
                registerRequest, endpointUsed);
        RegisterResponse response = sendableResponse.getResponse();

        // Create CoAP Response from LwM2m request
        // -------------------------------
        // TODO this should be called after.
        sendableResponse.sent();
        if (response.getCode() == org.eclipse.leshan.core.ResponseCode.CREATED) {
            CoapResponse coapResponse = CoapResponse.of(Code.C201_CREATED);
            coapResponse.options().setLocationPath(RESOURCE_NAME + "/" + response.getRegistrationID());
            return completedFuture(coapResponse);
        } else {
            return errorMessage(response.getCode(), response.getErrorMessage());
        }

    }

    protected CompletableFuture<CoapResponse> handleUpdate(CoapRequest coapRequest, String registrationId) {
        // Get identity
        Identity sender = getForeignPeerIdentity(coapRequest);

        // Create LwM2m request from CoAP request
        Long lifetime = null;
        String smsNumber = null;
        EnumSet<BindingMode> binding = null;
        Link[] objectLinks = null;
        Map<String, String> additionalParams = new HashMap<>();

        try {
            for (Entry<String, String> entry : coapRequest.options().getUriQueryMap().entrySet()) {
                if (entry.getKey().equals(QUERY_PARAM_LIFETIME)) {
                    lifetime = Long.valueOf(entry.getValue());
                } else if (entry.getKey().equals(QUERY_PARAM_SMS)) {
                    smsNumber = entry.getValue();
                } else if (entry.getKey().equals(QUERY_PARAM_BINDING_MODE)) {
                    binding = BindingMode.parse(entry.getValue());
                } else {
                    additionalParams.put(entry.getKey(), entry.getValue());
                }
            }
        } catch (NumberFormatException | ParseException e) {
            return handleInvalidRequest(coapRequest, e.getMessage() != null ? e.getMessage() : "Uri Query", e);
        }
        if (coapRequest.getPayload() != null && coapRequest.getPayload().size() > 0) {
            try {
                objectLinks = linkParser.parseCoreLinkFormat(coapRequest.getPayload().getBytes());
            } catch (LinkParseException e) {
                return handleInvalidRequest(coapRequest, e.getMessage() != null ? e.getMessage() : "Invalid Links", e);
            }
        }
        UpdateRequest updateRequest = new UpdateRequest(registrationId, lifetime, smsNumber, binding, objectLinks,
                additionalParams, coapRequest);

        // Handle request
        final SendableResponse<UpdateResponse> sendableResponse = receiver.requestReceived(sender, null, updateRequest,
                endpointUsed);
        UpdateResponse updateResponse = sendableResponse.getResponse();

        // Create CoAP Response from LwM2m request
        // TODO this should be called after.
        sendableResponse.sent();
        if (updateResponse.getCode().isError()) {
            return errorMessage(updateResponse.getCode(), updateResponse.getErrorMessage());
        } else {
            return completedFuture(CoapResponse.of(Code.valueOf(updateResponse.getCode().getCode())));
        }

    }

    protected CompletableFuture<CoapResponse> handleDeregister(CoapRequest coapRequest, String registrationId) {
        // TODO implement it

        // Get identity
//        Request coapRequest = exchange.advanced().getRequest();
//        Identity sender = getForeignPeerIdentity(exchange.advanced(), coapRequest);
//
//        // Create request
//        DeregisterRequest deregisterRequest = new DeregisterRequest(registrationId, coapRequest);
//
//        // Handle request
//        final SendableResponse<DeregisterResponse> sendableResponse = receiver.requestReceived(sender, null,
//                deregisterRequest, exchange.advanced().getEndpoint().getUri());
//        DeregisterResponse deregisterResponse = sendableResponse.getResponse();
//
//        // Create CoAP Response from LwM2m request
//        if (deregisterResponse.getCode().isError()) {
//            exchange.respond(toCoapResponseCode(deregisterResponse.getCode()), deregisterResponse.getErrorMessage());
//        } else {
//            exchange.respond(toCoapResponseCode(deregisterResponse.getCode()));
//        }
//        sendableResponse.sent();
        return completedFuture(CoapResponse.of(Code.C405_METHOD_NOT_ALLOWED));
    }

}
