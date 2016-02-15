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
import java.util.Date;

import org.eclipse.leshan.core.request.DeregisterRequest;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.request.RegisterRequest;
import org.eclipse.leshan.core.request.UpdateRequest;
import org.eclipse.leshan.core.response.DeregisterResponse;
import org.eclipse.leshan.core.response.RegisterResponse;
import org.eclipse.leshan.core.response.UpdateResponse;
import org.eclipse.leshan.server.client.Client;
import org.eclipse.leshan.server.client.ClientRegistry;
import org.eclipse.leshan.server.client.ClientUpdate;
import org.eclipse.leshan.server.security.SecurityCheck;
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

    public RegisterResponse register(Identity sender, RegisterRequest registerRequest, InetSocketAddress serverEndpoint) {

        if (registerRequest.getEndpointName() == null || registerRequest.getEndpointName().isEmpty() || sender == null) {
            return RegisterResponse.badRequest(null);
        }

        // We must check if the client is using the right identity.
        if (!isAuthorized(registerRequest.getEndpointName(), sender)) {
            return RegisterResponse.forbidden(null);
        }

        Client.Builder builder = new Client.Builder(RegistrationHandler.createRegistrationId(),
                registerRequest.getEndpointName(), sender.getPeerAddress().getAddress(), sender.getPeerAddress()
                        .getPort(), serverEndpoint);

        builder.lwM2mVersion(registerRequest.getLwVersion()).lifeTimeInSec(registerRequest.getLifetime())
                .bindingMode(registerRequest.getBindingMode()).objectLinks(registerRequest.getObjectLinks())
                .smsNumber(registerRequest.getSmsNumber()).registrationDate(new Date()).lastUpdate(new Date())
                .additionalRegistrationAttributes(registerRequest.getAdditionalAttributes());

        Client client = builder.build();

        if (clientRegistry.registerClient(client)) {
            LOG.debug("New registered client: {}", client);
            return RegisterResponse.success(client.getRegistrationId());
        } else {
            return RegisterResponse.forbidden(null);
        }
    }

    public UpdateResponse update(Identity sender, UpdateRequest updateRequest) {

        if (sender == null) {
            return UpdateResponse.badRequest(null);
        }

        // We must check if the client is using the right identity.
        Client client = clientRegistry.findByRegistrationId(updateRequest.getRegistrationId());
        if (client == null) {
            return UpdateResponse.notFound();
        }
        if (!isAuthorized(client.getEndpoint(), sender)) {
            return UpdateResponse.badRequest("forbidden");
        }

        client = clientRegistry.updateClient(new ClientUpdate(updateRequest.getRegistrationId(), sender
                .getPeerAddress().getAddress(), sender.getPeerAddress().getPort(), updateRequest.getLifeTimeInSec(),
                updateRequest.getSmsNumber(), updateRequest.getBindingMode(), updateRequest.getObjectLinks()));
        if (client == null) {
            return UpdateResponse.notFound();
        } else {
            return UpdateResponse.success();
        }
    }

    public DeregisterResponse deregister(Identity sender, DeregisterRequest deregisterRequest) {
        if (sender == null) {
            return DeregisterResponse.badRequest(null);
        }

        // We must check if the client is using the right identity.
        Client client = clientRegistry.findByRegistrationId(deregisterRequest.getRegistrationID());
        if (client == null) {
            return DeregisterResponse.notFound();
        }
        if (!isAuthorized(client.getEndpoint(), sender)) {
            return DeregisterResponse.badRequest("forbidden");
        }

        Client unregistered = clientRegistry.deregisterClient(deregisterRequest.getRegistrationID());
        if (unregistered != null) {
            return DeregisterResponse.success();
        } else {
            LOG.debug("Invalid deregistration");
            return DeregisterResponse.notFound();
        }
    }

    private static String createRegistrationId() {
        return RandomStringUtils.random(10, true, true);
    }

    /**
     * Return true if the client with the given lightweight M2M endPoint is authorized to communicate with the given
     * security parameters.
     * 
     * @param lwM2mEndPointName the lightweight M2M endPoint name
     * @param clientIdentity the identity at TLS level
     * @return true if device get authorization
     */
    private boolean isAuthorized(String lwM2mEndPointName, Identity clientIdentity) {
        // do we have security information for this client?
        SecurityInfo expectedSecurityInfo = securityStore.getByEndpoint(lwM2mEndPointName);
        return SecurityCheck.checkSecurityInfo(lwM2mEndPointName, clientIdentity, expectedSecurityInfo);
    }

}
