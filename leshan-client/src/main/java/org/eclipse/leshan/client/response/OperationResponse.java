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
 *     Zebra Technologies - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.client.response;

import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.leshan.client.request.identifier.ClientIdentifier;

public abstract class OperationResponse {

    public abstract boolean isSuccess();

    public abstract String getErrorMessage();

    public abstract ResponseCode getResponseCode();

    public abstract byte[] getPayload();

    public abstract ClientIdentifier getClientIdentifier();

    public static OperationResponse of(final Response response, final ClientIdentifier clientIdentifier) {
        return new SuccessfulOperationResponse(response, clientIdentifier);
    }

    public static OperationResponse failure(final ResponseCode responseCode, final String errorMessage) {
        return new FailedOperationResponse(responseCode, errorMessage);
    }

    private static class SuccessfulOperationResponse extends OperationResponse {
        private final Response response;
        private final ClientIdentifier clientIdentifier;

        public SuccessfulOperationResponse(final Response response, final ClientIdentifier clientIdentifier) {
            this.response = response;
            this.clientIdentifier = clientIdentifier;
        }

        @Override
        public boolean isSuccess() {
            return true;
        }

        @Override
        public ResponseCode getResponseCode() {
            return response.getCode();
        }

        @Override
        public byte[] getPayload() {
            return response.getPayload();
        }

        @Override
        public String getErrorMessage() {
            throw new UnsupportedOperationException("Successful Operations do not have Error Messages.");
        }

        @Override
        public ClientIdentifier getClientIdentifier() {
            return clientIdentifier;
        }

    }

    private static class FailedOperationResponse extends OperationResponse {
        private final ResponseCode responseCode;
        private final String errorMessage;

        public FailedOperationResponse(final ResponseCode responseCode, final String errorMessage) {
            this.responseCode = responseCode;
            this.errorMessage = errorMessage;
        }

        @Override
        public boolean isSuccess() {
            return false;
        }

        @Override
        public ResponseCode getResponseCode() {
            return responseCode;
        }

        @Override
        public String getErrorMessage() {
            return errorMessage;
        }

        @Override
        public byte[] getPayload() {
            throw new UnsupportedOperationException("Failed Operations Do Not Have Payloads... for NOW...");
        }

        @Override
        public ClientIdentifier getClientIdentifier() {
            throw new UnsupportedOperationException("Failed Operations Do Not Have Location Paths... for NOW...");
        }

    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("Response[" + isSuccess() + "|" + getResponseCode() + "]");

        return builder.toString();
    }

}
