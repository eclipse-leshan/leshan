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

import org.eclipse.leshan.core.link.Link;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.util.Validate;

/**
 * A container object for updating a LW-M2M client's registration properties on the server.
 */
public class RegistrationUpdate {

    private final String registrationId;

    private final Identity identity;
    private final Long lifeTimeInSec;
    private final String smsNumber;
    private final EnumSet<BindingMode> bindingMode;
    private final Link[] objectLinks;
    private final Map<String, String> additionalAttributes;
    private final Map<String, String> applicationData;

    public RegistrationUpdate(String registrationId, Identity identity, Long lifeTimeInSec, String smsNumber,
            EnumSet<BindingMode> bindingMode, Link[] objectLinks, Map<String, String> additionalAttributes,
            Map<String, String> applicationData) {
        Validate.notNull(registrationId);
        Validate.notNull(identity);
        this.registrationId = registrationId;
        this.identity = identity;
        this.lifeTimeInSec = lifeTimeInSec;
        this.smsNumber = smsNumber;
        this.bindingMode = bindingMode;
        this.objectLinks = objectLinks;
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
        Identity identity = this.identity != null ? this.identity : registration.getIdentity();
        Link[] linkObject = this.objectLinks != null ? this.objectLinks : registration.getObjectLinks();
        long lifeTimeInSec = this.lifeTimeInSec != null ? this.lifeTimeInSec : registration.getLifeTimeInSec();
        EnumSet<BindingMode> bindingMode = this.bindingMode != null ? this.bindingMode : registration.getBindingMode();
        String smsNumber = this.smsNumber != null ? this.smsNumber : registration.getSmsNumber();

        Map<String, String> additionalAttributes = this.additionalAttributes.isEmpty()
                ? registration.getAdditionalRegistrationAttributes()
                : updateAdditionalAttributes(registration.getAdditionalRegistrationAttributes());

        Map<String, String> applicationData = this.applicationData.isEmpty() ? registration.getApplicationData()
                : this.applicationData;

        // this needs to be done in any case, even if no properties have changed, in order
        // to extend the client registration time-to-live period ...
        Date lastUpdate = new Date();

        Registration.Builder builder = new Registration.Builder(registration.getId(), registration.getEndpoint(),
                identity);
        builder.extractDataFromObjectLink(this.objectLinks != null); // we parse object link only if there was updated.

        builder.lwM2mVersion(registration.getLwM2mVersion()).lifeTimeInSec(lifeTimeInSec).smsNumber(smsNumber)
                .bindingMode(bindingMode).queueMode(registration.getQueueMode()).objectLinks(linkObject)
                .registrationDate(registration.getRegistrationDate()).lastUpdate(lastUpdate)
                .additionalRegistrationAttributes(additionalAttributes).rootPath(registration.getRootPath())
                .supportedContentFormats(registration.getSupportedContentFormats())
                .supportedObjects(registration.getSupportedObject())
                .availableInstances(registration.getAvailableInstances()).applicationData(applicationData);

        return builder.build();

    }

    public String getRegistrationId() {
        return registrationId;
    }

    public Identity getIdentity() {
        return identity;
    }

    public InetAddress getAddress() {
        return identity.getPeerAddress().getAddress();
    }

    public Integer getPort() {
        return identity.getPeerAddress().getPort();
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

    private Map<String, String> updateAdditionalAttributes(Map<String, String> oldAdditionalAttributes) {

        // putAll method updates already present key values or add them if not present.
        Map<String, String> aux = new HashMap<String, String>();
        aux.putAll(oldAdditionalAttributes);
        aux.putAll(this.additionalAttributes);
        return aux;
    }

    @Override
    public String toString() {
        return "RegistrationUpdate [registrationId=" + registrationId + ", identity=" + identity + ", lifeTimeInSec="
                + lifeTimeInSec + ", smsNumber=" + smsNumber + ", bindingMode=" + bindingMode + ", objectLinks="
                + Arrays.toString(objectLinks) + ", additionalAttributes=" + additionalAttributes + ", applicationData="
                + applicationData + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(objectLinks);
        result = prime * result + Objects.hash(additionalAttributes, applicationData, bindingMode, identity,
                lifeTimeInSec, registrationId, smsNumber);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        RegistrationUpdate other = (RegistrationUpdate) obj;
        return Objects.equals(additionalAttributes, other.additionalAttributes)
                && Objects.equals(applicationData, other.applicationData)
                && Objects.equals(bindingMode, other.bindingMode) && Objects.equals(identity, other.identity)
                && Objects.equals(lifeTimeInSec, other.lifeTimeInSec) && Arrays.equals(objectLinks, other.objectLinks)
                && Objects.equals(registrationId, other.registrationId) && Objects.equals(smsNumber, other.smsNumber);
    }
}
