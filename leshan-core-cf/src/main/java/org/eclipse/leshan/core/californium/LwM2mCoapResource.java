/*******************************************************************************
 * Copyright (c) 2019 Sierra Wireless and others.
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
package org.eclipse.leshan.core.californium;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.Message;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.elements.EndpointContext;
import org.eclipse.leshan.core.californium.identity.IdentityHandler;
import org.eclipse.leshan.core.californium.identity.IdentityHandlerProvider;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.request.exception.InvalidRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Common {@link CoapResource} used to handle LWM2M request.
 * <p>
 * It provides mainly features about error handling.
 */
public class LwM2mCoapResource extends CoapResource {

    private static final Logger LOG = LoggerFactory.getLogger(LwM2mCoapResource.class);

    private final IdentityHandlerProvider identityHandlerProvider;

    /**
     * @param name the resource name
     * @see CoapResource#CoapResource(String)
     */
    public LwM2mCoapResource(String name, IdentityHandlerProvider identityHandlerProvider) {
        super(name);
        this.identityHandlerProvider = identityHandlerProvider;
    }

    @Override
    public void handleRequest(Exchange exchange) {
        try {
            super.handleRequest(exchange);
        } catch (InvalidRequestException e) {
            handleInvalidRequest(exchange, e.getMessage(), e);
        } catch (RuntimeException e) {
            Request request = exchange.getRequest();
            LOG.error("Exception while handling request [{}] on the resource {} from {}", request, getURI(),
                    extractIdentitySafely(exchange, request), e);
            exchange.sendResponse(new Response(ResponseCode.INTERNAL_SERVER_ERROR));
        }
    }

    /**
     * Handle an Invalid Request by sending a BAD_REQUEST response and logging the error using debug level.
     *
     * @param exchange The CoAP exchange linked to the invalid request.
     * @param message The error message describing why the request is invalid.
     */
    protected void handleInvalidRequest(CoapExchange exchange, String message) {
        handleInvalidRequest(exchange.advanced(), message, null);
    }

    /**
     * Handle an Invalid Request by sending a BAD_REQUEST response and logging the error using debug level.
     *
     * @param exchange The exchange linked to the invalid request.
     * @param message The error message describing why the request is invalid.
     * @param error An {@link Throwable} raised while we handle try create a LWM2M request from CoAP request.
     */
    protected void handleInvalidRequest(Exchange exchange, String message, Throwable error) {
        Request request = exchange.getRequest();

        // Log error
        if (LOG.isDebugEnabled()) {
            if (error != null) {
                LOG.debug("Invalid request [{}] received on the resource {} from {}", request, getURI(),
                        extractIdentitySafely(exchange, request), error);
            } else {
                LOG.debug("Invalid request [{}] received on the resource {} from {} : {}", request, getURI(),
                        extractIdentitySafely(exchange, request), message);
            }
        }

        // Send Response
        Response response = new Response(ResponseCode.BAD_REQUEST);
        if (message != null) {
            response.setPayload(message);
            response.getOptions().setContentFormat(MediaTypeRegistry.TEXT_PLAIN);
        }
        exchange.sendResponse(response);
    }

    protected Identity getForeignPeerIdentity(Exchange exchange, Message receivedMessage) {
        IdentityHandler identityHandler = identityHandlerProvider.getIdentityHandler(exchange.getEndpoint());
        if (identityHandler != null) {
            return identityHandler.getIdentity(receivedMessage);
        }
        return null;
    }

    /**
     * Create Leshan {@link Identity} from Californium {@link EndpointContext}.
     *
     * @return The corresponding Leshan {@link Identity} or <code>null</code> if we didn't succeed to extract Identity.
     */
    protected Identity extractIdentitySafely(Exchange exchange, Message receivedMessage) {
        try {
            return getForeignPeerIdentity(exchange, receivedMessage);
        } catch (RuntimeException e) {
            LOG.error("Unable to extract identity", e);
            return null;
        }
    }
}
