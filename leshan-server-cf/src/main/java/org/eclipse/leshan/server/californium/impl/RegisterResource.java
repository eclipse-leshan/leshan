/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
package org.eclipse.leshan.server.californium.impl;

import static org.eclipse.leshan.core.californium.EndpointContextUtil.extractIdentity;
import static org.eclipse.leshan.core.californium.ResponseCodeUtil.toCoapResponseCode;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.coap.CoAP.Type;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.core.server.resources.Resource;
import org.eclipse.leshan.Link;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.request.DeregisterRequest;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.request.RegisterRequest;
import org.eclipse.leshan.core.request.UpdateRequest;
import org.eclipse.leshan.core.request.exception.InvalidRequestException;
import org.eclipse.leshan.core.response.DeregisterResponse;
import org.eclipse.leshan.core.response.RegisterResponse;
import org.eclipse.leshan.core.response.SendableResponse;
import org.eclipse.leshan.core.response.UpdateResponse;
import org.eclipse.leshan.server.registration.RegistrationHandler;
import org.eclipse.leshan.server.registration.RegistrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A CoAP {@link Resource} in charge of handling clients registration requests.
 * <p>
 * This resource is the entry point of the Resource Directory ("/rd"). Each new client is added to the
 * {@link RegistrationService}.
 * </p>
 */
public class RegisterResource extends CoapResource {

    private static final String QUERY_PARAM_ENDPOINT = "ep=";

    private static final String QUERY_PARAM_BINDING_MODE = "b=";

    private static final String QUERY_PARAM_LWM2M_VERSION = "lwm2m=";

    private static final String QUERY_PARAM_SMS = "sms=";

    private static final String QUERY_PARAM_LIFETIME = "lt=";

    private static final Logger LOG = LoggerFactory.getLogger(RegisterResource.class);

    public static final String RESOURCE_NAME = "rd";

    private final RegistrationHandler registrationHandler;

    public RegisterResource(RegistrationHandler registrationHandler) {
        super(RESOURCE_NAME);

        this.registrationHandler = registrationHandler;
        getAttributes().addResourceType("core.rd");
    }

    @Override
    public void handleRequest(Exchange exchange) {
        try {
            super.handleRequest(exchange);
        } catch (InvalidRequestException e) {
            LOG.debug("InvalidRequestException while handling request({}) on the /rd resource", exchange.getRequest(),
                    e);
            Response response = new Response(ResponseCode.BAD_REQUEST);
            response.setPayload(e.getMessage());
            exchange.sendResponse(response);
        } catch (RuntimeException e) {
            LOG.error("Exception while handling request({}) on the /rd resource", exchange.getRequest(), e);
            exchange.sendResponse(new Response(ResponseCode.INTERNAL_SERVER_ERROR));
        }
    }

    @Override
    public void handlePOST(CoapExchange exchange) {
        Request request = exchange.advanced().getRequest();

        LOG.debug("POST received : {}", request);

        // The LWM2M spec (section 8.2) mandates the usage of confirmable messages
        if (!Type.CON.equals(request.getType())) {
            exchange.respond(ResponseCode.BAD_REQUEST);
            return;
        }

        List<String> uri = exchange.getRequestOptions().getUriPath();
        if (uri == null || uri.size() == 0 || !RESOURCE_NAME.equals(uri.get(0))) {
            exchange.respond(ResponseCode.BAD_REQUEST);
            return;
        }

        if (uri.size() == 1) {
            handleRegister(exchange, request);
            return;
        } else if (uri.size() == 2) {
            handleUpdate(exchange, request, uri.get(1));
            return;
        } else {
            exchange.respond(ResponseCode.BAD_REQUEST);
            return;
        }
    }

    @Override
    public void handleDELETE(CoapExchange exchange) {
        LOG.debug("DELETE received : {}", exchange.advanced().getRequest());

        List<String> uri = exchange.getRequestOptions().getUriPath();

        if (uri != null && uri.size() == 2 && RESOURCE_NAME.equals(uri.get(0))) {
            handleDeregister(exchange, uri.get(1));
        } else {
            LOG.debug("Invalid deregistration");
            exchange.respond(ResponseCode.NOT_FOUND);
        }
    }

    private void handleRegister(CoapExchange exchange, Request request) {
        // Get identity
        // --------------------------------
        Identity sender = extractIdentity(request.getSourceContext());

        // Create LwM2m request from CoAP request
        // --------------------------------
        // We don't check content media type is APPLICATION LINK FORMAT for now as this is the only format we can expect
        String endpoint = null;
        Long lifetime = null;
        String smsNumber = null;
        String lwVersion = null;
        BindingMode binding = null;

        // Get object Links
        Link[] objectLinks = Link.parse(request.getPayload());

        Map<String, String> additionalParams = new HashMap<>();

        // Get parameters
        for (String param : request.getOptions().getUriQuery()) {
            if (param.startsWith(QUERY_PARAM_ENDPOINT)) {
                endpoint = param.substring(3);
            } else if (param.startsWith(QUERY_PARAM_LIFETIME)) {
                lifetime = Long.valueOf(param.substring(3));
            } else if (param.startsWith(QUERY_PARAM_SMS)) {
                smsNumber = param.substring(4);
            } else if (param.startsWith(QUERY_PARAM_LWM2M_VERSION)) {
                lwVersion = param.substring(6);
            } else if (param.startsWith(QUERY_PARAM_BINDING_MODE)) {
                binding = BindingMode.valueOf(param.substring(2));
            } else {
                String[] tokens = param.split("\\=");
                if (tokens != null && tokens.length == 2) {
                    additionalParams.put(tokens[0], tokens[1]);
                }
            }
        }

        // Create request
        RegisterRequest registerRequest = new RegisterRequest(endpoint, lifetime, lwVersion, binding, smsNumber,
                objectLinks, additionalParams);

        // Handle request
        // -------------------------------
        InetSocketAddress serverEndpoint = exchange.advanced().getEndpoint().getAddress();
        final SendableResponse<RegisterResponse> sendableResponse = registrationHandler.register(sender,
                registerRequest, serverEndpoint);
        RegisterResponse response = sendableResponse.getResponse();

        // Create CoAP Response from LwM2m request
        // -------------------------------
        if (response.getCode() == org.eclipse.leshan.ResponseCode.CREATED) {
            exchange.setLocationPath(RESOURCE_NAME + "/" + response.getRegistrationID());
            exchange.respond(ResponseCode.CREATED);
        } else {
            exchange.respond(toCoapResponseCode(response.getCode()), response.getErrorMessage());
        }
        sendableResponse.sent();
    }

    private void handleUpdate(CoapExchange exchange, Request request, String registrationId) {
        // Get identity
        Identity sender = extractIdentity(request.getSourceContext());

        // Create LwM2m request from CoAP request
        Long lifetime = null;
        String smsNumber = null;
        BindingMode binding = null;
        Link[] objectLinks = null;
        Map<String, String> additionalParams = new HashMap<>();

        for (String param : request.getOptions().getUriQuery()) {
            if (param.startsWith(QUERY_PARAM_LIFETIME)) {
                lifetime = Long.valueOf(param.substring(3));
            } else if (param.startsWith(QUERY_PARAM_SMS)) {
                smsNumber = param.substring(4);
            } else if (param.startsWith(QUERY_PARAM_BINDING_MODE)) {
                binding = BindingMode.valueOf(param.substring(2));
            } else {
                String[] tokens = param.split("\\=");
                if (tokens != null && tokens.length == 2) {
                    additionalParams.put(tokens[0], tokens[1]);
                }
            }
        }
        if (request.getPayload() != null && request.getPayload().length > 0) {
            objectLinks = Link.parse(request.getPayload());
        }
        UpdateRequest updateRequest = new UpdateRequest(registrationId, lifetime, smsNumber, binding, objectLinks,
                additionalParams);

        // Handle request
        final SendableResponse<UpdateResponse> sendableResponse = registrationHandler.update(sender, updateRequest);
        UpdateResponse updateResponse = sendableResponse.getResponse();

        // Create CoAP Response from LwM2m request
        if (updateResponse.getCode().isError()) {
            exchange.respond(toCoapResponseCode(updateResponse.getCode()), updateResponse.getErrorMessage());
        } else {
            exchange.respond(toCoapResponseCode(updateResponse.getCode()));
        }
        sendableResponse.sent();
    }

    private void handleDeregister(CoapExchange exchange, String registrationId) {
        // Get identity
        Identity sender = extractIdentity(exchange.advanced().getRequest().getSourceContext());

        // Create request
        DeregisterRequest deregisterRequest = new DeregisterRequest(registrationId);

        // Handle request
        final SendableResponse<DeregisterResponse> sendableResponse = registrationHandler.deregister(sender,
                deregisterRequest);
        DeregisterResponse deregisterResponse = sendableResponse.getResponse();

        // Create CoAP Response from LwM2m request
        if (deregisterResponse.getCode().isError()) {
            exchange.respond(toCoapResponseCode(deregisterResponse.getCode()), deregisterResponse.getErrorMessage());
        } else {
            exchange.respond(toCoapResponseCode(deregisterResponse.getCode()));
        }
        sendableResponse.sent();
    }

    // Since the V1_0-20150615-D version of specification, the registration update should be a CoAP POST.
    // To keep compatibility with older version, we still accept CoAP PUT.
    // TODO remove this backward compatibility when the version 1.0.0 of the spec will be released.
    @Override
    public void handlePUT(CoapExchange exchange) {
        Request request = exchange.advanced().getRequest();

        LOG.debug("UPDATE received : {}", request);
        if (!Type.CON.equals(request.getType())) {
            exchange.respond(ResponseCode.BAD_REQUEST);
            return;
        }

        List<String> uri = exchange.getRequestOptions().getUriPath();
        if (uri == null || uri.size() != 2 || !RESOURCE_NAME.equals(uri.get(0))) {
            exchange.respond(ResponseCode.BAD_REQUEST);
            return;
        }

        LOG.debug(
                "Warning a client made a registration update using a CoAP PUT, a POST must be used since version V1_0-20150615-D of the specification. Request: {}",
                request);
        handleUpdate(exchange, request, uri.get(1));
    }

    /*
     * Override the default behavior so that requests to sub resources (typically /rd/{client-reg-id}) are handled by
     * /rd resource.
     */
    @Override
    public Resource getChild(String name) {
        return this;
    }
}
