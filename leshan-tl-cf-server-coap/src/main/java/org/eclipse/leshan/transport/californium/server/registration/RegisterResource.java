/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
 *     Michał Wadowski (Orange) - Improved compliance with rfc6690
 *******************************************************************************/
package org.eclipse.leshan.transport.californium.server.registration;

import static org.eclipse.leshan.transport.californium.ResponseCodeUtil.toCoapResponseCode;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.californium.core.CoapExchange;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.coap.CoAP.Type;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.option.StringOption;
import org.eclipse.californium.core.server.resources.Resource;
import org.eclipse.leshan.core.endpoint.EndPointUriHandler;
import org.eclipse.leshan.core.link.Link;
import org.eclipse.leshan.core.link.LinkParseException;
import org.eclipse.leshan.core.link.LinkParser;
import org.eclipse.leshan.core.peer.IpPeer;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.request.DeregisterRequest;
import org.eclipse.leshan.core.request.RegisterRequest;
import org.eclipse.leshan.core.request.UpdateRequest;
import org.eclipse.leshan.core.response.DeregisterResponse;
import org.eclipse.leshan.core.response.RegisterResponse;
import org.eclipse.leshan.core.response.SendableResponse;
import org.eclipse.leshan.core.response.UpdateResponse;
import org.eclipse.leshan.server.registration.RegistrationService;
import org.eclipse.leshan.server.request.UplinkDeviceManagementRequestReceiver;
import org.eclipse.leshan.transport.californium.LwM2mCoapResource;
import org.eclipse.leshan.transport.californium.identity.IdentityHandlerProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A CoAP {@link Resource} in charge of handling clients registration requests.
 * <p>
 * This resource is the entry point of the Resource Directory ("/rd"). Each new client is added to the
 * {@link RegistrationService}.
 * </p>
 */
public class RegisterResource extends LwM2mCoapResource {

    private static final String QUERY_PARAM_ENDPOINT = "ep";

    private static final String QUERY_PARAM_BINDING_MODE = "b";

    private static final String QUERY_PARAM_LWM2M_VERSION = "lwm2m";

    private static final String QUERY_PARAM_SMS = "sms";

    private static final String QUERY_PARAM_LIFETIME = "lt";

    private static final String QUERY_PARAM_QUEUEMMODE = "Q"; // since LWM2M 1.1

    private static final Logger LOG = LoggerFactory.getLogger(RegisterResource.class);

    public static final String RESOURCE_NAME = "rd";

    private final UplinkDeviceManagementRequestReceiver receiver;
    private final LinkParser linkParser;
    private final EndPointUriHandler uriHandler;

    public RegisterResource(UplinkDeviceManagementRequestReceiver receiver, LinkParser linkParser,
            IdentityHandlerProvider identityHandlerProvider, EndPointUriHandler uriHandler) {
        super(RESOURCE_NAME, identityHandlerProvider);

        this.receiver = receiver;
        this.linkParser = linkParser;
        this.uriHandler = uriHandler;
        getAttributes().addResourceType("core.rd");
    }

    @Override
    public void handlePOST(CoapExchange exchange) {
        Request request = exchange.advanced().getRequest();
        LOG.trace("POST received : {}", request);

        // The LWM2M spec (section 8.2) mandates the usage of confirmable messages
        if (!Type.CON.equals(request.getType())) {
            handleInvalidRequest(exchange, "CON CoAP type expected");
            return;
        }

        List<StringOption> uri = exchange.getRequestOptions().getUriPath();
        if (uri == null || uri.isEmpty() || !RESOURCE_NAME.equals(uri.get(0).getStringValue())) {
            handleInvalidRequest(exchange, "Bad URI");
            return;
        }

        if (uri.size() == 1) {
            handleRegister(exchange, request);
            return;
        } else if (uri.size() == 2) {
            handleUpdate(exchange, request, uri.get(1).getStringValue());
            return;
        } else {
            handleInvalidRequest(exchange, "Bad URI");
            return;
        }
    }

    @Override
    public void handleDELETE(CoapExchange exchange) {
        LOG.trace("DELETE received : {}", exchange.advanced().getRequest());

        List<StringOption> uri = exchange.getRequestOptions().getUriPath();

        if (uri != null && uri.size() == 2 && RESOURCE_NAME.equals(uri.get(0).getStringValue())) {
            handleDeregister(exchange, uri.get(1).getStringValue());
        } else {
            handleInvalidRequest(exchange, "Bad URI");
        }
    }

    protected void handleRegister(CoapExchange exchange, Request request) {
        // Get identity
        // --------------------------------
        IpPeer sender = getForeignPeerIdentity(exchange.advanced(), request);

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
            objectLinks = linkParser.parseCoreLinkFormat(request.getPayload());
        } catch (LinkParseException e) {
            handleInvalidRequest(exchange.advanced(), e.getMessage() != null ? e.getMessage() : "Invalid Links", e);
            return;
        }

        Map<String, String> additionalParams = new HashMap<>();

        // Get parameters
        // TODO maybe we should use LwM2mAttributeParser ?
        for (String param : request.getOptions().getUriQueryStrings()) {
            String[] p = param.split("=", 2);
            String paramName = p[0];
            if (paramName.equals(QUERY_PARAM_ENDPOINT)) {
                if (p.length == 2) {
                    endpoint = p[1];
                } else {
                    endpoint = "";
                }
            } else if (paramName.equals(QUERY_PARAM_LIFETIME)) {
                if (p.length == 2) {
                    lifetime = Long.valueOf(p[1]);
                } else {
                    handleInvalidRequest(exchange.advanced(), "lifetime (lt) param can not be empty", null);
                }
            } else if (paramName.equals(QUERY_PARAM_SMS)) {
                if (p.length == 2) {
                    smsNumber = p[1];
                } else {
                    smsNumber = "";
                }
                smsNumber = param.substring(4);
            } else if (paramName.equals(QUERY_PARAM_LWM2M_VERSION)) {
                if (p.length == 2) {
                    lwVersion = p[1];
                } else {
                    lwVersion = "";
                }
            } else if (paramName.equals(QUERY_PARAM_BINDING_MODE)) {
                if (p.length == 2) {
                    binding = BindingMode.parse(p[1]);
                } else {
                    binding = EnumSet.noneOf(BindingMode.class);
                }
            } else if (paramName.equals(QUERY_PARAM_QUEUEMMODE)) {
                if (p.length == 2) {
                    handleInvalidRequest(exchange.advanced(), "queue param (Q) most not have value", null);
                } else {
                    queueMode = true;
                }

            } else {
                String[] tokens = param.split("=", 2);
                if (tokens != null && tokens.length == 2) {
                    additionalParams.put(tokens[0], tokens[1]);
                }
            }
        }
        // Create request
        Request coapRequest = exchange.advanced().getRequest();
        RegisterRequest registerRequest = new RegisterRequest(endpoint, lifetime, lwVersion, binding, queueMode,
                smsNumber, objectLinks, additionalParams, coapRequest);

        // Handle request
        // -------------------------------
        final SendableResponse<RegisterResponse> sendableResponse = receiver.requestReceived(sender, null,
                registerRequest, uriHandler.createUri(exchange.advanced().getEndpoint().getUri()));
        RegisterResponse response = sendableResponse.getResponse();

        // Create CoAP Response from LwM2m request
        // -------------------------------
        if (response.getCode() == org.eclipse.leshan.core.ResponseCode.CREATED) {
            exchange.setLocationPath(RESOURCE_NAME + "/" + response.getRegistrationID());
            exchange.respond(ResponseCode.CREATED);
        } else {
            exchange.respond(toCoapResponseCode(response.getCode()), response.getErrorMessage());
        }
        sendableResponse.sent();
    }

    protected void handleUpdate(CoapExchange exchange, Request request, String registrationId) {
        // Get identity
        IpPeer sender = getForeignPeerIdentity(exchange.advanced(), request);

        // Create LwM2m request from CoAP request
        Long lifetime = null;
        String smsNumber = null;
        EnumSet<BindingMode> binding = null;
        Link[] objectLinks = null;
        Map<String, String> additionalParams = new HashMap<>();

        for (String param : request.getOptions().getUriQueryStrings()) {
            String[] p = param.split("=", 2);
            String paramName = p[0];
            if (paramName.equals(QUERY_PARAM_LIFETIME)) {
                if (p.length == 2) {
                    lifetime = Long.valueOf(p[1]);
                } else {
                    handleInvalidRequest(exchange.advanced(), "lifetime (lt) param can not be empty", null);
                }
            } else if (paramName.equals(QUERY_PARAM_SMS)) {
                if (p.length == 2) {
                    smsNumber = p[1];
                } else {
                    smsNumber = "";
                }
                smsNumber = param.substring(4);
            } else if (paramName.equals(QUERY_PARAM_BINDING_MODE)) {
                if (p.length == 2) {
                    binding = BindingMode.parse(p[1]);
                } else {
                    binding = EnumSet.noneOf(BindingMode.class);
                }
            } else {
                String[] tokens = param.split("\\=");
                if (tokens != null && tokens.length == 2) {
                    additionalParams.put(tokens[0], tokens[1]);
                }
            }
        }
        if (request.getPayload() != null && request.getPayload().length > 0) {
            try {
                objectLinks = linkParser.parseCoreLinkFormat(request.getPayload());
            } catch (LinkParseException e) {
                handleInvalidRequest(exchange.advanced(), e.getMessage() != null ? e.getMessage() : "Invalid Links", e);
                return;
            }
        }
        Request coapRequest = exchange.advanced().getRequest();
        UpdateRequest updateRequest = new UpdateRequest(registrationId, lifetime, smsNumber, binding, objectLinks,
                additionalParams, coapRequest);

        // Handle request
        final SendableResponse<UpdateResponse> sendableResponse = receiver.requestReceived(sender, null, updateRequest,
                uriHandler.createUri(exchange.advanced().getEndpoint().getUri()));
        UpdateResponse updateResponse = sendableResponse.getResponse();

        // Create CoAP Response from LwM2m request
        if (updateResponse.getCode().isError()) {
            exchange.respond(toCoapResponseCode(updateResponse.getCode()), updateResponse.getErrorMessage());
        } else {
            exchange.respond(toCoapResponseCode(updateResponse.getCode()));
        }
        sendableResponse.sent();
    }

    protected void handleDeregister(CoapExchange exchange, String registrationId) {
        // Get identity
        Request coapRequest = exchange.advanced().getRequest();
        IpPeer sender = getForeignPeerIdentity(exchange.advanced(), coapRequest);

        // Create request
        DeregisterRequest deregisterRequest = new DeregisterRequest(registrationId, coapRequest);

        // Handle request
        final SendableResponse<DeregisterResponse> sendableResponse = receiver.requestReceived(sender, null,
                deregisterRequest, uriHandler.createUri(exchange.advanced().getEndpoint().getUri()));
        DeregisterResponse deregisterResponse = sendableResponse.getResponse();

        // Create CoAP Response from LwM2m request
        if (deregisterResponse.getCode().isError()) {
            exchange.respond(toCoapResponseCode(deregisterResponse.getCode()), deregisterResponse.getErrorMessage());
        } else {
            exchange.respond(toCoapResponseCode(deregisterResponse.getCode()));
        }
        sendableResponse.sent();
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
