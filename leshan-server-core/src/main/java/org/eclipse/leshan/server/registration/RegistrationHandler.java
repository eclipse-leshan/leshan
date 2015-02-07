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
package org.eclipse.leshan.server.registration;

import java.net.InetSocketAddress;

import org.eclipse.leshan.ResponseCode;
import org.eclipse.leshan.core.request.DeregisterRequest;
import org.eclipse.leshan.core.request.RegisterRequest;
import org.eclipse.leshan.core.request.UpdateRequest;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.RegisterResponse;
import org.eclipse.leshan.server.client.Client;
import org.eclipse.leshan.server.client.ClientRegistry;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.eclipse.leshan.server.security.SecurityStore;
import org.eclipse.leshan.util.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handle the client registration logic. Check if the client is allowed to register, with the wanted security scheme.
 * Create the {@link Client} representing the registered client and add it to the {@link ClientRegistry}
 */
public class RegistrationHandler {

    private static final Logger LOG = LoggerFactory.getLogger(RegistrationHandler.class);

    private SecurityStore securityStore;
    private ClientRegistry clientRegistry;

    public RegistrationHandler(ClientRegistry clientRegistry, SecurityStore securityStore) {
        this.clientRegistry = clientRegistry;
        this.securityStore = securityStore;
    }

    public RegisterResponse register(RegisterRequest registerRequest) {

        if (registerRequest.getEndpointName() == null || registerRequest.getEndpointName().isEmpty()) {
            return new RegisterResponse(ResponseCode.BAD_REQUEST);
        } else {
            // register
            String registrationId = RegistrationHandler.createRegistrationId();

            // do we have security information for this client?
            SecurityInfo securityInfo = securityStore.getByEndpoint(registerRequest.getEndpointName());

            // which end point did the client post this request to?
            InetSocketAddress registrationEndpoint = registerRequest.getRegistrationEndpoint();

            // if this is a secure end-point, we must check that the registering client is using the right identity.
            if (registerRequest.isSecure()) {
                String pskIdentity = registerRequest.getPskIdentity();
                LOG.debug("Registration request received using the secure endpoint {} with identity {}",
                        registrationEndpoint, pskIdentity);

                if (securityInfo == null || pskIdentity == null || !pskIdentity.equals(securityInfo.getIdentity())) {
                    LOG.warn("Invalid identity for client {}: expected '{}' but was '{}'",
                            registerRequest.getEndpointName(),
                            securityInfo == null ? null : securityInfo.getIdentity(), pskIdentity);
                    return new RegisterResponse(ResponseCode.BAD_REQUEST);

                } else {
                    LOG.debug("authenticated client {} using DTLS PSK", registerRequest.getEndpointName());
                }
            } else {
                if (securityInfo != null) {
                    LOG.warn("client {} must connect using DTLS PSK", registerRequest.getEndpointName());
                    return new RegisterResponse(ResponseCode.BAD_REQUEST);
                }
            }

            Client client = new Client(registrationId, registerRequest.getEndpointName(),
                    registerRequest.getSourceAddress(), registerRequest.getSourcePort(),
                    registerRequest.getLwVersion(), registerRequest.getLifetime(), registerRequest.getSmsNumber(),
                    registerRequest.getBindingMode(), registerRequest.getObjectLinks(), registrationEndpoint);

            clientRegistry.registerClient(client);
            LOG.debug("New registered client: {}", client);

            return new RegisterResponse(ResponseCode.CREATED, client.getRegistrationId());
        }
    }

    public LwM2mResponse update(UpdateRequest updateRequest) {
        Client client = clientRegistry.updateClient(updateRequest);
        if (client == null) {
            return new LwM2mResponse(ResponseCode.NOT_FOUND);
        } else {
            return new LwM2mResponse(ResponseCode.CHANGED);
        }
    }

    public LwM2mResponse deregister(DeregisterRequest deregisterRequest) {
        Client unregistered = clientRegistry.deregisterClient(deregisterRequest.getRegistrationID());
        if (unregistered != null) {
            return new LwM2mResponse(ResponseCode.DELETED);
        } else {
            LOG.debug("Invalid deregistration");
            return new LwM2mResponse(ResponseCode.NOT_FOUND);
        }
    }

    private static String createRegistrationId() {
        return RandomStringUtils.random(10, true, true);
    }
}
