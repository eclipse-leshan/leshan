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
 *     Michał Wadowski (Orange) - Improved compliance with rfc6690
 *******************************************************************************/
package org.eclipse.leshan.core.request;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.eclipse.leshan.core.LwM2m.LwM2mVersion;
import org.eclipse.leshan.core.link.Link;
import org.eclipse.leshan.core.request.exception.InvalidRequestException;
import org.eclipse.leshan.core.response.UpdateResponse;

/**
 * A Lightweight M2M request for updating the LWM2M Client properties required by the LWM2M Server to contact the LWM2M
 * Client.
 */
public class UpdateRequest extends AbstractLwM2mRequest<UpdateResponse>
        implements UplinkDeviceManagementRequest<UpdateResponse> {

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
    public void accept(UplinkDeviceManagementRequestVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return String.format(
                "UpdateRequest [registrationId=%s, lifeTimeInSec=%s, smsNumber=%s, bindingMode=%s, objectLinks=%s, additionalAttributes=%s]",
                registrationId, lifeTimeInSec, smsNumber, bindingMode, Arrays.toString(objectLinks),
                additionalAttributes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof UpdateRequest))
            return false;
        UpdateRequest that = (UpdateRequest) o;
        return that.canEqual(this) && Objects.equals(lifeTimeInSec, that.lifeTimeInSec)
                && Objects.equals(smsNumber, that.smsNumber) && Objects.equals(bindingMode, that.bindingMode)
                && Objects.equals(registrationId, that.registrationId)
                && Objects.deepEquals(objectLinks, that.objectLinks)
                && Objects.equals(additionalAttributes, that.additionalAttributes);
    }

    public boolean canEqual(Object o) {
        return (o instanceof UpdateRequest);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lifeTimeInSec, smsNumber, bindingMode, registrationId, Arrays.hashCode(objectLinks),
                additionalAttributes);
    }
}
