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
package org.eclipse.leshan.transport.javacoap.resource;

import static java.util.concurrent.CompletableFuture.completedFuture;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.peer.IpPeer;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.exception.InvalidRequestException;
import org.eclipse.leshan.transport.javacoap.identity.IdentityHandler;
import org.eclipse.leshan.transport.javacoap.request.ResponseCodeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mbed.coap.packet.CoapRequest;
import com.mbed.coap.packet.CoapResponse;
import com.mbed.coap.packet.CoapResponse.Builder;
import com.mbed.coap.packet.Code;
import com.mbed.coap.packet.MediaTypes;
import com.mbed.coap.packet.Opaque;
import com.mbed.coap.transport.TransportContext;
import com.mbed.coap.utils.Service;

public abstract class LwM2mCoapResource implements Service<CoapRequest, CoapResponse> {

    private static final Logger LOG = LoggerFactory.getLogger(LwM2mCoapResource.class);

    private final String uri;
    IdentityHandler identityHandler;

    public LwM2mCoapResource(String uri, IdentityHandler identityHandler) {
        this.uri = uri;
        this.identityHandler = identityHandler;
    }

    @Override
    public CompletableFuture<CoapResponse> apply(CoapRequest coapRequest) {
        try {
            // The LWM2M transport spec v1.1.1 (section 6.4) all operation must be confirmable message except notify and
            // execute which may be NON
            if (coapRequest.getTransContext().get(TransportContext.NON_CONFIRMABLE)) {
                return handleInvalidRequest(coapRequest, "CON CoAP type expected");
            }

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
        case FETCH:
            return handleFETCH(coapRequest);
        case PATCH:
            return handlePATCH(coapRequest);
        case iPATCH:
            return handleIPATCH(coapRequest);
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

    protected CompletableFuture<CoapResponse> handleFETCH(CoapRequest coapRequest) {
        return completedFuture(CoapResponse.of(Code.C405_METHOD_NOT_ALLOWED));
    }

    protected CompletableFuture<CoapResponse> handlePATCH(CoapRequest coapRequest) {
        return completedFuture(CoapResponse.of(Code.C405_METHOD_NOT_ALLOWED));
    }

    protected CompletableFuture<CoapResponse> handleIPATCH(CoapRequest coapRequest) {
        return completedFuture(CoapResponse.of(Code.C405_METHOD_NOT_ALLOWED));
    }

    protected String getURI() {
        return uri;
    }

    protected IpPeer getForeignPeerIdentity(CoapRequest coapRequest) {
        return (IpPeer) identityHandler.getIdentity(coapRequest);
    }

    protected IpPeer extractIdentitySafely(CoapRequest coapRequest) {
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
     * @param coapRequest The invalid CoAP request
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

        Builder coapResponseBuilder = CoapResponse.coapResponse(Code.C400_BAD_REQUEST);
        if (message != null) {
            coapResponseBuilder.payload(Opaque.of(message));
            coapResponseBuilder.contentFormat(MediaTypes.CT_TEXT_PLAIN);
        }
        return completedFuture(coapResponseBuilder.build());
    }

    protected CompletableFuture<CoapResponse> errorMessage(ResponseCode errorCode, String message) {
        Builder coapResponseBuilder = CoapResponse.coapResponse(ResponseCodeUtil.toCoapResponseCode(errorCode));
        if (message != null) {
            coapResponseBuilder //
                    .payload(Opaque.of(message)) //
                    .contentFormat(MediaTypes.CT_TEXT_PLAIN);
        }
        return completedFuture(coapResponseBuilder.build());
    }

    public CompletableFuture<CoapResponse> responseWithPayload(ResponseCode code, ContentFormat format,
            byte[] payload) {
        return completedFuture(CoapResponse //
                .coapResponse(ResponseCodeUtil.toCoapResponseCode(code)) //
                .payload(Opaque.of(payload)) //
                .contentFormat((short) format.getCode()) //
                .build());
    }

    public CompletableFuture<CoapResponse> emptyResponse(ResponseCode code) {
        return completedFuture(CoapResponse.of(ResponseCodeUtil.toCoapResponseCode(code)));
    }

    protected List<String> getUriPart(CoapRequest coapRequest) {
        String uriAsString = coapRequest.options().getUriPath();
        if (uriAsString == null) {
            return null;
        }
        // remove first '/'
        if (uriAsString.startsWith("/")) {
            uriAsString = uriAsString.substring(1);
        }
        List<String> uri = Arrays.asList(uriAsString.split("/"));
        return uri;
    }
}
