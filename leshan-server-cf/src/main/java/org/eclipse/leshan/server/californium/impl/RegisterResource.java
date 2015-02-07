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

import java.net.InetSocketAddress;
import java.util.List;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.coap.CoAP.Type;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.core.server.resources.Resource;
import org.eclipse.leshan.LinkObject;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.request.DeregisterRequest;
import org.eclipse.leshan.core.request.RegisterRequest;
import org.eclipse.leshan.core.request.UpdateRequest;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.RegisterResponse;
import org.eclipse.leshan.server.client.ClientRegistry;
import org.eclipse.leshan.server.registration.RegistrationHandler;
import org.eclipse.leshan.util.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A CoAP {@link Resource} in charge of handling clients registration requests.
 * <p>
 * This resource is the entry point of the Resource Directory ("/rd"). Each new client is added to the
 * {@link ClientRegistry}.
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
    public void handlePOST(CoapExchange exchange) {
        Request request = exchange.advanced().getRequest();

        LOG.debug("POST received : {}", request);

        // The LW M2M spec (section 8.2) mandates the usage of Confirmable
        // messages
        if (!Type.CON.equals(request.getType())) {
            exchange.respond(ResponseCode.BAD_REQUEST);
            return;
        }

        // Create LwM2m request from CoAP request
        // --------------------------------
        // TODO: assert content media type is APPLICATION LINK FORMAT?
        String endpoint = null;
        Long lifetime = null;
        String smsNumber = null;
        String lwVersion = null;
        BindingMode binding = null;
        LinkObject[] objectLinks = null;
        // Get Params
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
            }
        }
        // Get object Links
        if (request.getPayload() != null) {
            objectLinks = LinkObject.parse(request.getPayload());
        }
        // Which end point did the client post this request to?
        InetSocketAddress registrationEndpoint = exchange.advanced().getEndpoint().getAddress();
        // Get Security info
        String pskIdentity = null;
        if (exchange.advanced().getEndpoint() instanceof SecureEndpoint)
            pskIdentity = ((SecureEndpoint) exchange.advanced().getEndpoint()).getPskIdentity(request);

        RegisterRequest registerRequest = new RegisterRequest(endpoint, lifetime, lwVersion, binding, smsNumber,
                objectLinks, request.getSource(), request.getSourcePort(), registrationEndpoint, pskIdentity);

        // Handle request
        // -------------------------------
        RegisterResponse response = registrationHandler.register(registerRequest);

        // Create CoAP Response from LwM2m request
        // -------------------------------
        if (response.getCode() == org.eclipse.leshan.ResponseCode.CREATED) {
            exchange.setLocationPath(RESOURCE_NAME + "/" + response.getRegistrationID());
            exchange.respond(ResponseCode.CREATED);
        } else {
            // TODO we lost specific message error with this refactoring
            // exchange.respond(fromLwM2mCode(response.getCode()),"error message");
            exchange.respond(fromLwM2mCode(response.getCode()));
            if (exchange.advanced().getEndpoint() instanceof SecureEndpoint) {

            }
        }
    }

    public static ResponseCode fromLwM2mCode(final org.eclipse.leshan.ResponseCode code) {
        Validate.notNull(code);

        switch (code) {
        case CREATED:
            return ResponseCode.CREATED;
        case DELETED:
            return ResponseCode.DELETED;
        case CHANGED:
            return ResponseCode.CHANGED;
        case CONTENT:
            return ResponseCode.CONTENT;
        case BAD_REQUEST:
            return ResponseCode.BAD_REQUEST;
        case UNAUTHORIZED:
            return ResponseCode.UNAUTHORIZED;
        case NOT_FOUND:
            return ResponseCode.NOT_FOUND;
        case METHOD_NOT_ALLOWED:
            return ResponseCode.METHOD_NOT_ALLOWED;
        default:
            // TODO how can we manage CONFLICT code ...
            // } else if (code == leshan.ResponseCode.CONFLICT) {
            // //return 137;
            // } else {
            throw new IllegalArgumentException("Invalid CoAP code for LWM2M response: " + code);
        }
    }

    /**
     * Updates an existing Client registration.
     *
     * @param exchange the CoAP request containing the updated registration properties
     */
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
            exchange.respond(ResponseCode.NOT_FOUND);
            return;
        }

        // Create LwM2m request from CoAP request
        // --------------------------------
        String registrationId = uri.get(1);
        Long lifetime = null;
        String smsNumber = null;
        BindingMode binding = null;
        LinkObject[] objectLinks = null;
        for (String param : request.getOptions().getUriQuery()) {
            if (param.startsWith(QUERY_PARAM_LIFETIME)) {
                lifetime = Long.valueOf(param.substring(3));
            } else if (param.startsWith(QUERY_PARAM_SMS)) {
                smsNumber = param.substring(4);
            } else if (param.startsWith(QUERY_PARAM_BINDING_MODE)) {
                binding = BindingMode.valueOf(param.substring(2));
            }
        }
        if (request.getPayload() != null && request.getPayload().length > 0) {
            objectLinks = LinkObject.parse(request.getPayload());
        }
        UpdateRequest updateRequest = new UpdateRequest(registrationId, request.getSource(), request.getSourcePort(),
                lifetime, smsNumber, binding, objectLinks);

        // Handle request
        // -------------------------------
        LwM2mResponse updateResponse = registrationHandler.update(updateRequest);

        // Create CoAP Response from LwM2m request
        // -------------------------------
        exchange.respond(fromLwM2mCode(updateResponse.getCode()));
    }

    @Override
    public void handleDELETE(CoapExchange exchange) {
        LOG.debug("DELETE received : {}", exchange.advanced().getRequest());

        List<String> uri = exchange.getRequestOptions().getUriPath();

        if (uri != null && uri.size() == 2 && RESOURCE_NAME.equals(uri.get(0))) {
            DeregisterRequest deregisterRequest = new DeregisterRequest(uri.get(1));
            LwM2mResponse deregisterResponse = registrationHandler.deregister(deregisterRequest);
            exchange.respond(fromLwM2mCode(deregisterResponse.getCode()));
        } else {
            LOG.debug("Invalid deregistration");
            exchange.respond(ResponseCode.NOT_FOUND);
        }
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
