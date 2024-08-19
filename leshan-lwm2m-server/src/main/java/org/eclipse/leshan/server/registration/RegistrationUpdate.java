/*******************************************************************************
 * Copyright (c) 2015 Sierra Wireless and others.
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
 *     Achim Kraus (Bosch Software Innovations GmbH) - add Identity as destination
 *     Michał Wadowski (Orange) - Improved compliance with rfc6690
 *******************************************************************************/
package org.eclipse.leshan.server.registration;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.eclipse.leshan.core.LwM2m.Version;
import org.eclipse.leshan.core.link.Link;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.peer.IpPeer;
import org.eclipse.leshan.core.peer.LwM2mPeer;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.util.Validate;

/**
 * A container object for updating a LW-M2M client's registration properties on the server.
 */
public class RegistrationUpdate {

    private final String registrationId;

    private final LwM2mPeer clientTransportData;
    private final Long lifeTimeInSec;
    private final String smsNumber;
    private final EnumSet<BindingMode> bindingMode;
    private final Link[] objectLinks;
    // The location where LWM2M objects are hosted on the device
    private final String alternatePath;
    // All ContentFormat supported by the client
    private final Set<ContentFormat> supportedContentFormats;
    // All supported object (object id => version)
    private final Map<Integer, Version> supportedObjects;
    // All available instances
    private final Set<LwM2mPath> availableInstances;
    private final Map<String, String> additionalAttributes;
    private final Map<String, String> applicationData;

    public RegistrationUpdate(String registrationId, LwM2mPeer clientTransportData, Long lifeTimeInSec,
            String smsNumber, EnumSet<BindingMode> bindingMode, Link[] objectLinks, String alternatePath,
            Set<ContentFormat> supportedContentFormats, Map<Integer, Version> supportedObjects,
            Set<LwM2mPath> availableInstances, Map<String, String> additionalAttributes,
            Map<String, String> applicationData) {

        // mandatory params
        Validate.notNull(registrationId);
        Validate.notNull(clientTransportData);
        this.registrationId = registrationId;
        this.clientTransportData = clientTransportData;

        // others params
        this.lifeTimeInSec = lifeTimeInSec;
        this.smsNumber = smsNumber;
        this.bindingMode = bindingMode;

        // object links related params
        this.objectLinks = objectLinks;
        this.alternatePath = alternatePath;
        this.supportedContentFormats = supportedContentFormats;
        this.supportedObjects = supportedObjects;
        this.availableInstances = availableInstances;

        // out of spec data
        if (additionalAttributes == null)
            this.additionalAttributes = Collections.emptyMap();
        else
            this.additionalAttributes = Collections.unmodifiableMap(new HashMap<>(additionalAttributes));
        if (applicationData == null)
            this.applicationData = Collections.emptyMap();
        else
            this.applicationData = Collections.unmodifiableMap(new HashMap<>(applicationData));
    }

    /**
     * Returns an updated version of the registration.
     *
     * @param registration the registration to update
     * @return the updated registration
     */
    public Registration update(Registration registration) {
        LwM2mPeer transportData = this.clientTransportData != null ? this.clientTransportData
                : registration.getClientTransportData();
        long lifeTimeInSec = this.lifeTimeInSec != null ? this.lifeTimeInSec : registration.getLifeTimeInSec();
        EnumSet<BindingMode> bindingMode = this.bindingMode != null ? this.bindingMode : registration.getBindingMode();
        String smsNumber = this.smsNumber != null ? this.smsNumber : registration.getSmsNumber();

        Link[] linkObject = this.objectLinks != null ? this.objectLinks : registration.getObjectLinks();
        String alternatePath = this.alternatePath != null ? this.alternatePath : registration.getRootPath();
        Set<ContentFormat> supportedContentFormats = this.supportedContentFormats != null ? this.supportedContentFormats
                : registration.getSupportedContentFormats();
        Map<Integer, Version> supportedObjects = this.supportedObjects != null ? this.supportedObjects
                : registration.getSupportedObject();
        Set<LwM2mPath> availableInstances = this.availableInstances != null ? this.availableInstances
                : registration.getAvailableInstances();

        Map<String, String> additionalAttributes = this.additionalAttributes.isEmpty()
                ? registration.getAdditionalRegistrationAttributes()
                : updateAdditionalAttributes(registration.getAdditionalRegistrationAttributes());

        Map<String, String> applicationData = this.applicationData.isEmpty() ? registration.getApplicationData()
                : this.applicationData;

        // this needs to be done in any case, even if no properties have changed, in order
        // to extend the client registration time-to-live period ...
        Date lastUpdate = new Date();

        Registration.Builder builder = new Registration.Builder(registration.getId(), registration.getEndpoint(),
                transportData, registration.getLastEndpointUsed());

        builder.registrationDate(lastUpdate)
                // unmodifiable data
                .lwM2mVersion(registration.getLwM2mVersion()) //
                .queueMode(registration.getQueueMode()) //
                .registrationDate(registration.getRegistrationDate())
                // modifiable data
                .lifeTimeInSec(lifeTimeInSec) //
                .bindingMode(bindingMode) //
                .smsNumber(smsNumber) //
                // object link data
                .objectLinks(linkObject) //
                .rootPath(alternatePath) //
                .supportedContentFormats(supportedContentFormats) //
                .supportedObjects(supportedObjects) //
                .availableInstances(availableInstances) //
                // out of spec data
                .additionalRegistrationAttributes(additionalAttributes) //
                .applicationData(applicationData);

        return builder.build();
    }

    public String getRegistrationId() {
        return registrationId;
    }

    public LwM2mPeer getClientTransportData() {
        return clientTransportData;
    }

    public InetAddress getAddress() {
        if (clientTransportData instanceof IpPeer) {
            return ((IpPeer) clientTransportData).getSocketAddress().getAddress();
        }
        return null;
    }

    public Integer getPort() {
        if (clientTransportData instanceof IpPeer) {
            return ((IpPeer) clientTransportData).getSocketAddress().getPort();
        }
        return null;
    }

    public Long getLifeTimeInSec() {
        return lifeTimeInSec;
    }

    public String getSmsNumber() {
        return smsNumber;
    }

    public EnumSet<BindingMode> getBindingMode() {
        return bindingMode;
    }

    public Link[] getObjectLinks() {
        return objectLinks;
    }

    public Map<String, String> getAdditionalAttributes() {
        return additionalAttributes;
    }

    public Map<String, String> getApplicationData() {
        return applicationData;
    }

    public String getAlternatePath() {
        return alternatePath;
    }

    public Set<ContentFormat> getSupportedContentFormats() {
        return supportedContentFormats;
    }

    public Map<Integer, Version> getSupportedObjects() {
        return supportedObjects;
    }

    public Set<LwM2mPath> getAvailableInstances() {
        return availableInstances;
    }

    private Map<String, String> updateAdditionalAttributes(Map<String, String> oldAdditionalAttributes) {
        // putAll method updates already present key values or add them if not present.
        Map<String, String> aux = new HashMap<String, String>();
        aux.putAll(oldAdditionalAttributes);
        aux.putAll(this.additionalAttributes);
        return aux;
    }

    @Override
    public String toString() {
        return String.format(
                "RegistrationUpdate [registrationId=%s, clientTransportData=%s, lifeTimeInSec=%s, smsNumber=%s, bindingMode=%s, objectLinks=%s, alternatePath=%s, supportedContentFormats=%s, supportedObjects=%s, availableInstances=%s, additionalAttributes=%s, applicationData=%s]",
                registrationId, clientTransportData, lifeTimeInSec, smsNumber, bindingMode,
                Arrays.toString(objectLinks), alternatePath, supportedContentFormats, supportedObjects,
                availableInstances, additionalAttributes, applicationData);
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RegistrationUpdate)) return false;
        RegistrationUpdate that = (RegistrationUpdate) o;
        return Objects.equals(registrationId, that.registrationId) && Objects.equals(clientTransportData, that.clientTransportData) && Objects.equals(lifeTimeInSec, that.lifeTimeInSec) && Objects.equals(smsNumber, that.smsNumber) && Objects.equals(bindingMode, that.bindingMode) && Objects.deepEquals(objectLinks, that.objectLinks) && Objects.equals(alternatePath, that.alternatePath) && Objects.equals(supportedContentFormats, that.supportedContentFormats) && Objects.equals(supportedObjects, that.supportedObjects) && Objects.equals(availableInstances, that.availableInstances) && Objects.equals(additionalAttributes, that.additionalAttributes) && Objects.equals(applicationData, that.applicationData);
    }

    @Override
    public final int hashCode() {
        return Objects.hash(registrationId, clientTransportData, lifeTimeInSec, smsNumber, bindingMode, Arrays.hashCode(objectLinks), alternatePath, supportedContentFormats, supportedObjects, availableInstances, additionalAttributes, applicationData);
    }
}
