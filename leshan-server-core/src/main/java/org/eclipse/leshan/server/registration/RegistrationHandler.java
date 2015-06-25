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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.DatatypeConverter;

import org.eclipse.leshan.ResponseCode;
import org.eclipse.leshan.core.request.DeregisterRequest;
import org.eclipse.leshan.core.request.RegisterRequest;
import org.eclipse.leshan.core.request.UpdateRequest;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.RegisterResponse;
import org.eclipse.leshan.server.client.Client;
import org.eclipse.leshan.server.client.ClientRegistry;
import org.eclipse.leshan.server.client.ClientUpdate;
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
                PublicKey rpk = registerRequest.getSourcePublicKey();
                String pskIdentity = registerRequest.getPskIdentity();
                String X509Identity = registerRequest.getX509Identity();

                if (securityInfo == null) {
                    LOG.debug("A client {} without security info try to connect through the secure endpont",
                            registerRequest.getEndpointName());
                    return new RegisterResponse(ResponseCode.FORBIDDEN);
                } else if (pskIdentity != null) {
                    // Manage PSK authentication
                    // ----------------------------------------------------
                    LOG.debug("Registration request received using the secure endpoint {} with identity {}",
                            registrationEndpoint, pskIdentity);

                    if (pskIdentity == null || !pskIdentity.equals(securityInfo.getIdentity())) {
                        LOG.warn("Invalid identity for client {}: expected '{}' but was '{}'",
                                registerRequest.getEndpointName(), securityInfo.getIdentity(), pskIdentity);
                        return new RegisterResponse(ResponseCode.FORBIDDEN);
                    } else {
                        LOG.debug("authenticated client {} using DTLS PSK", registerRequest.getEndpointName());
                    }
                } else if (rpk != null) {
                    // Manage RPK authentication
                    // ----------------------------------------------------
                    LOG.debug("Registration request received using the secure endpoint {} with rpk {}",
                            registrationEndpoint, DatatypeConverter.printHexBinary(rpk.getEncoded()));

                    if (rpk == null || !rpk.equals(securityInfo.getRawPublicKey())) {
                        LOG.warn("Invalid rpk for client {}: expected \n'{}'\n but was \n'{}'",
                                registerRequest.getEndpointName(),
                                DatatypeConverter.printHexBinary(securityInfo.getRawPublicKey().getEncoded()),
                                DatatypeConverter.printHexBinary(rpk.getEncoded()));
                        return new RegisterResponse(ResponseCode.FORBIDDEN);
                    } else {
                        LOG.debug("authenticated client {} using DTLS RPK", registerRequest.getEndpointName());
                    }
                } else if (X509Identity != null) {
                    // Manage X509 certificate authentication
                    // ----------------------------------------------------
                    LOG.debug("Registration request received using the secure endpoint {} with X509 identity {}",
                            registrationEndpoint, X509Identity);

                    String endpointFromCert = null;
                    String endpointFromReq = registerRequest.getEndpointName();

                    if (!securityInfo.useX509Cert()) {
                        LOG.warn("Client {} is not supposed to use X509 certificate to authenticate", endpointFromReq);
                        return new RegisterResponse(ResponseCode.FORBIDDEN);
                    }

                    Matcher endpointMatcher = Pattern.compile("CN=.*?,").matcher(X509Identity);
                    if (endpointMatcher.find()) {
                        endpointFromCert = endpointMatcher.group().substring(3, endpointMatcher.group().length() - 1);
                    }

                    if (endpointFromCert == null || !endpointFromCert.equals(endpointFromReq)) {
                        LOG.warn("Invalid certificate endpoint for client {}: expected \n'{}'\n but was \n'{}'",
                                endpointFromReq, endpointFromReq, endpointFromCert);
                        return new RegisterResponse(ResponseCode.FORBIDDEN);
                    } else {
                        LOG.debug("authenticated client {} using DTLS X509 certificates", endpointFromReq);
                    }
                } else {
                    LOG.warn("Unable to authenticate client {}: unknown authentication mode.",
                            registerRequest.getEndpointName());
                    return new RegisterResponse(ResponseCode.FORBIDDEN);
                }
            } else {
                if (securityInfo != null) {
                    LOG.warn("client {} must connect using DTLS ", registerRequest.getEndpointName());
                    return new RegisterResponse(ResponseCode.BAD_REQUEST);
                }
            }

            Client client = new Client(registrationId, registerRequest.getEndpointName(),
                    registerRequest.getSourceAddress(), registerRequest.getSourcePort(),
                    registerRequest.getLwVersion(), registerRequest.getLifetime(), registerRequest.getSmsNumber(),
                    registerRequest.getBindingMode(), registerRequest.getObjectLinks(), registrationEndpoint);

            if (clientRegistry.registerClient(client)) {
                LOG.debug("New registered client: {}", client);
                return new RegisterResponse(ResponseCode.CREATED, client.getRegistrationId());
            } else {
                return new RegisterResponse(ResponseCode.FORBIDDEN);
            }
        }
    }

    public LwM2mResponse update(UpdateRequest updateRequest) {
        Client client = clientRegistry.updateClient(new ClientUpdate(updateRequest.getRegistrationId(), updateRequest
                .getAddress(), updateRequest.getPort(), updateRequest.getLifeTimeInSec(), updateRequest.getSmsNumber(),
                updateRequest.getBindingMode(), updateRequest.getObjectLinks()));
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
