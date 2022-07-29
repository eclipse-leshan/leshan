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
package org.eclipse.leshan.server;

import java.net.URI;

import org.eclipse.leshan.core.request.BootstrapRequest;
import org.eclipse.leshan.core.request.DeregisterRequest;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.request.LwM2mRequest;
import org.eclipse.leshan.core.request.RegisterRequest;
import org.eclipse.leshan.core.request.SendRequest;
import org.eclipse.leshan.core.request.UpdateRequest;
import org.eclipse.leshan.core.request.UplinkRequest;
import org.eclipse.leshan.core.request.UplinkRequestVisitor;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.SendableResponse;
import org.eclipse.leshan.server.endpoint.ClientProfile;
import org.eclipse.leshan.server.endpoint.LwM2mRequestReceiver;
import org.eclipse.leshan.server.endpoint.PeerProfile;
import org.eclipse.leshan.server.registration.RegistrationHandler;
import org.eclipse.leshan.server.send.SendHandler;

public class ServerRequestReceiver implements LwM2mRequestReceiver {

    private final RegistrationHandler registrationHandler;
    private final SendHandler sendHandler;

    public ServerRequestReceiver(RegistrationHandler registrationHandler, SendHandler sendHandler) {
        this.registrationHandler = registrationHandler;
        this.sendHandler = sendHandler;
    }

    @Override
    public <T extends LwM2mResponse> SendableResponse<T> requestReceived(Identity identity,
            PeerProfile foreignPeerProfile, LwM2mRequest<T> lwm2mRequest, URI lwm2mEndpoint) {
        // check we get expected inputs
        ClientProfile clientProfile = assertIsClientProfile(foreignPeerProfile);
        UplinkRequest<? extends LwM2mResponse> downlinkRequest = assertIsUplinkRequest(lwm2mRequest);

        // handle request received;
        RequestHandler<T> requestHandler = new RequestHandler<T>(identity, clientProfile, lwm2mEndpoint);
        downlinkRequest.accept(requestHandler);
        return requestHandler.getResponse();
    }

    public class RequestHandler<T extends LwM2mResponse> implements UplinkRequestVisitor {

        private final Identity sender;
        private final ClientProfile profile;
        private final URI endpoint;
        private SendableResponse<? extends LwM2mResponse> response;

        public RequestHandler(Identity identity, ClientProfile clientProfile, URI lwm2mEndpoint) {
            this.sender = identity;
            this.profile = clientProfile;
            this.endpoint = lwm2mEndpoint;
        }

        @Override
        public void visit(RegisterRequest request) {
            response = registrationHandler.register(sender, request, endpoint);
        }

        @Override
        public void visit(UpdateRequest request) {
            response = registrationHandler.update(sender, request);

        }

        @Override
        public void visit(DeregisterRequest request) {
            response = registrationHandler.deregister(sender, request);
        }

        @Override
        public void visit(BootstrapRequest request) {
            // Not implemented.
        }

        @Override
        public void visit(SendRequest request) {
            response = sendHandler.handleSend(profile.getRegistration(), request);
        }

        @SuppressWarnings("unchecked")
        public SendableResponse<T> getResponse() {
            return (SendableResponse<T>) response;
        }
    }

    private ClientProfile assertIsClientProfile(PeerProfile foreignPeerProfile) {
        if (foreignPeerProfile == null) {
            return null;
        }

        if (!(foreignPeerProfile instanceof ClientProfile)) {
            throw new IllegalStateException(
                    String.format("Unable to handle %s, LWM2M server only support ClientProfile",
                            foreignPeerProfile.getClass().getSimpleName()));
        }
        return (ClientProfile) foreignPeerProfile;
    }

    private UplinkRequest<? extends LwM2mResponse> assertIsUplinkRequest(
            LwM2mRequest<? extends LwM2mResponse> lwm2mRequest) {
        if (!(lwm2mRequest instanceof UplinkRequest<?>)) {
            throw new IllegalStateException(
                    String.format("Unable to handle %s, LWM2M server only support UplinkRequest",
                            lwm2mRequest.getClass().getSimpleName()));
        }

        return (UplinkRequest<? extends LwM2mResponse>) lwm2mRequest;
    }

    @Override
    public void onError(Identity identity, PeerProfile profile, Exception e,
            Class<? extends LwM2mRequest<? extends LwM2mResponse>> requestType, URI lwm2mEndpoint) {
        ClientProfile clientProfile = assertIsClientProfile(profile);

        if (requestType.equals(SendRequest.class)) {
            sendHandler.onError(clientProfile.getRegistration(), e);
        }
    }
}
