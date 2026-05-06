/*******************************************************************************
 * Copyright (c) 2025 Sierra Wireless and others.
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
package org.eclipse.leshan.server.registration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;

import org.eclipse.leshan.core.link.Link;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.util.Validate;
import org.eclipse.leshan.server.LeshanServer;
import org.eclipse.leshan.server.registration.EndDeviceRegistration.Builder;
import org.eclipse.leshan.server.registration.RegistrationDataExtractor.RegistrationData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class to monitor End Devices registration behind LWM2M Gateway (Object 25)
 */
public class EndDeviceRegistrationHandler {

    private static final Logger LOG = LoggerFactory.getLogger(EndDeviceRegistrationHandler.class);

    protected final GatewayRegistrationStore registrationStore;
    protected final RegistrationServiceImpl registrationService;
    protected final RegistrationDataExtractor dataExtractor;
    protected final EndDeviceRegistrationIdProvider idProvider;
    protected final LeshanServer server;

    public EndDeviceRegistrationHandler(RegistrationServiceImpl registrationService,
            RegistrationDataExtractor dataExtractor, EndDeviceRegistrationIdProvider idProvider, LeshanServer server) {
        Validate.notNull(registrationService);
        Validate.notNull(dataExtractor);
        Validate.notNull(idProvider);
        Validate.notNull(server);

        RegistrationStore store = registrationService.getStore();
        if (!(store instanceof GatewayRegistrationStore)) {
            this.registrationStore = null;
            this.registrationService = null;
            this.dataExtractor = null;
            this.server = null;
            this.idProvider = null;
            LOG.warn(
                    "{} does not support implement GatewayRegistrationStore, so EndDeviceRegistrationHandler will do nothing and Gateways will not be supported",
                    store.getClass().getSimpleName());
        } else {
            this.registrationStore = (GatewayRegistrationStore) store;
            this.registrationService = registrationService;
            this.dataExtractor = dataExtractor;
            this.idProvider = idProvider;
            this.server = server;

            setupAutomaticHandling();
        }
    }

    /**
     * set automatic handling like read
     */
    protected void setupAutomaticHandling() {
        // listen registration to automatically handle Iot Device Registration.
        this.registrationService.addListener(createRegistrationListener());
    }

    /**
     * Update parent Gateway and add or replace all end device child to registration store, then raise registration
     * event.
     *
     * @param gateway the parent registration
     * @param endDevices end device data generally get by reading object 25 and using
     *        {@link #convertToEndDeviceData(Collection)}
     */
    public void registerEndDevices(DeviceRegistration gateway, List<EndDeviceData> endDevices) {

        // Create end device registrations
        List<EndDeviceRegistration> endsDeviceRegistrations = createEndDeviceRegistrations(gateway, endDevices);

        // Update Gateway and add end devices to store
        List<RegistrationModification> modifications = registrationStore.replaceEndDeviceRegistrations(gateway,
                endsDeviceRegistrations);

        // Notify modifications
        if (modifications != null)
            raiseRegistrationEvents(modifications);
    }

    protected List<EndDeviceRegistration> createEndDeviceRegistrations(DeviceRegistration gateway,
            List<EndDeviceData> endDevices) {
        List<EndDeviceRegistration> endsDeviceRegistrations = new ArrayList<>();
        for (EndDeviceData endDevice : endDevices) {

            String endpoint = endDevice.getEndpoint();
            String prefix = endDevice.getPrefix();
            Link[] objectLinks = endDevice.getObjectLinks();
            String registrationId = idProvider.getRegistrationId(gateway, endDevice);

            Builder builder = new EndDeviceRegistration.Builder(gateway, registrationId, prefix, endpoint);
            RegistrationData objLinksData = dataExtractor.extractDataFromObjectLinks(objectLinks,
                    gateway.getLwM2mVersion());
            if (objLinksData != null) {
                builder //
                        .objectLinks(objectLinks).availableInstances(objLinksData.getAvailableInstances())
                        .supportedContentFormats(objLinksData.getSupportedContentFormats())
                        .supportedObjects(objLinksData.getSupportedObjects());

                endsDeviceRegistrations.add(builder.build());
            } else {
                LOG.error("Invalid object link for {} {}, no registration will be created this end device", endpoint,
                        prefix);
            }
        }
        return endsDeviceRegistrations;
    }

    protected void raiseRegistrationEvents(List<RegistrationModification> modifications) {
        for (RegistrationModification modification : modifications) {
            if (modification instanceof RegistrationAddition) {
                RegistrationAddition addition = (RegistrationAddition) modification;
                Deregistration deregistration = addition.getPreviousRegistration();
                if (deregistration != null) {
                    registrationService.fireUnregistered(deregistration, addition.getNewRegistration());
                    registrationService.fireRegistered(addition);
                } else {
                    registrationService.fireRegistered(addition);
                }

            } else if (modification instanceof UpdatedRegistration) {
                registrationService.fireUpdated((UpdatedRegistration) modification);
            } else if (modification instanceof Deregistration) {
                registrationService.fireUnregistered((Deregistration) modification, null);
            }
        }
    }

    /**
     * @return <code>true</code> if we should read object25 for this registration
     */
    protected boolean shouldReadObject25For(Registration registration) {
        return isGateway(registration);
    }

    protected boolean isGateway(Registration registration) {
        return registration.isGateway() && registration instanceof DeviceRegistration;
    }

    /**
     * Read Object 25 for given registration, then execute <code>onSuccess</code> if read is a success
     *
     * @param registration target for the ReadRequest on Object 25
     * @param onSuccess callback if read is successful, first parameter is the gateway, second one is collection of end
     *        device data.
     * @param onError callback if read failed, first parameter is an error message (it must not be null), second one is
     *        exception (it can be null) and last one is device ReadResponse failure (can be null too)
     */
    protected void readObject25(Registration registration,
            BiConsumer<DeviceRegistration, List<EndDeviceData>> onSuccess, ErrorHandler onError) {
        server.send(registration, new ReadRequest(25), //
                r -> {
                    if (r.isSuccess()) {
                        try {
                            LwM2mObject object25 = (LwM2mObject) r.getContent();
                            List<EndDeviceData> data = convertToEndDeviceData(object25.getInstances().values());
                            onSuccess.accept((DeviceRegistration) registration, data);
                        } catch (Exception e) {
                            onError.handleError(
                                    String.format("Unable to handle end device data from object 25 of gateway %s",
                                            registration.getEndpoint()),
                                    e, null);
                        }
                    } else {
                        onError.handleError(
                                String.format("Unable to read object 25 of gateway %s", registration.getEndpoint()),
                                null, r);
                    }
                }, e -> {
                    onError.handleError(
                            String.format("Unable to read object 25 of gateway %s", registration.getEndpoint()), e,
                            null);
                });
    }

    private List<EndDeviceData> convertToEndDeviceData(Collection<LwM2mObjectInstance> instances) {
        List<EndDeviceData> result = new ArrayList<>();
        for (LwM2mObjectInstance instance : instances) {
            // extract data from LWM2M LwM2mObjectInstance
            LwM2mResource endpointResource = instance.getResource(0);
            String endpoint = endpointResource == null ? null : (String) endpointResource.getValue();
            LwM2mResource prefixResource = instance.getResource(1);
            String prefix = prefixResource == null ? null : (String) prefixResource.getValue();
            LwM2mResource objectLinksResource = instance.getResource(3);
            Link[] objectLinks = objectLinksResource == null ? null : (Link[]) objectLinksResource.getValue();
            result.add(new EndDeviceData(endpoint, prefix, objectLinks));
        }
        return result;
    }

    protected void handleError(String errorMessage, Exception cause, ReadResponse errorResponse) {
        if (cause != null) {
            LOG.error(errorMessage, cause);
        } else if (errorResponse != null) {
            LOG.error("{} : {} - {}", errorMessage, errorResponse.getCode(), errorResponse.getErrorMessage());
        } else {
            LOG.error(errorMessage);
        }
    }

    @FunctionalInterface
    public interface ErrorHandler {
        void handleError(String errorMessage, Exception cause, ReadResponse deviceFailureResponse);
    }

    protected RegistrationListener createRegistrationListener() {
        return new RegistrationListener() {

            @Override
            public void registered(RegistrationAddition registrationAddition) {
                if (shouldReadObject25For(registrationAddition.getNewRegistration())) {
                    readObject25(registrationAddition.getNewRegistration(),
                            EndDeviceRegistrationHandler.this::registerEndDevices,
                            EndDeviceRegistrationHandler.this::handleError);
                }
            }

            @Override
            public void updated(UpdatedRegistration updatedRegistration) {
                if (shouldSendUpdatedEventForChildren(updatedRegistration)) {
                    fireUpdatedEventForChildren(updatedRegistration);
                }
            }

            @Override
            public void unregistered(Deregistration deregistration, boolean expired, Registration newReg) {
                if (shouldSendDeregisterEventForChildren(deregistration, expired, newReg)) {
                    fireDeregisterEventForChildren(deregistration, expired, newReg);
                }
            }
        };
    }

    protected boolean shouldSendUpdatedEventForChildren(UpdatedRegistration updatedRegistration) {
        return isGateway(updatedRegistration.getUpdatedRegistration())
                && !updatedRegistration.getChildrenUpdatedRegistration().isEmpty();
    }

    protected void fireUpdatedEventForChildren(UpdatedRegistration updatedRegistration) {
        for (UpdatedRegistration childUpdate : updatedRegistration.getChildrenUpdatedRegistration()) {
            registrationService.fireUpdated(childUpdate);
        }
    }

    protected boolean shouldSendDeregisterEventForChildren(Deregistration deregistration, boolean expired,
            Registration newReg) {
        return isGateway(deregistration.getRegistration()) && !deregistration.getChildrenDeRegistration().isEmpty();
    }

    protected void fireDeregisterEventForChildren(Deregistration deregistration, boolean expired, Registration newReg) {
        for (Deregistration childDereg : deregistration.getChildrenDeRegistration()) {
            registrationService.fireUnregistered(childDereg, expired, newReg);
        }
    }
}
