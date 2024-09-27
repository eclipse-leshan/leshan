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
package org.eclipse.leshan.transport.javacoap.server.resource;

import static java.util.concurrent.CompletableFuture.completedFuture;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;

import org.eclipse.leshan.core.link.Link;
import org.eclipse.leshan.core.link.LinkParseException;
import org.eclipse.leshan.core.link.LinkParser;
import org.eclipse.leshan.core.peer.LwM2mPeer;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.request.DeregisterRequest;
import org.eclipse.leshan.core.request.RegisterRequest;
import org.eclipse.leshan.core.request.UpdateRequest;
import org.eclipse.leshan.core.response.DeregisterResponse;
import org.eclipse.leshan.core.response.RegisterResponse;
import org.eclipse.leshan.core.response.SendableResponse;
import org.eclipse.leshan.core.response.UpdateResponse;
import org.eclipse.leshan.server.request.UplinkDeviceManagementRequestReceiver;
import org.eclipse.leshan.transport.javacoap.identity.IdentityHandler;
import org.eclipse.leshan.transport.javacoap.request.ResponseCodeUtil;
import org.eclipse.leshan.transport.javacoap.resource.LwM2mCoapResource;
import org.eclipse.leshan.transport.javacoap.server.endpoint.EndpointUriProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mbed.coap.packet.CoapRequest;
import com.mbed.coap.packet.CoapResponse;
import com.mbed.coap.packet.Code;

public class RegistrationResource extends LwM2mCoapResource {

    private static final Logger LOG = LoggerFactory.getLogger(RegistrationResource.class);

    private static final String QUERY_PARAM_ENDPOINT = "ep";
    private static final String QUERY_PARAM_BINDING_MODE = "b";
    private static final String QUERY_PARAM_LWM2M_VERSION = "lwm2m";
    private static final String QUERY_PARAM_SMS = "sms";
    private static final String QUERY_PARAM_LIFETIME = "lt";
    private static final String QUERY_PARAM_QUEUEMMODE = "Q"; // since LWM2M 1.1

    public static final String RESOURCE_NAME = "rd";
    public static final String RESOURCE_URI = "/" + RESOURCE_NAME + "/*";

    private final UplinkDeviceManagementRequestReceiver receiver;
    private final LinkParser linkParser;
    private final EndpointUriProvider endpointUriProvider;

    public RegistrationResource(UplinkDeviceManagementRequestReceiver receiver, LinkParser linkParser,
            EndpointUriProvider endpointUriProvider, IdentityHandler identityHandler) {
        super(RESOURCE_URI, identityHandler);
        this.receiver = receiver;
        this.linkParser = linkParser;
        this.endpointUriProvider = endpointUriProvider;
    }

    @Override
    public CompletableFuture<CoapResponse> handlePOST(CoapRequest coapRequest) {
        LOG.trace("POST received : {}", coapRequest);

        // validate URI
        List<String> uri = getUriPart(coapRequest);
        if (uri == null || uri.size() == 0 || !RESOURCE_NAME.equals(uri.get(0))) {
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

        /// validate URI
        List<String> uri = getUriPart(coapRequest);
        if (uri != null && uri.size() == 2 && RESOURCE_NAME.equals(uri.get(0))) {
            return handleDeregister(coapRequest, uri.get(1));
        } else {
            return handleInvalidRequest(coapRequest, "Bad URI");
        }
    }

    protected CompletableFuture<CoapResponse> handleRegister(CoapRequest coapRequest) {
        // Get identity
        // --------------------------------
        LwM2mPeer sender = getForeignPeerIdentity(coapRequest);

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
                    endpoint = entry.getValue() == null ? "" : entry.getValue();
                } else if (entry.getKey().equals(QUERY_PARAM_LIFETIME)) {
                    lifetime = Long.valueOf(entry.getValue());
                } else if (entry.getKey().equals(QUERY_PARAM_SMS)) {
                    smsNumber = entry.getValue() == null ? "" : entry.getValue();
                } else if (entry.getKey().equals(QUERY_PARAM_LWM2M_VERSION)) {
                    lwVersion = entry.getValue() == null ? "" : entry.getValue();
                } else if (entry.getKey().equals(QUERY_PARAM_BINDING_MODE)) {
                    binding = BindingMode.parse(entry.getValue());
                } else if (entry.getKey().equals(QUERY_PARAM_QUEUEMMODE)) {
                    queueMode = true;
                } else {
                    additionalParams.put(entry.getKey(), entry.getValue());
                }
            }
        } catch (/* NumberFormatException | */ IllegalArgumentException e) {
            return handleInvalidRequest(coapRequest, e.getMessage() != null ? e.getMessage() : "Uri Query", e);
        }

        // Create request
        RegisterRequest registerRequest = new RegisterRequest(endpoint, lifetime, lwVersion, binding, queueMode,
                smsNumber, objectLinks, additionalParams, coapRequest);

        // Handle request
        // -------------------------------
        final SendableResponse<RegisterResponse> sendableResponse = receiver.requestReceived(sender, null,
                registerRequest, endpointUriProvider.getEndpointUri());
        RegisterResponse response = sendableResponse.getResponse();

        // Create CoAP Response from LwM2m request
        // -------------------------------
        // TODO this should be called once request is sent. (No java-coap API for this)
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
        LwM2mPeer sender = getForeignPeerIdentity(coapRequest);

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
                    smsNumber = entry.getValue() == null ? "" : entry.getValue();
                } else if (entry.getKey().equals(QUERY_PARAM_BINDING_MODE)) {
                    binding = BindingMode.parse(entry.getValue());
                } else {
                    additionalParams.put(entry.getKey(), entry.getValue());
                }
            }
        } catch (/* NumberFormatException | */ IllegalArgumentException e) {
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
                endpointUriProvider.getEndpointUri());
        UpdateResponse updateResponse = sendableResponse.getResponse();

        // Create CoAP Response from LwM2m request
        // TODO this should be called once request is sent. (No java-coap API for this)
        sendableResponse.sent();
        if (updateResponse.getCode().isError()) {
            return errorMessage(updateResponse.getCode(), updateResponse.getErrorMessage());
        } else {
            return completedFuture(CoapResponse.of(ResponseCodeUtil.toCoapResponseCode(updateResponse.getCode())));
        }

    }

    protected CompletableFuture<CoapResponse> handleDeregister(CoapRequest coapRequest, String registrationId) {
        // Get identity
        LwM2mPeer sender = getForeignPeerIdentity(coapRequest);

        // Create request
        DeregisterRequest deregisterRequest = new DeregisterRequest(registrationId, coapRequest);

        // Handle request
        final SendableResponse<DeregisterResponse> sendableResponse = receiver.requestReceived(sender, null,
                deregisterRequest, endpointUriProvider.getEndpointUri());
        DeregisterResponse deregisterResponse = sendableResponse.getResponse();

        // Create CoAP Response from LwM2m request
        // TODO this should be called once request is sent. (No java-coap API for this)
        sendableResponse.sent();
        if (deregisterResponse.getCode().isError()) {
            return errorMessage(deregisterResponse.getCode(), deregisterResponse.getErrorMessage());
        } else {
            return completedFuture(CoapResponse.of(ResponseCodeUtil.toCoapResponseCode(deregisterResponse.getCode())));
        }
    }
}
