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

import org.eclipse.leshan.core.endpoint.EndpointUri;
import org.eclipse.leshan.core.peer.LwM2mPeer;
import org.eclipse.leshan.core.request.DeregisterRequest;
import org.eclipse.leshan.core.request.RegisterRequest;
import org.eclipse.leshan.core.request.SendRequest;
import org.eclipse.leshan.core.request.UpdateRequest;
import org.eclipse.leshan.core.request.UplinkDeviceManagementRequest;
import org.eclipse.leshan.core.request.UplinkDeviceManagementRequestVisitor;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.SendableResponse;
import org.eclipse.leshan.server.profile.ClientProfile;
import org.eclipse.leshan.server.registration.RegistrationHandler;
import org.eclipse.leshan.server.send.SendHandler;

public class DefaultUplinkRequestReceiver implements UplinkDeviceManagementRequestReceiver {

    private final RegistrationHandler registrationHandler;
    private final SendHandler sendHandler;

    public DefaultUplinkRequestReceiver(RegistrationHandler registrationHandler, SendHandler sendHandler) {
        this.registrationHandler = registrationHandler;
        this.sendHandler = sendHandler;
    }

    @Override
    public void onError(LwM2mPeer sender, ClientProfile senderProfile, Exception exception,
            Class<? extends UplinkDeviceManagementRequest<? extends LwM2mResponse>> requestType,
            EndpointUri serverEndpointUri) {
        if (requestType.equals(SendRequest.class)) {
            sendHandler.onError(senderProfile.getRegistration(),
                    exception.getMessage() != null ? exception.getMessage() : null, exception);
        }
    }

    @Override
    public <T extends LwM2mResponse> SendableResponse<T> requestReceived(LwM2mPeer sender, ClientProfile senderProfile,
            UplinkDeviceManagementRequest<T> request, EndpointUri serverEndpointUri) {

        RequestHandler<T> requestHandler = new RequestHandler<T>(sender, senderProfile, serverEndpointUri);
        request.accept(requestHandler);
        return requestHandler.getResponse();
    }

    public class RequestHandler<T extends LwM2mResponse> implements UplinkDeviceManagementRequestVisitor {

        private final LwM2mPeer sender;
        private final ClientProfile senderProfile;
        private final EndpointUri endpointUri;
        private SendableResponse<? extends LwM2mResponse> response;

        public RequestHandler(LwM2mPeer sender, ClientProfile clientProfile, EndpointUri serverEndpointUri) {
            this.sender = sender;
            this.senderProfile = clientProfile;
            this.endpointUri = serverEndpointUri;
        }

        @Override
        public void visit(RegisterRequest request) {
            response = registrationHandler.register(sender, request, endpointUri);
        }

        @Override
        public void visit(UpdateRequest request) {
            response = registrationHandler.update(sender, request, endpointUri);

        }

        @Override
        public void visit(DeregisterRequest request) {
            response = registrationHandler.deregister(sender, request, endpointUri);
        }

        @Override
        public void visit(SendRequest request) {
            response = sendHandler.handleSend(sender, senderProfile.getRegistration(), request, endpointUri);
        }

        @SuppressWarnings("unchecked")
        public SendableResponse<T> getResponse() {
            return (SendableResponse<T>) response;
        }
    }

}
