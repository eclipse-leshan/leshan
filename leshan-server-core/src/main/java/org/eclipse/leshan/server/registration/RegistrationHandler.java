/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
 *     Achim Kraus (Bosch Software Innovations GmbH) - use Identity as destination
 *     Rokwoon Kim (contracted with NTELS) - use registrationIdProvider
 *******************************************************************************/
package org.eclipse.leshan.server.registration;

import java.net.URI;
import java.util.Date;

import org.eclipse.leshan.core.LwM2m.LwM2mVersion;
import org.eclipse.leshan.core.request.DeregisterRequest;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.request.RegisterRequest;
import org.eclipse.leshan.core.request.UpdateRequest;
import org.eclipse.leshan.core.response.DeregisterResponse;
import org.eclipse.leshan.core.response.RegisterResponse;
import org.eclipse.leshan.core.response.SendableResponse;
import org.eclipse.leshan.core.response.UpdateResponse;
import org.eclipse.leshan.server.security.Authorization;
import org.eclipse.leshan.server.security.Authorizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handle the client registration logic. Check if the client is allowed to register, with the wanted security scheme.
 * Create the {@link Registration} representing the registered client and add it to the {@link RegistrationService}
 */
public class RegistrationHandler {

    private static final Logger LOG = LoggerFactory.getLogger(RegistrationHandler.class);

    private final RegistrationServiceImpl registrationService;
    private final RegistrationIdProvider registrationIdProvider;
    private final Authorizer authorizer;

    public RegistrationHandler(RegistrationServiceImpl registrationService, Authorizer authorizer,
            RegistrationIdProvider registrationIdProvider) {
        this.registrationService = registrationService;
        this.authorizer = authorizer;
        this.registrationIdProvider = registrationIdProvider;
    }

    public SendableResponse<RegisterResponse> register(Identity sender, RegisterRequest registerRequest,
            URI endpointUsed) {

        // Create Registration from RegisterRequest
        Registration.Builder builder = new Registration.Builder(
                registrationIdProvider.getRegistrationId(registerRequest), registerRequest.getEndpointName(), sender,
                endpointUsed);
        builder.extractDataFromObjectLink(true);

        builder.lwM2mVersion(LwM2mVersion.get(registerRequest.getLwVersion()))
                .lifeTimeInSec(registerRequest.getLifetime()).bindingMode(registerRequest.getBindingMode())
                .queueMode(registerRequest.getQueueMode()).objectLinks(registerRequest.getObjectLinks())
                .smsNumber(registerRequest.getSmsNumber()).registrationDate(new Date()).lastUpdate(new Date())
                .additionalRegistrationAttributes(registerRequest.getAdditionalAttributes());

        Registration registrationToApproved = builder.build();

        // We check if the client get authorization.
        Authorization authorization = authorizer.isAuthorized(registerRequest, registrationToApproved, sender);
        if (authorization.isDeclined()) {
            return new SendableResponse<>(RegisterResponse.forbidden(null));
        }

        // Add Authorization Application Data to Registration if needed
        final Registration approvedRegistration;
        if (authorization.hasApplicationData()) {
            approvedRegistration = new Registration.Builder(registrationToApproved)
                    .applicationData(authorization.getApplicationData()).build();
        } else {
            approvedRegistration = registrationToApproved;
        }

        // Add registration to the store
        final Deregistration deregistration = registrationService.getStore().addRegistration(approvedRegistration);

        // Create callback to notify new registration and de-registration
        LOG.debug("New registration: {}", approvedRegistration);
        Runnable whenSent = new Runnable() {
            @Override
            public void run() {
                if (deregistration != null) {
                    registrationService.fireUnregistered(deregistration.getRegistration(),
                            deregistration.getObservations(), approvedRegistration);
                    registrationService.fireRegistered(approvedRegistration, deregistration.registration,
                            deregistration.observations);
                } else {
                    registrationService.fireRegistered(approvedRegistration, null, null);
                }
            }
        };

        return new SendableResponse<>(RegisterResponse.success(approvedRegistration.getId()), whenSent);
    }

    public SendableResponse<UpdateResponse> update(Identity sender, UpdateRequest updateRequest) {

        // We check if there is a registration to update
        Registration currentRegistration = registrationService.getById(updateRequest.getRegistrationId());
        if (currentRegistration == null) {
            return new SendableResponse<>(UpdateResponse.notFound());
        }

        // We check if the client get authorization.
        Authorization authorization = authorizer.isAuthorized(updateRequest, currentRegistration, sender);
        if (authorization.isDeclined()) {
            return new SendableResponse<>(UpdateResponse.badRequest("forbidden"));
        }

        // validate request
        updateRequest.validate(currentRegistration.getLwM2mVersion());

        // Create update
        final RegistrationUpdate update = new RegistrationUpdate(updateRequest.getRegistrationId(), sender,
                updateRequest.getLifeTimeInSec(), updateRequest.getSmsNumber(), updateRequest.getBindingMode(),
                updateRequest.getObjectLinks(), updateRequest.getAdditionalAttributes(),
                authorization.getApplicationData());

        // update registration
        final UpdatedRegistration updatedRegistration = registrationService.getStore().updateRegistration(update);
        if (updatedRegistration == null) {
            LOG.debug("Invalid update:  registration {} not found", currentRegistration.getId());
            return new SendableResponse<>(UpdateResponse.notFound());
        } else {
            LOG.debug("Updated registration {} by {}", updatedRegistration, update);
            // Create callback to notify registration update
            Runnable whenSent = new Runnable() {
                @Override
                public void run() {
                    registrationService.fireUpdated(update, updatedRegistration.getUpdatedRegistration(),
                            updatedRegistration.getPreviousRegistration());
                };
            };
            return new SendableResponse<>(UpdateResponse.success(), whenSent);
        }
    }

    public SendableResponse<DeregisterResponse> deregister(Identity sender, DeregisterRequest deregisterRequest) {

        // We check if there is a registration to remove
        Registration currentRegistration = registrationService.getById(deregisterRequest.getRegistrationId());
        if (currentRegistration == null) {
            return new SendableResponse<>(DeregisterResponse.notFound());
        }

        // We check if the client get authorization.
        Authorization authorization = authorizer.isAuthorized(deregisterRequest, currentRegistration, sender);
        if (authorization.isDeclined()) {
            return new SendableResponse<>(DeregisterResponse.badRequest("forbidden"));
        }

        final Deregistration deregistration = registrationService.getStore()
                .removeRegistration(deregisterRequest.getRegistrationId());

        if (deregistration != null) {
            LOG.debug("Deregistered client: {}", deregistration.getRegistration());
            // Create callback to notify new de-registration
            Runnable whenSent = new Runnable() {
                @Override
                public void run() {
                    registrationService.fireUnregistered(deregistration.getRegistration(),
                            deregistration.getObservations(), null);
                };
            };
            return new SendableResponse<>(DeregisterResponse.success(), whenSent);
        } else {
            LOG.debug("Invalid deregistration :  registration {} not found", currentRegistration.getId());
            return new SendableResponse<>(DeregisterResponse.notFound());
        }
    }

}
