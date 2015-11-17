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
import java.security.PublicKey;

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
import org.eclipse.leshan.server.security.SecurityInfo;
import org.eclipse.leshan.server.security.SecurityStore;
import org.eclipse.leshan.util.Hex;
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

        Client client = new Client(RegistrationHandler.createRegistrationId(), registerRequest.getEndpointName(),
                sender.getPeerAddress().getAddress(), sender.getPeerAddress().getPort(),
                registerRequest.getLwVersion(), registerRequest.getLifetime(), registerRequest.getSmsNumber(),
                registerRequest.getBindingMode(), registerRequest.getObjectLinks(), serverEndpoint);

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

        // if this is a secure end-point, we must check that the registering client is using the right identity.
        if (clientIdentity.isSecure()) {
            if (expectedSecurityInfo == null) {
                LOG.debug("A client {} without security info try to connect through the secure endpont",
                        lwM2mEndPointName);
                return false;
            } else if (clientIdentity.isPSK()) {
                // Manage PSK authentication
                // ----------------------------------------------------
                String pskIdentity = clientIdentity.getPskIdentity();
                LOG.debug("Registration request received using the secure endpoint with identity {}", pskIdentity);

                if (pskIdentity == null || !pskIdentity.equals(expectedSecurityInfo.getIdentity())) {
                    LOG.warn("Invalid identity for client {}: expected '{}' but was '{}'", lwM2mEndPointName,
                            expectedSecurityInfo.getIdentity(), pskIdentity);
                    return false;
                } else {
                    LOG.debug("authenticated client {} using DTLS PSK", lwM2mEndPointName);
                }
            } else if (clientIdentity.isRPK()) {
                // Manage RPK authentication
                // ----------------------------------------------------
                PublicKey publicKey = clientIdentity.getRawPublicKey();
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Registration request received using the secure endpoint with rpk {}",
                            Hex.encodeHexString(publicKey.getEncoded()));
                }

                if (publicKey == null || !publicKey.equals(expectedSecurityInfo.getRawPublicKey())) {
                    if (LOG.isWarnEnabled()) {
                        LOG.warn("Invalid rpk for client {}: expected \n'{}'\n but was \n'{}'", lwM2mEndPointName,
                                Hex.encodeHexString(expectedSecurityInfo.getRawPublicKey().getEncoded()),
                                Hex.encodeHexString(publicKey.getEncoded()));
                    }
                    return false;
                } else {
                    LOG.debug("authenticated client {} using DTLS RPK", lwM2mEndPointName);
                }
            } else if (clientIdentity.isX509()) {
                // Manage X509 certificate authentication
                // ----------------------------------------------------
                String x509CommonName = clientIdentity.getX509CommonName();
                LOG.debug("Registration request received using the secure endpoint with X509 identity {}",
                        x509CommonName);

                if (!expectedSecurityInfo.useX509Cert()) {
                    LOG.warn("Client {} is not supposed to use X509 certificate to authenticate", lwM2mEndPointName);
                    return false;
                }

                if (!x509CommonName.equals(lwM2mEndPointName)) {
                    LOG.warn("Invalid certificate common name for client {}: expected \n'{}'\n but was \n'{}'",
                            lwM2mEndPointName, lwM2mEndPointName, x509CommonName);
                    return false;
                } else {
                    LOG.debug("authenticated client {} using DTLS X509 certificates", lwM2mEndPointName);
                }
            } else {
                LOG.warn("Unable to authenticate client {}: unknown authentication mode.", lwM2mEndPointName);
                return false;
            }
        } else {
            if (expectedSecurityInfo != null) {
                LOG.warn("client {} must connect using DTLS ", lwM2mEndPointName);
                return false;
            }
        }
        return true;
    }
}
