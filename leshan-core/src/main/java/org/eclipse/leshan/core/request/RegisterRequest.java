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

import org.eclipse.leshan.LinkObject;
import org.eclipse.leshan.core.response.RegisterResponse;
import org.eclipse.leshan.util.Validate;

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
    private final LinkObject[] objectLinks;
    private final Map<String, String> additionalAttributes;

    public RegisterRequest(String endpointName, Long lifetime, String lwVersion, BindingMode bindingMode,
            String smsNumber, LinkObject[] objectLinks, Map<String, String> additionalAttributes) {

        Validate.notNull(endpointName);
        Validate.noNullElements(objectLinks);

        this.endpointName = endpointName;
        this.lifetime = lifetime;
        this.lwVersion = lwVersion;
        this.bindingMode = bindingMode;
        this.smsNumber = smsNumber;
        this.objectLinks = objectLinks;
        if (additionalAttributes == null)
            this.additionalAttributes = Collections.unmodifiableMap(new HashMap<String, String>());
        else
            this.additionalAttributes = Collections.unmodifiableMap(additionalAttributes);
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

    public LinkObject[] getObjectLinks() {
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
