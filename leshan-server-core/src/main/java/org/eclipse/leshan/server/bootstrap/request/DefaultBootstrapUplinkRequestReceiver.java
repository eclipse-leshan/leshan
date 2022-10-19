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
package org.eclipse.leshan.server.bootstrap.request;

import java.net.URI;

import org.eclipse.leshan.core.request.BootstrapRequest;
import org.eclipse.leshan.core.request.DeregisterRequest;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.request.RegisterRequest;
import org.eclipse.leshan.core.request.SendRequest;
import org.eclipse.leshan.core.request.UpdateRequest;
import org.eclipse.leshan.core.request.UplinkRequest;
import org.eclipse.leshan.core.request.UplinkRequestVisitor;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.SendableResponse;
import org.eclipse.leshan.server.bootstrap.BootstrapHandler;

public class DefaultBootstrapUplinkRequestReceiver implements BootstrapUplinkRequestReceiver {

    private final BootstrapHandler bootstapHandler;

    public DefaultBootstrapUplinkRequestReceiver(BootstrapHandler bootstapHandler) {
        this.bootstapHandler = bootstapHandler;
    }

    @Override
    public void onError(Identity senderIdentity, Exception exception,
            Class<? extends UplinkRequest<? extends LwM2mResponse>> requestType, URI serverEndpointUri) {
    }

    @Override
    public <T extends LwM2mResponse> SendableResponse<T> requestReceived(Identity senderIdentity,
            UplinkRequest<T> request, URI serverEndpointUri) {

        RequestHandler<T> requestHandler = new RequestHandler<T>(senderIdentity, serverEndpointUri);
        request.accept(requestHandler);
        return requestHandler.getResponse();
    }

    public class RequestHandler<T extends LwM2mResponse> implements UplinkRequestVisitor {

        private final Identity senderIdentity;
        private final URI serverEndpointUri;
        private SendableResponse<? extends LwM2mResponse> response;

        public RequestHandler(Identity senderIdentity, URI serverEndpointUri) {
            this.senderIdentity = senderIdentity;
            this.serverEndpointUri = serverEndpointUri;
        }

        @Override
        public void visit(RegisterRequest request) {
        }

        @Override
        public void visit(UpdateRequest request) {

        }

        @Override
        public void visit(DeregisterRequest request) {
        }

        @Override
        public void visit(BootstrapRequest request) {
            response = bootstapHandler.bootstrap(senderIdentity, request, serverEndpointUri);
        }

        @Override
        public void visit(SendRequest request) {
        }

        @SuppressWarnings("unchecked")
        public SendableResponse<T> getResponse() {
            return (SendableResponse<T>) response;
        }
    }

}
