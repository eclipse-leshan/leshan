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

    public RegistrationUpdate(String registrationId, Identity identity, Long lifeTimeInSec, String smsNumber,
            EnumSet<BindingMode> bindingMode, Link[] objectLinks, Map<String, String> additionalAttributes) {
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
                .availableInstances(registration.getAvailableInstances())
                .applicationData(registration.getApplicationData())
                .lastEndpointUsed(registration.getLastEndpointUsed());

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
                "RegistrationUpdate [registrationId=%s, identity=%s, lifeTimeInSec=%s, smsNumber=%s, bindingMode=%s, objectLinks=%s]",
                registrationId, identity, lifeTimeInSec, smsNumber, bindingMode, Arrays.toString(objectLinks));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((bindingMode == null) ? 0 : bindingMode.hashCode());
        result = prime * result + ((identity == null) ? 0 : identity.hashCode());
        result = prime * result + ((lifeTimeInSec == null) ? 0 : lifeTimeInSec.hashCode());
        result = prime * result + Arrays.hashCode(objectLinks);
        result = prime * result + ((registrationId == null) ? 0 : registrationId.hashCode());
        result = prime * result + ((smsNumber == null) ? 0 : smsNumber.hashCode());
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
        if (bindingMode != other.bindingMode)
            return false;
        if (identity == null) {
            if (other.identity != null)
                return false;
        } else if (!identity.equals(other.identity))
            return false;
        if (lifeTimeInSec == null) {
            if (other.lifeTimeInSec != null)
                return false;
        } else if (!lifeTimeInSec.equals(other.lifeTimeInSec))
            return false;
        if (!Arrays.equals(objectLinks, other.objectLinks))
            return false;
        if (registrationId == null) {
            if (other.registrationId != null)
                return false;
        } else if (!registrationId.equals(other.registrationId))
            return false;
        if (smsNumber == null) {
            if (other.smsNumber != null)
                return false;
        } else if (!smsNumber.equals(other.smsNumber))
            return false;
        return true;
    }
}
