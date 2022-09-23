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
package org.eclipse.leshan.server.request;

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
import org.eclipse.leshan.server.profile.ClientProfile;
import org.eclipse.leshan.server.registration.RegistrationHandler;
import org.eclipse.leshan.server.send.SendHandler;

public class DefaultUplinkRequestReceiver implements UplinkRequestReceiver {

    private final RegistrationHandler registrationHandler;
    private final SendHandler sendHandler;

    public DefaultUplinkRequestReceiver(RegistrationHandler registrationHandler, SendHandler sendHandler) {
        this.registrationHandler = registrationHandler;
        this.sendHandler = sendHandler;
    }

    @Override
    public void onError(Identity senderIdentity, ClientProfile senderProfile, Exception exception,
            Class<? extends UplinkRequest<? extends LwM2mResponse>> requestType, URI serverEndpointUri) {
        if (requestType.equals(SendRequest.class)) {
            sendHandler.onError(senderProfile.getRegistration(), exception);
        }
    }

    @Override
    public <T extends LwM2mResponse> SendableResponse<T> requestReceived(Identity senderIdentity,
            ClientProfile senderProfile, UplinkRequest<T> request, URI serverEndpointUri) {

        RequestHandler<T> requestHandler = new RequestHandler<T>(senderIdentity, senderProfile, serverEndpointUri);
        request.accept(requestHandler);
        return requestHandler.getResponse();
    }

    public class RequestHandler<T extends LwM2mResponse> implements UplinkRequestVisitor {

        private final Identity senderIdentity;
        private final ClientProfile senderProfile;
        private final URI endpoint;
        private SendableResponse<? extends LwM2mResponse> response;

        public RequestHandler(Identity senderIdentity, ClientProfile clientProfile, URI serverEndpointUri) {
            this.senderIdentity = senderIdentity;
            this.senderProfile = clientProfile;
            this.endpoint = serverEndpointUri;
        }

        @Override
        public void visit(RegisterRequest request) {
            response = registrationHandler.register(senderIdentity, request, endpoint);
        }

        @Override
        public void visit(UpdateRequest request) {
            response = registrationHandler.update(senderIdentity, request);

        }

        @Override
        public void visit(DeregisterRequest request) {
            response = registrationHandler.deregister(senderIdentity, request);
        }

        @Override
        public void visit(BootstrapRequest request) {
            // Not implemented.
        }

        @Override
        public void visit(SendRequest request) {
            response = sendHandler.handleSend(senderProfile.getRegistration(), request);
        }

        @SuppressWarnings("unchecked")
        public SendableResponse<T> getResponse() {
            return (SendableResponse<T>) response;
        }
    }

}
