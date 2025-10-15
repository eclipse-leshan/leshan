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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.leshan.core.LwM2m.LwM2mVersion;
import org.eclipse.leshan.core.LwM2m.Version;
import org.eclipse.leshan.core.endpoint.EndpointUri;
import org.eclipse.leshan.core.link.Link;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.peer.LwM2mPeer;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.util.Validate;

/**
 * An immutable structure which represent a LW-M2M client registration on the server
 */
public class EndDeviceRegistration implements IRegistration {

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

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.leshan.server.registration.IRegistration#getId()
     */
    @Override
    public String getId() {
        return id;
    }

    public String getPrefix() {
        return prefix;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.leshan.server.registration.IRegistration#getRegistrationDate()
     */
    @Override
    public Date getRegistrationDate() {
        return parentGateway.getRegistrationDate();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.leshan.server.registration.IRegistration#getClientTransportData()
     */
    @Override
    public LwM2mPeer getClientTransportData() {
        return parentGateway.getClientTransportData();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.leshan.server.registration.IRegistration#getSocketAddress()
     */
    @Override
    public InetSocketAddress getSocketAddress() {
        return parentGateway.getSocketAddress();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.leshan.server.registration.IRegistration#getAddress()
     */
    @Override
    public InetAddress getAddress() {
        return parentGateway.getAddress();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.leshan.server.registration.IRegistration#getPort()
     */
    @Override
    public Integer getPort() {
        return parentGateway.getPort();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.leshan.server.registration.IRegistration#getObjectLinks()
     */
    @Override
    public Link[] getObjectLinks() {
        return objectLinks;
    }

    @Override
    public Link[] getSortedObjectLinks() {
        // TODO should we remove it OR implement it
        return objectLinks;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.leshan.server.registration.IRegistration#getLifeTimeInSec()
     */
    @Override
    public Long getLifeTimeInSec() {
        return parentGateway.getLifeTimeInSec();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.leshan.server.registration.IRegistration#getSmsNumber()
     */
    @Override
    public String getSmsNumber() {
        return parentGateway.getSmsNumber();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.leshan.server.registration.IRegistration#getLwM2mVersion()
     */
    @Override
    public LwM2mVersion getLwM2mVersion() {
        return parentGateway.getLwM2mVersion();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.leshan.server.registration.IRegistration#getBindingMode()
     */
    @Override
    public EnumSet<BindingMode> getBindingMode() {
        return parentGateway.getBindingMode();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.leshan.server.registration.IRegistration#getQueueMode()
     */
    @Override
    public Boolean getQueueMode() {
        return parentGateway.getQueueMode();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.leshan.server.registration.IRegistration#getRootPath()
     */
    @Override
    public String getRootPath() {
        return parentGateway.getRootPath();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.leshan.server.registration.IRegistration#getSupportedContentFormats()
     */
    @Override
    public Set<ContentFormat> getSupportedContentFormats() {
        return supportedContentFormats;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.leshan.server.registration.IRegistration#getAvailableInstances()
     */
    @Override
    public Set<LwM2mPath> getAvailableInstances() {
        return availableInstances;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.leshan.server.registration.IRegistration#getEndpoint()
     */
    @Override
    public String getEndpoint() {
        return endpoint;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.leshan.server.registration.IRegistration#getLastUpdate()
     */
    @Override
    public Date getLastUpdate() {
        return parentGateway.getLastUpdate();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.leshan.server.registration.IRegistration#getExpirationTimeStamp()
     */
    @Override
    public long getExpirationTimeStamp() {
        return getExpirationTimeStamp(0L);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.leshan.server.registration.IRegistration#getExpirationTimeStamp(long)
     */
    @Override
    public long getExpirationTimeStamp(long gracePeriodInSec) {
        return getLastUpdate().getTime() + getLifeTimeInSec() * 1000 + gracePeriodInSec * 1000;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.leshan.server.registration.IRegistration#canInitiateConnection()
     */
    @Override
    public boolean canInitiateConnection() {
        // We consider that initiates a connection (acting as DTLS client to initiate a handshake) does not make sense
        // for QueueMode as if we lost connection device is probably absent.
        return !usesQueueMode();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.leshan.server.registration.IRegistration#isAlive()
     */
    @Override
    public boolean isAlive() {
        return isAlive(0);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.leshan.server.registration.IRegistration#isAlive(long)
     */
    @Override
    public boolean isAlive(long gracePeriodInSec) {
        return getExpirationTimeStamp(gracePeriodInSec) > System.currentTimeMillis();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.leshan.server.registration.IRegistration#getAdditionalRegistrationAttributes()
     */
    @Override
    public Map<String, String> getAdditionalRegistrationAttributes() {
        return Collections.emptyMap();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.leshan.server.registration.IRegistration#usesQueueMode()
     */
    @Override
    public boolean usesQueueMode() {
        return parentGateway.usesQueueMode();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.leshan.server.registration.IRegistration#getSupportedVersion(java.lang.Integer)
     */
    @Override
    public Version getSupportedVersion(Integer objectid) {
        return getSupportedObject().get(objectid);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.leshan.server.registration.IRegistration#getSupportedObject()
     */
    @Override
    public Map<Integer, Version> getSupportedObject() {
        return supportedObjects;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.leshan.server.registration.IRegistration#getCustomRegistrationData()
     */
    @Override
    public Map<String, String> getCustomRegistrationData() {
        return Collections.emptyMap();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.leshan.server.registration.IRegistration#getEndpointUri()
     */
    @Override
    public EndpointUri getEndpointUri() {
        return parentGateway.getEndpointUri();
    }

    @Override
    public boolean isGateway() {
        return false;
    }

    @Override
    public List<IRegistration> getChildEndDevices() {
        return Collections.emptyList();
    }

    @Override
    public IRegistration getChildEndDevices(String prefix) {
        return null;
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

        public Builder(IRegistration gateway, String registrationId, String prefix, String endpoint) {

            Validate.notNull(gateway);
            Validate.notNull(registrationId);
            Validate.notEmpty(endpoint);
            Validate.notEmpty(prefix);
            Validate.isTrue(gateway.isGateway(), "registration should be a gateway");

            this.parentGateway = (Registration) gateway;
            this.registrationId = registrationId;
            this.endpoint = endpoint;
            this.prefix = prefix;
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
