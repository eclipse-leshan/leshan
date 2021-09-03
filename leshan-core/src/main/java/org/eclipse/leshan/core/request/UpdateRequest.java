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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.leshan.core.Link;
import org.eclipse.leshan.core.LwM2m.LwM2mVersion;
import org.eclipse.leshan.core.request.exception.InvalidRequestException;
import org.eclipse.leshan.core.response.UpdateResponse;

/**
 * A Lightweight M2M request for updating the LWM2M Client properties required by the LWM2M Server to contact the LWM2M
 * Client.
 */
public class UpdateRequest extends AbstractLwM2mRequest<UpdateResponse> implements UplinkRequest<UpdateResponse> {

    private final Long lifeTimeInSec;
    private final String smsNumber;
    private final EnumSet<BindingMode> bindingMode;
    private final String registrationId;
    private final Link[] objectLinks;
    private final Map<String, String> additionalAttributes;

    /**
     * Sets all fields.
     * 
     * @param registrationId the ID under which the client is registered
     * @param lifetime the number of seconds the client would like its registration to be valid
     * @param smsNumber the SMS number the client can receive messages under
     * @param binding the binding mode(s) the client supports
     * @param objectLinks the objects and object instances the client hosts/supports
     * @exception InvalidRequestException if the registrationId is empty.
     */
    public UpdateRequest(String registrationId, Long lifetime, String smsNumber, EnumSet<BindingMode> binding,
            Link[] objectLinks, Map<String, String> additionalAttributes) throws InvalidRequestException {
        this(registrationId, lifetime, smsNumber, binding, objectLinks, additionalAttributes, null);
    }

    /**
     * Sets all fields.
     * 
     * @param registrationId the ID under which the client is registered
     * @param lifetime the number of seconds the client would like its registration to be valid
     * @param smsNumber the SMS number the client can receive messages under
     * @param binding the binding mode(s) the client supports
     * @param objectLinks the objects and object instances the client hosts/supports
     * @param coapRequest the underlying request
     * 
     * @exception InvalidRequestException if the registrationId is empty.
     */
    public UpdateRequest(String registrationId, Long lifetime, String smsNumber, EnumSet<BindingMode> binding,
            Link[] objectLinks, Map<String, String> additionalAttributes, Object coapRequest)
            throws InvalidRequestException {
        super(coapRequest);
        if (registrationId == null || registrationId.isEmpty())
            throw new InvalidRequestException("registrationId is mandatory");

        this.registrationId = registrationId;
        this.objectLinks = objectLinks;
        this.lifeTimeInSec = lifetime;
        this.bindingMode = binding;
        this.smsNumber = smsNumber;
        if (additionalAttributes == null)
            this.additionalAttributes = Collections.emptyMap();
        else
            this.additionalAttributes = Collections.unmodifiableMap(new HashMap<>(additionalAttributes));
    }

    public String getRegistrationId() {
        return registrationId;
    }

    public Link[] getObjectLinks() {
        return objectLinks;
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

    public Map<String, String> getAdditionalAttributes() {
        return additionalAttributes;
    }

    public void validate(LwM2mVersion targetedVersion) {
        if (bindingMode != null) {
            String err = BindingMode.isValidFor(bindingMode, targetedVersion);
            if (err != null) {
                throw new InvalidRequestException("Invalid Binding mode: %s", err);
            }
        }
    }

    @Override
    public void accept(UplinkRequestVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((additionalAttributes == null) ? 0 : additionalAttributes.hashCode());
        result = prime * result + ((bindingMode == null) ? 0 : bindingMode.hashCode());
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
        UpdateRequest other = (UpdateRequest) obj;
        if (additionalAttributes == null) {
            if (other.additionalAttributes != null)
                return false;
        } else if (!additionalAttributes.equals(other.additionalAttributes))
            return false;
        if (bindingMode != other.bindingMode)
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

    @Override
    public String toString() {
        return String.format(
                "UpdateRequest [registrationId=%s, lifeTimeInSec=%s, smsNumber=%s, bindingMode=%s, objectLinks=%s, additionalAttributes=%s]",
                registrationId, lifeTimeInSec, smsNumber, bindingMode, Arrays.toString(objectLinks),
                additionalAttributes);
    }
}
