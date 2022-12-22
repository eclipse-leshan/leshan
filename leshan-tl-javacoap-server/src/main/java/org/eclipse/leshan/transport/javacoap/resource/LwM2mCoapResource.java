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

import java.util.concurrent.CompletableFuture;

import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.request.exception.InvalidRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mbed.coap.packet.CoapRequest;
import com.mbed.coap.packet.CoapResponse;
import com.mbed.coap.packet.Code;
import com.mbed.coap.packet.MediaTypes;
import com.mbed.coap.packet.Opaque;
import com.mbed.coap.utils.Service;

public abstract class LwM2mCoapResource implements Service<CoapRequest, CoapResponse> {

    private static final Logger LOG = LoggerFactory.getLogger(LwM2mCoapResource.class);

    private final String uri;

    public LwM2mCoapResource(String uri) {
        this.uri = uri;
    }

    @Override
    public CompletableFuture<CoapResponse> apply(CoapRequest coapRequest) {
        try {
            return handleRequest(coapRequest);
        } catch (InvalidRequestException e) {
            return handleInvalidRequest(coapRequest, e.getMessage(), e);
        } catch (RuntimeException e) {
            LOG.error("Exception while handling request [{}] on the resource {} from {}", coapRequest, getURI(),
                    extractIdentitySafely(coapRequest), e);
            return completedFuture(CoapResponse.of(Code.C500_INTERNAL_SERVER_ERROR));
        }
    }

    protected CompletableFuture<CoapResponse> handleRequest(CoapRequest coapRequest) {
        switch (coapRequest.getMethod()) {
        case GET:
            return handleGET(coapRequest);
        case POST:
            return handlePOST(coapRequest);
        case PUT:
            return handlePUT(coapRequest);
        case DELETE:
            return handleDELETE(coapRequest);
        default:
            return completedFuture(CoapResponse.of(Code.C500_INTERNAL_SERVER_ERROR,
                    String.format("supported Method %s", coapRequest.getMethod())));
        }
    }

    protected CompletableFuture<CoapResponse> handleDELETE(CoapRequest coapRequest) {
        return completedFuture(CoapResponse.of(Code.C405_METHOD_NOT_ALLOWED));
    }

    protected CompletableFuture<CoapResponse> handlePUT(CoapRequest coapRequest) {
        return completedFuture(CoapResponse.of(Code.C405_METHOD_NOT_ALLOWED));
    }

    protected CompletableFuture<CoapResponse> handlePOST(CoapRequest coapRequest) {
        return completedFuture(CoapResponse.of(Code.C405_METHOD_NOT_ALLOWED));
    }

    protected CompletableFuture<CoapResponse> handleGET(CoapRequest coapRequest) {
        return completedFuture(CoapResponse.of(Code.C405_METHOD_NOT_ALLOWED));
    }

    protected String getURI() {
        return uri;
    }

    protected Identity getForeignPeerIdentity(CoapRequest coapRequest) {
        return Identity.unsecure(coapRequest.getPeerAddress());

    }

    protected Identity extractIdentitySafely(CoapRequest coapRequest) {
        try {
            return getForeignPeerIdentity(coapRequest);
        } catch (RuntimeException e) {
            LOG.error("Unable to extract identity", e);
            return null;
        }
    }

    /**
     * Handle an Invalid Request by sending a BAD_REQUEST response and logging the error using debug level.
     *
     * @param CoapRequest The invalid CoAP request
     * @param message The error message describing why the request is invalid.
     */
    protected CompletableFuture<CoapResponse> handleInvalidRequest(CoapRequest coapRequest, String message) {
        return handleInvalidRequest(coapRequest, message, null);
    }

    /**
     * Handle an Invalid Request by sending a BAD_REQUEST response and logging the error using debug level.
     *
     * @param coapRequest The invalid CoAP request
     * @param message The error message describing why the request is invalid.
     * @param error An {@link Throwable} raised while we handle try create a LWM2M request from CoAP request.
     */
    protected CompletableFuture<CoapResponse> handleInvalidRequest(CoapRequest coapRequest, String message,
            Throwable error) {

        // Log error
        if (LOG.isDebugEnabled()) {
            if (error != null) {
                LOG.debug("Invalid request [{}] received on the resource rd from {}", coapRequest, getURI(),
                        extractIdentitySafely(coapRequest), error);
            } else {
                LOG.debug("Invalid request [{}] received on the resource rd from {} : {}", coapRequest, getURI(),
                        extractIdentitySafely(coapRequest), message);
            }
        }

        CoapResponse coapResponse = CoapResponse.of(Code.C400_BAD_REQUEST);
        if (message != null) {
            coapResponse.payload(Opaque.of(message));
            coapResponse.options().setContentFormat(MediaTypes.CT_TEXT_PLAIN);
        }
        return completedFuture(coapResponse);
    }

    protected CompletableFuture<CoapResponse> errorMessage(ResponseCode errorCode, String message) {
        CoapResponse coapResponse = CoapResponse.of(Code.valueOf(errorCode.getCode()));
        if (message != null) {
            coapResponse.payload(Opaque.of(message));
            coapResponse.options().setContentFormat(MediaTypes.CT_TEXT_PLAIN);
        }
        return completedFuture(coapResponse);
    }
}
