package org.eclipse.leshan.server.registration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.leshan.core.link.Link;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.server.LeshanServer;
import org.eclipse.leshan.server.registration.EndDeviceRegistration.Builder;
import org.eclipse.leshan.server.registration.RegistrationDataExtractor.RegistrationData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EndDeviceRegistrationHandler implements RegistrationListener {

    private static final Logger LOG = LoggerFactory.getLogger(EndDeviceRegistrationHandler.class);

    private final RegistrationServiceImpl registrationService;
    private final RegistrationIdProvider registrationIdProvider;
    private final RegistrationDataExtractor dataExtractor;

    private final LeshanServer server;

    public static class EndDeviceData {
        public final String endpoint;
        public final String prefix;
        public final Link[] objectLinks;

        public EndDeviceData(String endpoint, String prefix, Link[] objectLinks) {
            this.endpoint = endpoint;
            this.prefix = prefix;
            this.objectLinks = objectLinks;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public Link[] getObjectLinks() {
            return objectLinks;
        }
    }

    public EndDeviceRegistrationHandler(RegistrationServiceImpl registrationService,
            RegistrationIdProvider registrationIdProvider, RegistrationDataExtractor dataExtractor,
            LeshanServer server) {
        this.registrationService = registrationService;
        this.registrationIdProvider = registrationIdProvider;
        this.dataExtractor = dataExtractor;
        this.server = server;

        // listen registration to automatically handle Iot Device Registration.
        this.registrationService.addListener(this);
    }

    public void registerEndDevices(IRegistration gateway, LwM2mObject object25) {

        // convert object 25 value in End Device Registration
        List<EndDeviceRegistration> endsDeviceRegistrations = new ArrayList<>();
        for (LwM2mObjectInstance instance : object25.getInstances().values()) {

            // TODO should be create a class to extract data easily from node ?
            LwM2mResource endpointResource = instance.getResource(0);
            String endpoint = endpointResource == null ? null : (String) endpointResource.getValue();
            LwM2mResource prefixResource = instance.getResource(1);
            String prefix = prefixResource == null ? null : (String) prefixResource.getValue();
            LwM2mResource objectLinksResource = instance.getResource(1);
            Link[] objectLinks = objectLinksResource == null ? null : (Link[]) objectLinksResource.getValue();

            // TODO Should generate ID or create manual one
            String registrationId = gateway.getId() + prefix;
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
                LOG.error("Invalid object link for {} {}", endpoint, prefix);
            }
        }

        // TODO add Gateway + endDevice to registration store
    }

    @Override
    public void registered(IRegistration registration, IRegistration previousReg,
            Collection<Observation> previousObservations) {
        if (registration.isGateway()) {
            server.send(registration, new ReadRequest(25), //
                    r -> {
                        if (r.isSuccess()) {
                            // register new End devices
                            registerEndDevices(registration, (LwM2mObject) r.getContent());
                        } else {
                            // TODO better handle this
                            LOG.error("Unable to read Object 25 : {}", r);
                        }
                    }, e -> {
                        // TODO better handle this
                        LOG.error("Unable to read Object 25 ", e);
                    });
        }
    }

    @Override
    public void updated(RegistrationUpdate update, IRegistration updatedReg, IRegistration previousReg) {
    }

    @Override
    public void unregistered(IRegistration registration, Collection<Observation> observations, boolean expired,
            IRegistration newReg) {
    }
}
