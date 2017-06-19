/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.core.request;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.leshan.Link;
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

}
