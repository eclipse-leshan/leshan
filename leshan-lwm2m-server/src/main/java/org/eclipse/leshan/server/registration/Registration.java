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
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.peer.IpPeer;
import org.eclipse.leshan.core.peer.LwM2mPeer;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.util.StringUtils;
import org.eclipse.leshan.core.util.Validate;

/**
 * An immutable structure which represent a LW-M2M client registration on the server
 */
public class Registration implements IRegistration {

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

    // All child devices (prefix -> endpoint)
    private final Map<String, String> endDevices;

    protected Registration(Builder builder) {

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

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.leshan.server.registration.IRegistration#getId()
     */
    @Override
    public String getId() {
        return id;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.leshan.server.registration.IRegistration#getRegistrationDate()
     */
    @Override
    public Date getRegistrationDate() {
        return registrationDate;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.leshan.server.registration.IRegistration#getClientTransportData()
     */
    @Override
    public LwM2mPeer getClientTransportData() {
        return clientTransportData;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.leshan.server.registration.IRegistration#getSocketAddress()
     */
    @Override
    public InetSocketAddress getSocketAddress() {
        if (clientTransportData instanceof IpPeer) {
            return ((IpPeer) clientTransportData).getSocketAddress();
        }
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.leshan.server.registration.IRegistration#getAddress()
     */
    @Override
    public InetAddress getAddress() {
        if (clientTransportData instanceof IpPeer) {
            return ((IpPeer) clientTransportData).getSocketAddress().getAddress();
        }
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.leshan.server.registration.IRegistration#getPort()
     */
    @Override
    public Integer getPort() {
        if (clientTransportData instanceof IpPeer) {
            return ((IpPeer) clientTransportData).getSocketAddress().getPort();
        }
        return null;
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

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.leshan.server.registration.IRegistration#getSortedObjectLinks()
     */
    @Override
    public Link[] getSortedObjectLinks() {
        // sort the list of objects
        if (objectLinks == null) {
            return null;
        }

        Link[] res = Arrays.copyOf(objectLinks, objectLinks.length);

        Arrays.sort(res, (o1, o2) -> {
            if (o1 == null && o2 == null)
                return 0;
            if (o1 == null)
                return -1;
            if (o2 == null)
                return 1;
            // by URL
            String[] url1 = o1.getUriReference().split("/");
            String[] url2 = o2.getUriReference().split("/");

            for (int i = 0; i < url1.length && i < url2.length; i++) {
                // is it two numbers?
                if (isNumber(url1[i]) && isNumber(url2[i])) {
                    int cmp = Integer.parseInt(url1[i]) - Integer.parseInt(url2[i]);
                    if (cmp != 0) {
                        return cmp;
                    }
                } else {

                    int v = url1[i].compareTo(url2[i]);

                    if (v != 0) {
                        return v;
                    }
                }
            }

            return url1.length - url2.length;
        });

        return res;
    }

    private static boolean isNumber(String s) {
        return !StringUtils.isEmpty(s) && StringUtils.isNumeric(s);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.leshan.server.registration.IRegistration#getLifeTimeInSec()
     */
    @Override
    public Long getLifeTimeInSec() {
        return lifeTimeInSec;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.leshan.server.registration.IRegistration#getSmsNumber()
     */
    @Override
    public String getSmsNumber() {
        return smsNumber;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.leshan.server.registration.IRegistration#getLwM2mVersion()
     */
    @Override
    public LwM2mVersion getLwM2mVersion() {
        return lwM2mVersion;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.leshan.server.registration.IRegistration#getBindingMode()
     */
    @Override
    public EnumSet<BindingMode> getBindingMode() {
        return bindingMode;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.leshan.server.registration.IRegistration#getQueueMode()
     */
    @Override
    public Boolean getQueueMode() {
        return queueMode;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.leshan.server.registration.IRegistration#getRootPath()
     */
    @Override
    public String getRootPath() {
        return rootPath;
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
        return lastUpdate;
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
        return lastUpdate.getTime() + lifeTimeInSec * 1000 + gracePeriodInSec * 1000;
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
        return additionalRegistrationAttributes;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.leshan.server.registration.IRegistration#usesQueueMode()
     */
    @Override
    public boolean usesQueueMode() {
        if (lwM2mVersion.olderThan(LwM2mVersion.V1_1))
            return bindingMode.contains(BindingMode.Q);
        else
            return queueMode;
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
        return customRegistrationData;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.leshan.server.registration.IRegistration#getEndpointUri()
     */
    @Override
    public EndpointUri getEndpointUri() {
        return endpointUri;
    }

    @Override
    public boolean isGateway() {
        return getSupportedVersion(25) != null;
    }

    @Override
    public Map<String, String> getChildEndDevices() {
        return endDevices;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.leshan.server.registration.IRegistration#toString()
     */
    @Override
    public String toString() {
        return String.format(
                "Registration [registrationDate=%s, clientTransportData=%s, lifeTimeInSec=%s, smsNumber=%s, lwM2mVersion=%s, bindingMode=%s, queueMode=%s, endpoint=%s, id=%s, objectLinks=%s, additionalRegistrationAttributes=%s, rootPath=%s, supportedContentFormats=%s, supportedObjects=%s, availableInstances=%s, lastUpdate=%s, customRegistrationData=%s, endpointUri=%s]",
                registrationDate, clientTransportData, lifeTimeInSec, smsNumber, lwM2mVersion, bindingMode, queueMode,
                endpoint, id, Arrays.toString(objectLinks), additionalRegistrationAttributes, rootPath,
                supportedContentFormats, supportedObjects, availableInstances, lastUpdate, customRegistrationData,
                endpointUri);
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Registration))
            return false;
        Registration that = (Registration) o;
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
                && Objects.equals(endpointUri, that.endpointUri);
    }

    @Override
    public final int hashCode() {
        return Objects.hash(registrationDate, clientTransportData, lifeTimeInSec, smsNumber, lwM2mVersion, bindingMode,
                queueMode, endpoint, id, Arrays.hashCode(objectLinks), additionalRegistrationAttributes, rootPath,
                supportedContentFormats, supportedObjects, availableInstances, lastUpdate, customRegistrationData,
                endpointUri);
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

        public Builder(Registration registration) {

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

        public Registration build() {
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
            return new Registration(this);
        }
    }
}
