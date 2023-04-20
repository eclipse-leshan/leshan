/*******************************************************************************
 * Copyright (c) 2023 Sierra Wireless and others.
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
package org.eclipse.leshan.server.gateway;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.leshan.core.link.Link;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.request.RegisterRequest;
import org.eclipse.leshan.server.registration.Deregistration;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationHandler;
import org.eclipse.leshan.server.registration.RegistrationIdProvider;
import org.eclipse.leshan.server.registration.RegistrationListener;
import org.eclipse.leshan.server.registration.RegistrationServiceImpl;
import org.eclipse.leshan.server.registration.RegistrationUpdate;
import org.eclipse.leshan.server.registration.UpdatedRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO we should double check/think about possible race condition between gateway/iotDevice registration modification in registration store.
public class IotDevicesRegistrationHandler implements RegistrationListener, GatewayService {

    private static final Logger LOG = LoggerFactory.getLogger(RegistrationHandler.class);

    private final RegistrationServiceImpl registrationService;
    private final RegistrationIdProvider registrationIdProvider;

    public IotDevicesRegistrationHandler(RegistrationServiceImpl registrationService,
            RegistrationIdProvider registrationIdProvider) {
        this.registrationService = registrationService;
        this.registrationIdProvider = registrationIdProvider;

        // listen registration to automatically handle Iot Device Registration.
        this.registrationService.addListener(this);
    }

    @Override
    public boolean registerIotDevice(String gatewayRegId, String endpoint, String prefix, Link[] objectLinks) {
        // Search for Gateway registration
        Registration gatewayRegistration = registrationService.getById(gatewayRegId);
        if (gatewayRegistration == null) {
            LOG.error("gateway registration not found {}", gatewayRegId);
            return false;
        }

        // Create Iot Device registration
        RegisterRequest registerRequest = new RegisterRequest(endpoint, gatewayRegistration.getLifeTimeInSec(),
                gatewayRegistration.getLwM2mVersion().toString(), gatewayRegistration.getBindingMode(),
                gatewayRegistration.getQueueMode(), gatewayRegistration.getSmsNumber(), objectLinks, null);

        Registration.Builder builder = new Registration.Builder(
                registrationIdProvider.getRegistrationId(registerRequest), endpoint, gatewayRegistration.getIdentity(),
                gatewayRegistration.getLastEndpointUsed());

        builder.lwM2mVersion(gatewayRegistration.getLwM2mVersion());
        builder.queueMode(gatewayRegistration.getQueueMode());
        builder.bindingMode(gatewayRegistration.getBindingMode());
        builder.smsNumber(gatewayRegistration.getSmsNumber());
        builder.objectLinks(objectLinks);
        builder.extractDataFromObjectLink(true);

        // Add Application Data specific to Iot device Registration
        Map<String, String> appData = new HashMap<>();
        appData.put(GatewayAppData.GATEWAY_REGID, gatewayRegistration.getId());
        appData.put(GatewayAppData.IOT_DEVICE_PREFIX, prefix);
        builder.applicationData(appData);

        final Registration registration = builder.build();

        // TODO: how to handle identity with gateways ??
        // we probably need a service which can authorize a given gateway to manage a list of end devices
        // final Registration registration = authorizer.isAuthorized(registerRequest, builder.build(),
        // gatewayRegistration.getIdentity());

        // Update the gateway registration for adding the end IoT device
        // - update iot devices list
        List<String> iotDeviceEndpointNames = new ArrayList<>(GatewayAppData.getIotDevicesEndpointNamesFromString(
                gatewayRegistration.getApplicationData().get(GatewayAppData.IOT_DEVICES_ENDPOINT_NAMES)));
        iotDeviceEndpointNames.add(endpoint);
        // - update application data
        Map<String, String> gatewayAppData = new HashMap<>(gatewayRegistration.getApplicationData());
        gatewayAppData.put(GatewayAppData.IOT_DEVICES_ENDPOINT_NAMES,
                GatewayAppData.iotDevicesEndpointNamesToString(iotDeviceEndpointNames));
        // - update registration
        RegistrationUpdate update = new RegistrationUpdate(gatewayRegistration.getId(),
                gatewayRegistration.getIdentity(), null, null, null, null, null, gatewayAppData);
        registrationService.getStore().updateRegistration(update);

        // Add registration to the store
        final Deregistration deregistration = registrationService.getStore().addRegistration(registration);
        if (deregistration != null) {
            registrationService.fireUnregistered(deregistration.getRegistration(), deregistration.getObservations(),
                    registration);
            registrationService.fireRegistered(registration, deregistration.getRegistration(),
                    deregistration.getObservations());
        } else {
            registrationService.fireRegistered(registration, null, null);
        }
        return true;
    }

    @Override
    public UpdatedRegistration updateIotDeviceRegistration(RegistrationUpdate gatewayRegUpdate,
            String iotDeviceregistrationId, String endpoint, Link[] objectLinks) {

        RegistrationUpdate iotDeviceRegUpdate = new RegistrationUpdate(iotDeviceregistrationId,
                gatewayRegUpdate.getIdentity(), gatewayRegUpdate.getLifeTimeInSec(), gatewayRegUpdate.getSmsNumber(),
                gatewayRegUpdate.getBindingMode(), objectLinks, gatewayRegUpdate.getAdditionalAttributes(), null);

        UpdatedRegistration updatedRegistration = registrationService.getStore().updateRegistration(iotDeviceRegUpdate);
        registrationService.fireUpdated(iotDeviceRegUpdate, updatedRegistration.getUpdatedRegistration(),
                updatedRegistration.getPreviousRegistration());
        return updatedRegistration;
    }

    @Override
    public void registered(Registration registration, Registration previousReg,
            Collection<Observation> previousObservations) {
        // TODO should we read object25 automatically (or maybe we should have a manual and auto mode ?)
        // see : https://vermillard.com/post/object25/
    }

    @Override
    public void updated(RegistrationUpdate update, Registration updatedReg, Registration previousReg) {
        // TODO should we read object25 automatically (or maybe we should have a manual and auto mode ?)
        // see : https://vermillard.com/post/object25/
    }

    @Override
    public void unregistered(Registration registration, Collection<Observation> observations, boolean expired,
            Registration newReg) {

        String iotDevicesEndpointNamesAppData = registration.getApplicationData()
                .get(GatewayAppData.IOT_DEVICES_ENDPOINT_NAMES);
        // If this is a Gateway
        if (iotDevicesEndpointNamesAppData != null) {
            // Automatically deregister Iot Device when the Gateway unregister itself.
            List<String> iotDeviceEndpointNames = GatewayAppData
                    .getIotDevicesEndpointNamesFromString(iotDevicesEndpointNamesAppData);
            for (String iotDeviceEndpointName : iotDeviceEndpointNames) {
                Registration iotDeviceRegistration = registrationService.getByEndpoint(iotDeviceEndpointName);
                if (iotDeviceRegistration != null) {
                    // remove only if gateway registration id match
                    String gatewayRegId = iotDeviceRegistration.getApplicationData().get(GatewayAppData.GATEWAY_REGID);
                    if (gatewayRegId.equals(registration.getId())) {
                        final Deregistration iotDeviceDeregistration = registrationService.getStore()
                                .removeRegistration(iotDeviceRegistration.getId());

                        registrationService.fireUnregistered(iotDeviceDeregistration.getRegistration(),
                                iotDeviceDeregistration.getObservations(), null);

                        // TODO if gateway registration expired, does iotDeviceRegistration be considered as expired too
                        // ?
                    } else {
                        // TODO what should we do here ?
                        LOG.warn("Inconsistent registration state between gateway {} and Iot device {}", registration,
                                iotDeviceRegistration);
                    }
                }
            }
        }
    }
}
