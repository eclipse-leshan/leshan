/*******************************************************************************
 * Copyright (c) 2026 Sierra Wireless and others.
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
import org.eclipse.leshan.core.peer.LwM2mPeer;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.util.Validate;

/**
 * An immutable structure which represent a LW-M2M End Device registration on the server.
 * <p>
 * An End Device client is handled by a Gateway.
 * <p>
 * For more details see :
 * https://www.openmobilealliance.org/release/LwM2M_Gateway/V1_1_1-20240312-A/OMA-TS-LWM2M_Gateway-V1_1_1-20240312-A.pdf
 */
public class EndDeviceRegistration implements Registration {

    // The LWM2M Client's unique end point name.
    private final String endpoint;
    private final String id;
    private final String prefix;
    private final Link[] objectLinks;
    private final Registration parentGateway;

    // All ContentFormat supported by the client
    private final Set<ContentFormat> supportedContentFormats;
    // All supported object (object id => version)
    private final Map<Integer, Version> supportedObjects;
    // All available instances
    private final Set<LwM2mPath> availableInstances;

    protected EndDeviceRegistration(Builder builder) {
        // mandatory params
        id = builder.registrationId;
        endpoint = builder.endpoint;
        prefix = builder.prefix;
        parentGateway = builder.parentGateway;

        // object links related params
        objectLinks = builder.objectLinks;
        supportedContentFormats = builder.supportedContentFormats;
        supportedObjects = builder.supportedObjects;
        availableInstances = builder.availableInstances;
    }

    @Override
    public String getId() {
        return id;
    }

    public String getPrefix() {
        return prefix;
    }

    @Override
    public Date getRegistrationDate() {
        return parentGateway.getRegistrationDate();
    }

    @Override
    public LwM2mPeer getClientTransportData() {
        return parentGateway.getClientTransportData();
    }

    @Override
    public InetSocketAddress getSocketAddress() {
        return parentGateway.getSocketAddress();
    }

    @Override
    public InetAddress getAddress() {
        return parentGateway.getAddress();
    }

    @Override
    public Integer getPort() {
        return parentGateway.getPort();
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
        return parentGateway.getLifeTimeInSec();
    }

    @Override
    public String getSmsNumber() {
        return parentGateway.getSmsNumber();
    }

    @Override
    public LwM2mVersion getLwM2mVersion() {
        return parentGateway.getLwM2mVersion();
    }

    @Override
    public EnumSet<BindingMode> getBindingMode() {
        return parentGateway.getBindingMode();
    }

    @Override
    public Boolean getQueueMode() {
        return parentGateway.getQueueMode();
    }

    @Override
    public String getRootPath() {
        return parentGateway.getRootPath() + getPrefix();
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
        return parentGateway.getLastUpdate();
    }

    @Override
    public long getExpirationTimeStamp() {
        return parentGateway.getExpirationTimeStamp();
    }

    @Override
    public long getExpirationTimeStamp(long gracePeriodInSec) {
        return parentGateway.getExpirationTimeStamp(gracePeriodInSec);
    }

    @Override
    public boolean canInitiateConnection() {
        return parentGateway.canInitiateConnection();
    }

    @Override
    public boolean isAlive() {
        return parentGateway.isAlive();
    }

    @Override
    public boolean isAlive(long gracePeriodInSec) {
        return parentGateway.isAlive(gracePeriodInSec);
    }

    @Override
    public Map<String, String> getAdditionalRegistrationAttributes() {
        return Collections.emptyMap();
    }

    @Override
    public boolean usesQueueMode() {
        return parentGateway.usesQueueMode();
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
        return Collections.emptyMap();
    }

    @Override
    public EndpointUri getEndpointUri() {
        return parentGateway.getEndpointUri();
    }

    @Override
    public boolean isGateway() {
        return false;
    }

    @Override
    public Map<String, String> getChildEndDevices() {
        return Collections.emptyMap();
    }

    @Override
    public boolean hasChildEndDevices() {
        return false;
    }

    @Override
    public String toString() {
        return String.format(
                "EndDeviceRegistration [endpoint=%s, id=%s, prefix=%s, objectLinks=%s, supportedContentFormats=%s, supportedObjects=%s, availableInstances=%s, parentGateway=%s]",
                endpoint, id, prefix, Arrays.toString(objectLinks), supportedContentFormats, supportedObjects,
                availableInstances, parentGateway);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof EndDeviceRegistration))
            return false;
        EndDeviceRegistration that = (EndDeviceRegistration) o;
        return Objects.equals(availableInstances, that.availableInstances) && Objects.equals(endpoint, that.endpoint)
                && Objects.equals(id, that.id) && Arrays.equals(objectLinks, that.objectLinks)
                && Objects.equals(parentGateway, that.parentGateway) && Objects.equals(prefix, that.prefix)
                && Objects.equals(supportedContentFormats, that.supportedContentFormats)
                && Objects.equals(supportedObjects, that.supportedObjects);
    }

    @Override
    public int hashCode() {
        return Objects.hash(availableInstances, endpoint, id, Arrays.hashCode(objectLinks), parentGateway, prefix,
                supportedContentFormats, supportedObjects);
    }

    public static class Builder {
        private final Registration parentGateway;
        private final String registrationId;
        private final String endpoint;
        private final String prefix;
        private Link[] objectLinks;

        private Set<ContentFormat> supportedContentFormats;
        private Map<Integer, Version> supportedObjects;
        private Set<LwM2mPath> availableInstances;

        public Builder(Registration gateway, String registrationId, String prefix, String endpoint) {

            Validate.notNull(gateway);
            Validate.notNull(registrationId);
            Validate.notEmpty(endpoint);
            Validate.notEmpty(prefix);
            Validate.isTrue(gateway.isGateway(), "registration should be a gateway");

            this.parentGateway = gateway;
            this.registrationId = registrationId;
            this.endpoint = endpoint;
            this.prefix = prefix;
        }

        public Builder(Registration gateway, EndDeviceRegistration endDevice) {

            Validate.notNull(gateway);
            Validate.isTrue(gateway.isGateway(), "registration should be a gateway");

            this.parentGateway = gateway;
            this.registrationId = endDevice.id;
            this.endpoint = endDevice.endpoint;
            this.prefix = endDevice.prefix;
            this.objectLinks = endDevice.objectLinks;
            this.supportedContentFormats = endDevice.supportedContentFormats;
            this.supportedObjects = endDevice.supportedObjects;
            this.availableInstances = endDevice.availableInstances;
        }

        public Builder objectLinks(Link[] objectLinks) {
            this.objectLinks = objectLinks;
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

        public EndDeviceRegistration build() {
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
            // Create Registration
            return new EndDeviceRegistration(this);
        }
    }
}
