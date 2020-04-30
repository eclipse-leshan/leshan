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
 *******************************************************************************/
package org.eclipse.leshan.core.request;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.leshan.core.Link;
import org.eclipse.leshan.core.request.exception.InvalidRequestException;
import org.eclipse.leshan.core.response.RegisterResponse;

/**
 * A Lightweight M2M request for sending the LWM2M Client properties required by the LWM2M Server to contact the LWM2M
 * Client.
 */
public class RegisterRequest implements UplinkRequest<RegisterResponse> {

    private final String endpointName;
    private final Long lifetime;
    private final String lwVersion;
    private final BindingMode bindingMode;
    private final String smsNumber;
    private final Link[] objectLinks;
    private final Map<String, String> additionalAttributes;

    /**
     * Creates a request for registering a LWM2M client to a LWM2M Server.
     * 
     * @param endpointName is the LWM2M client identifier.
     * @param lifetime specifies the lifetime of the registration in seconds.
     * @param lwVersion indicates the version of the LWM2M Enabler that the LWM2M Client supports.
     * @param bindingMode indicates current {@link BindingMode} of the LWM2M Client.
     * @param smsNumber is the MSISDN where the LWM2M Client can be reached for use with the SMS binding.
     * @param objectLinks is the list of Objects supported and Object Instances available on the LWM2M Client.
     * @param additionalAttributes are any attributes/parameters which is out of the LWM2M specification.
     * @exception InvalidRequestException if endpoint name or objectlinks is empty.
     */
    public RegisterRequest(String endpointName, Long lifetime, String lwVersion, BindingMode bindingMode,
            String smsNumber, Link[] objectLinks, Map<String, String> additionalAttributes)
            throws InvalidRequestException {

        if (endpointName == null || endpointName.isEmpty())
            throw new InvalidRequestException("endpoint is mandatory");

        if (objectLinks == null || objectLinks.length == 0)
            throw new InvalidRequestException(
                    "supported object list is mandatory and mandatory objects should be present for endpoint %s",
                    endpointName);

        this.endpointName = endpointName;
        this.lifetime = lifetime;
        this.lwVersion = lwVersion;
        this.bindingMode = bindingMode;
        this.smsNumber = smsNumber;
        this.objectLinks = objectLinks;
        if (additionalAttributes == null)
            this.additionalAttributes = Collections.emptyMap();
        else
            this.additionalAttributes = Collections.unmodifiableMap(new HashMap<>(additionalAttributes));
    }

    public String getEndpointName() {
        return endpointName;
    }

    public Long getLifetime() {
        return lifetime;
    }

    public String getLwVersion() {
        return lwVersion;
    }

    public BindingMode getBindingMode() {
        return bindingMode;
    }

    public String getSmsNumber() {
        return smsNumber;
    }

    public Link[] getObjectLinks() {
        return objectLinks;
    }

    public Map<String, String> getAdditionalAttributes() {
        return additionalAttributes;
    }

    @Override
    public void accept(UplinkRequestVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return String.format(
                "RegisterRequest [endpointName=%s, lifetime=%s, lwVersion=%s, bindingMode=%s, smsNumber=%s, objectLinks=%s, additionalAttributes=%s]",
                endpointName, lifetime, lwVersion, bindingMode, smsNumber, Arrays.toString(objectLinks),
                additionalAttributes);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((additionalAttributes == null) ? 0 : additionalAttributes.hashCode());
        result = prime * result + ((bindingMode == null) ? 0 : bindingMode.hashCode());
        result = prime * result + ((endpointName == null) ? 0 : endpointName.hashCode());
        result = prime * result + ((lifetime == null) ? 0 : lifetime.hashCode());
        result = prime * result + ((lwVersion == null) ? 0 : lwVersion.hashCode());
        result = prime * result + Arrays.hashCode(objectLinks);
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
        RegisterRequest other = (RegisterRequest) obj;
        if (additionalAttributes == null) {
            if (other.additionalAttributes != null)
                return false;
        } else if (!additionalAttributes.equals(other.additionalAttributes))
            return false;
        if (bindingMode != other.bindingMode)
            return false;
        if (endpointName == null) {
            if (other.endpointName != null)
                return false;
        } else if (!endpointName.equals(other.endpointName))
            return false;
        if (lifetime == null) {
            if (other.lifetime != null)
                return false;
        } else if (!lifetime.equals(other.lifetime))
            return false;
        if (lwVersion == null) {
            if (other.lwVersion != null)
                return false;
        } else if (!lwVersion.equals(other.lwVersion))
            return false;
        if (!Arrays.equals(objectLinks, other.objectLinks))
            return false;
        if (smsNumber == null) {
            if (other.smsNumber != null)
                return false;
        } else if (!smsNumber.equals(other.smsNumber))
            return false;
        return true;
    }
}
