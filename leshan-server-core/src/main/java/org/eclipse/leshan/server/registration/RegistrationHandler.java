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
import org.eclipse.leshan.server.client.Registration;
import org.eclipse.leshan.server.client.RegistrationService;
import org.eclipse.leshan.server.client.RegistrationUpdate;
import org.eclipse.leshan.server.impl.RegistrationServiceImpl;
import org.eclipse.leshan.server.security.Authorizer;
import org.eclipse.leshan.util.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handle the client registration logic. Check if the client is allowed to register, with the wanted security scheme.
 * Create the {@link Registration} representing the registered client and add it to the {@link RegistrationService}
 */
public class RegistrationHandler {

    private static final Logger LOG = LoggerFactory.getLogger(RegistrationHandler.class);

    private RegistrationServiceImpl registrationService;
    private Authorizer authorizer;

    public RegistrationHandler(RegistrationServiceImpl registrationService, Authorizer authorizer) {
        this.registrationService = registrationService;
        this.authorizer = authorizer;
    }

    public RegisterResponse register(Identity sender, RegisterRequest registerRequest,
            InetSocketAddress serverEndpoint) {

        Registration.Builder builder = new Registration.Builder(RegistrationHandler.createRegistrationId(),
                registerRequest.getEndpointName(), sender.getPeerAddress().getAddress(), sender.getPeerAddress()
                        .getPort(), serverEndpoint);

        builder.lwM2mVersion(registerRequest.getLwVersion()).lifeTimeInSec(registerRequest.getLifetime())
                .bindingMode(registerRequest.getBindingMode()).objectLinks(registerRequest.getObjectLinks())
                .smsNumber(registerRequest.getSmsNumber()).registrationDate(new Date()).lastUpdate(new Date())
                .additionalRegistrationAttributes(registerRequest.getAdditionalAttributes());

        Registration registration = builder.build();

        // We must check if the client is using the right identity.
        if (!authorizer.isAuthorized(registerRequest, registration, sender)) {
            return RegisterResponse.forbidden(null);
        }

        // Add registration to the store
        Deregistration deregistration = registrationService.getStore().addRegistration(registration);

        // notify new registration and de-registration
        LOG.debug("New registration: {}", registration);
        if (deregistration != null) {
            registrationService.fireUnregistered(deregistration.getRegistration());
        }
        registrationService.fireRegistred(registration);
            
        return RegisterResponse.success(registration.getId());
    }

    public UpdateResponse update(Identity sender, UpdateRequest updateRequest) {

        // We must check if the client is using the right identity.
        Registration registration = registrationService.getById(updateRequest.getRegistrationId());
        if (registration == null) {
            return UpdateResponse.notFound();
        }

        if (!authorizer.isAuthorized(updateRequest, registration, sender)) {
            // TODO replace by Forbidden if https://github.com/OpenMobileAlliance/OMA_LwM2M_for_Developers/issues/181 is
            // closed.
            return UpdateResponse.badRequest("forbidden");
        }

        // Create update
        RegistrationUpdate update = new RegistrationUpdate(updateRequest.getRegistrationId(), sender
                .getPeerAddress().getAddress(), sender.getPeerAddress().getPort(), updateRequest.getLifeTimeInSec(),
                updateRequest.getSmsNumber(), updateRequest.getBindingMode(), updateRequest.getObjectLinks());

        // update registration
        registration = registrationService.getStore().updateRegistration(update);
        if (registration == null) {
            LOG.debug("Invalid update :  registration not found");
            return UpdateResponse.notFound();
        } else {
            LOG.debug("Updated registration {} by {}", registration, update);
            // notify registration update
            registrationService.fireUpdated(update, registration);
            return UpdateResponse.success();
        }
    }

    public DeregisterResponse deregister(Identity sender, DeregisterRequest deregisterRequest) {

        // We must check if the client is using the right identity.
        Registration registration = registrationService.getById(deregisterRequest.getRegistrationId());
        if (registration == null) {
            return DeregisterResponse.notFound();
        }
        if (!authorizer.isAuthorized(deregisterRequest, registration, sender)) {
            // TODO replace by Forbidden if https://github.com/OpenMobileAlliance/OMA_LwM2M_for_Developers/issues/181 is
            // closed.
            return DeregisterResponse.badRequest("forbidden");
        }

        Deregistration deregistration = registrationService.getStore()
                .removeRegistration(deregisterRequest.getRegistrationId());

        if (deregistration != null) {
            LOG.debug("Deregistered client : {}", deregistration.getRegistration());
            // notify new de-registration
            registrationService.fireUnregistered(registration);
            return DeregisterResponse.success();
        } else {
            LOG.debug("Invalid deregistration :  registration not found");
            return DeregisterResponse.notFound();
        }
    }

    private static String createRegistrationId() {
        return RandomStringUtils.random(10, true, true);
    }
}
