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
 *     Michał Wadowski (Orange) - Improved compliance with rfc6690
 *******************************************************************************/
package org.eclipse.leshan.server.registration;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.leshan.core.LwM2m.LwM2mVersion;
import org.eclipse.leshan.core.LwM2m.Version;
import org.eclipse.leshan.core.endpoint.EndpointUri;
import org.eclipse.leshan.core.link.Link;
import org.eclipse.leshan.core.link.LinkUtil;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.peer.IpPeer;
import org.eclipse.leshan.core.peer.LwM2mPeer;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.util.Validate;

/**
 * An immutable structure which represent a LW-M2M client registration on the server.
 *
 * This is default implementation of {@link Registration} and concern device/client connecting directly the server as
 * opposed to child {@link EndDeviceRegistration} behind a gateway.
 */
public class DeviceRegistration implements Registration {

    private static final long DEFAULT_LIFETIME_IN_SEC = 86400L;

    private final Date registrationDate;

    private final LwM2mPeer clientTransportData;

    private final long lifeTimeInSec;

    private final String smsNumber;

    private final LwM2mVersion lwM2mVersion;

    private final EnumSet<BindingMode> bindingMode;

    private final Boolean queueMode; // since LWM2M 1.1

    // The LWM2M Client's unique end point name.
    private final String endpoint;

    private final String id;

    private final Link[] objectLinks;

    private final Map<String, String> additionalRegistrationAttributes;

    // The location where LWM2M objects are hosted on the device
    private final String rootPath;

    // All ContentFormat supported by the client
    private final Set<ContentFormat> supportedContentFormats;

    // All supported object (object id => version)
    private final Map<Integer, Version> supportedObjects;

    // All available instances
    private final Set<LwM2mPath> availableInstances;

    private final Date lastUpdate;

    private final Map<String, String> customRegistrationData;

    // URI of endpoint used for this registration.
    private final EndpointUri endpointUri;

    // All child devices (prefix -> regid)
    private final Map<String, String> endDevices;

    protected DeviceRegistration(Builder builder) {

        // mandatory params
        id = builder.registrationId;
        clientTransportData = builder.clientTransportData;
        endpoint = builder.endpoint;
        endpointUri = builder.endpointUri;

        // object links related params
        objectLinks = builder.objectLinks;
        rootPath = builder.rootPath;
        supportedContentFormats = builder.supportedContentFormats;
        supportedObjects = builder.supportedObjects;
        availableInstances = builder.availableInstances;

        // other params
        lifeTimeInSec = builder.lifeTimeInSec;
        lwM2mVersion = builder.lwM2mVersion;
        bindingMode = builder.bindingMode;
        queueMode = builder.queueMode;
        registrationDate = builder.registrationDate;
        lastUpdate = builder.lastUpdate;
        smsNumber = builder.smsNumber;
        additionalRegistrationAttributes = builder.additionalRegistrationAttributes;

        customRegistrationData = builder.customRegistrationData;

        // gateway params
        endDevices = builder.endDevices;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Date getRegistrationDate() {
        return registrationDate;
    }

    @Override
    public LwM2mPeer getClientTransportData() {
        return clientTransportData;
    }

    @Override
    public InetSocketAddress getSocketAddress() {
        if (clientTransportData instanceof IpPeer) {
            return ((IpPeer) clientTransportData).getSocketAddress();
        }
        return null;
    }

    @Override
    public InetAddress getAddress() {
        if (clientTransportData instanceof IpPeer) {
            return ((IpPeer) clientTransportData).getSocketAddress().getAddress();
        }
        return null;
    }

    @Override
    public Integer getPort() {
        if (clientTransportData instanceof IpPeer) {
            return ((IpPeer) clientTransportData).getSocketAddress().getPort();
        }
        return null;
    }

    @Override
    public Link[] getObjectLinks() {
        return objectLinks;
    }

    @Override
    public Link[] getSortedObjectLinks() {
        return LinkUtil.sort(objectLinks);
    }

    @Override
    public Long getLifeTimeInSec() {
        return lifeTimeInSec;
    }

    @Override
    public String getSmsNumber() {
        return smsNumber;
    }

    @Override
    public LwM2mVersion getLwM2mVersion() {
        return lwM2mVersion;
    }

    @Override
    public EnumSet<BindingMode> getBindingMode() {
        return bindingMode;
    }

    @Override
    public Boolean getQueueMode() {
        return queueMode;
    }

    @Override
    public String getRootPath() {
        return rootPath;
    }

    @Override
    public Set<ContentFormat> getSupportedContentFormats() {
        return supportedContentFormats;
    }

    @Override
    public Set<LwM2mPath> getAvailableInstances() {
        return availableInstances;
    }

    @Override
    public String getEndpoint() {
        return endpoint;
    }

    @Override
    public Date getLastUpdate() {
        return lastUpdate;
    }

    @Override
    public long getExpirationTimeStamp() {
        return getExpirationTimeStamp(0L);
    }

    @Override
    public long getExpirationTimeStamp(long gracePeriodInSec) {
        return lastUpdate.getTime() + lifeTimeInSec * 1000 + gracePeriodInSec * 1000;
    }

    @Override
    public boolean canInitiateConnection() {
        // We consider that initiates a connection (acting as DTLS client to initiate a handshake) does not make sense
        // for QueueMode as if we lost connection device is probably absent.
        return !usesQueueMode();
    }

    @Override
    public boolean isAlive() {
        return isAlive(0);
    }

    @Override
    public boolean isAlive(long gracePeriodInSec) {
        return getExpirationTimeStamp(gracePeriodInSec) > System.currentTimeMillis();
    }

    @Override
    public Map<String, String> getAdditionalRegistrationAttributes() {
        return additionalRegistrationAttributes;
    }

    @Override
    public boolean usesQueueMode() {
        if (lwM2mVersion.olderThan(LwM2mVersion.V1_1))
            return bindingMode.contains(BindingMode.Q);
        else
            return queueMode;
    }

    @Override
    public Version getSupportedVersion(Integer objectid) {
        return getSupportedObject().get(objectid);
    }

    @Override
    public Map<Integer, Version> getSupportedObject() {
        return supportedObjects;
    }

    @Override
    public Map<String, String> getCustomRegistrationData() {
        return customRegistrationData;
    }

    @Override
    public EndpointUri getEndpointUri() {
        return endpointUri;
    }

    @Override
    public boolean isGateway() {
        return getSupportedVersion(25) != null;
    }

    @Override
    public boolean hasChildEndDevices() {
        return !endDevices.isEmpty();
    }

    @Override
    public Map<String, String> getChildEndDevices() {
        return endDevices;
    }

    @Override
    public String toString() {
        return String.format(
                "Registration [registrationDate=%s, clientTransportData=%s, lifeTimeInSec=%s, smsNumber=%s, lwM2mVersion=%s, bindingMode=%s, queueMode=%s, endpoint=%s, id=%s, objectLinks=%s, additionalRegistrationAttributes=%s, rootPath=%s, supportedContentFormats=%s, supportedObjects=%s, availableInstances=%s, lastUpdate=%s, customRegistrationData=%s, endpointUri=%s, endDevices=%s]",
                registrationDate, clientTransportData, lifeTimeInSec, smsNumber, lwM2mVersion, bindingMode, queueMode,
                endpoint, id, Arrays.toString(objectLinks), additionalRegistrationAttributes, rootPath,
                supportedContentFormats, supportedObjects, availableInstances, lastUpdate, customRegistrationData,
                endpointUri, endDevices);
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof DeviceRegistration))
            return false;
        DeviceRegistration that = (DeviceRegistration) o;
        return lifeTimeInSec == that.lifeTimeInSec && Objects.equals(registrationDate, that.registrationDate)
                && Objects.equals(clientTransportData, that.clientTransportData)
                && Objects.equals(smsNumber, that.smsNumber) && Objects.equals(lwM2mVersion, that.lwM2mVersion)
                && Objects.equals(bindingMode, that.bindingMode) && Objects.equals(queueMode, that.queueMode)
                && Objects.equals(endpoint, that.endpoint) && Objects.equals(id, that.id)
                && Arrays.equals(objectLinks, that.objectLinks)
                && Objects.equals(additionalRegistrationAttributes, that.additionalRegistrationAttributes)
                && Objects.equals(rootPath, that.rootPath)
                && Objects.equals(supportedContentFormats, that.supportedContentFormats)
                && Objects.equals(supportedObjects, that.supportedObjects)
                && Objects.equals(availableInstances, that.availableInstances)
                && Objects.equals(lastUpdate, that.lastUpdate)
                && Objects.equals(customRegistrationData, that.customRegistrationData)
                && Objects.equals(endpointUri, that.endpointUri) //
                && Objects.equals(endDevices, that.endDevices);
    }

    @Override
    public final int hashCode() {
        return Objects.hash(registrationDate, clientTransportData, lifeTimeInSec, smsNumber, lwM2mVersion, bindingMode,
                queueMode, endpoint, id, Arrays.hashCode(objectLinks), additionalRegistrationAttributes, rootPath,
                supportedContentFormats, supportedObjects, availableInstances, lastUpdate, customRegistrationData,
                endpointUri, endDevices);
    }

    public static class Builder {
        private final String registrationId;
        private final String endpoint;
        private final LwM2mPeer clientTransportData;
        private final EndpointUri endpointUri;

        private Date registrationDate;
        private Date lastUpdate;
        private Long lifeTimeInSec;
        private String smsNumber;
        private EnumSet<BindingMode> bindingMode;
        private Boolean queueMode;
        private LwM2mVersion lwM2mVersion = LwM2mVersion.getDefault();
        private Link[] objectLinks;
        private String rootPath;
        private Set<ContentFormat> supportedContentFormats;
        private Map<Integer, Version> supportedObjects;
        private Set<LwM2mPath> availableInstances;
        private Map<String, String> additionalRegistrationAttributes;
        private Map<String, String> customRegistrationData;
        private Map<String, String> endDevices;

        public Builder(DeviceRegistration registration) {

            // mandatory params
            registrationId = registration.id;
            clientTransportData = registration.clientTransportData;
            endpoint = registration.endpoint;
            endpointUri = registration.endpointUri;

            // object links related params
            objectLinks = registration.objectLinks;
            rootPath = registration.rootPath;
            supportedContentFormats = registration.supportedContentFormats;
            supportedObjects = registration.supportedObjects;
            availableInstances = registration.availableInstances;

            // other params
            lifeTimeInSec = registration.lifeTimeInSec;
            lwM2mVersion = registration.lwM2mVersion;
            bindingMode = registration.bindingMode;
            queueMode = registration.queueMode;
            registrationDate = registration.registrationDate;
            lastUpdate = registration.lastUpdate;
            smsNumber = registration.smsNumber;
            additionalRegistrationAttributes = registration.additionalRegistrationAttributes;

            customRegistrationData = registration.customRegistrationData;

            // gateway params
            endDevices = registration.endDevices;
        }

        public Builder(String registrationId, String endpoint, LwM2mPeer clientTransportData, EndpointUri endpointUri) {

            Validate.notNull(registrationId);
            Validate.notEmpty(endpoint);
            Validate.notNull(clientTransportData);
            Validate.notNull(endpointUri);

            this.registrationId = registrationId;
            this.endpoint = endpoint;
            this.clientTransportData = clientTransportData;
            this.endpointUri = endpointUri;
        }

        public Builder registrationDate(Date registrationDate) {
            this.registrationDate = registrationDate;
            return this;
        }

        public Builder lastUpdate(Date lastUpdate) {
            this.lastUpdate = lastUpdate;
            return this;
        }

        public Builder lifeTimeInSec(Long lifetimeInSec) {
            this.lifeTimeInSec = lifetimeInSec;
            return this;
        }

        public Builder smsNumber(String smsNumber) {
            this.smsNumber = smsNumber;
            return this;
        }

        @SuppressWarnings("java:S1319")
        public Builder bindingMode(EnumSet<BindingMode> bindingMode) {
            this.bindingMode = bindingMode;
            return this;
        }

        public Builder queueMode(Boolean queueMode) {
            this.queueMode = queueMode;
            return this;
        }

        public Builder lwM2mVersion(LwM2mVersion lwM2mVersion) {
            this.lwM2mVersion = lwM2mVersion;
            return this;
        }

        public Builder objectLinks(Link[] objectLinks) {
            this.objectLinks = objectLinks;
            return this;
        }

        public Builder rootPath(String rootPath) {
            this.rootPath = rootPath;
            return this;
        }

        public Builder supportedContentFormats(Set<ContentFormat> supportedContentFormats) {
            this.supportedContentFormats = supportedContentFormats;
            return this;
        }

        public Builder supportedContentFormats(ContentFormat... supportedContentFormats) {
            this.supportedContentFormats = new HashSet<>();
            for (ContentFormat contentFormat : supportedContentFormats) {
                this.supportedContentFormats.add(contentFormat);
            }
            return this;
        }

        public Builder supportedObjects(Map<Integer, Version> supportedObjects) {
            this.supportedObjects = supportedObjects;
            return this;
        }

        public Builder availableInstances(Set<LwM2mPath> availableInstances) {
            this.availableInstances = availableInstances;
            return this;
        }

        public Builder additionalRegistrationAttributes(Map<String, String> additionalRegistrationAttributes) {
            this.additionalRegistrationAttributes = additionalRegistrationAttributes;
            return this;
        }

        public Builder customRegistrationData(Map<String, String> customRegistrationData) {
            this.customRegistrationData = customRegistrationData;
            return this;
        }

        public Builder endDevices(Map<String, String> endDevices) {
            this.endDevices = endDevices;
            return this;
        }

        @SuppressWarnings("java:S3776")
        public DeviceRegistration build() {
            // Define Default value
            rootPath = rootPath == null ? "/" : rootPath;
            lifeTimeInSec = lifeTimeInSec == null ? DEFAULT_LIFETIME_IN_SEC : lifeTimeInSec;
            lwM2mVersion = lwM2mVersion == null ? LwM2mVersion.getDefault() : lwM2mVersion;
            bindingMode = bindingMode == null ? EnumSet.of(BindingMode.U) : bindingMode;
            queueMode = queueMode == null && lwM2mVersion.newerThan(LwM2mVersion.V1_0) ? Boolean.FALSE : queueMode;
            registrationDate = registrationDate == null ? new Date() : registrationDate;
            lastUpdate = lastUpdate == null ? new Date() : lastUpdate;

            // Make collection immutable
            // We create a new Collection and make it "unmodifiable".
            if (supportedContentFormats == null || supportedContentFormats.isEmpty()) {
                supportedContentFormats = Collections.emptySet();
            } else {
                supportedContentFormats = Collections.unmodifiableSet(new HashSet<>(supportedContentFormats));
            }
            if (supportedObjects == null || supportedObjects.isEmpty()) {
                supportedObjects = Collections.emptyMap();
            } else {
                supportedObjects = Collections.unmodifiableMap(new HashMap<>(supportedObjects));
            }
            if (availableInstances == null || availableInstances.isEmpty()) {
                availableInstances = Collections.emptySet();
            } else {
                availableInstances = Collections.unmodifiableSet(new TreeSet<>(availableInstances));
            }
            if (additionalRegistrationAttributes == null || additionalRegistrationAttributes.isEmpty()) {
                additionalRegistrationAttributes = Collections.emptyMap();
            } else {
                additionalRegistrationAttributes = Collections
                        .unmodifiableMap(new HashMap<>(additionalRegistrationAttributes));
            }
            if (customRegistrationData == null || customRegistrationData.isEmpty()) {
                customRegistrationData = Collections.emptyMap();
            } else {
                customRegistrationData = Collections.unmodifiableMap(new HashMap<>(customRegistrationData));
            }

            if (endDevices == null || endDevices.isEmpty()) {
                endDevices = Collections.emptyMap();
            } else {
                endDevices = Collections.unmodifiableMap(new HashMap<>(endDevices));
            }

            // Create Registration
            return new DeviceRegistration(this);
        }
    }
}
