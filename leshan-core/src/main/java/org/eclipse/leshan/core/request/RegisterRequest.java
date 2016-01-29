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
    private final Map<String, String> additionalParameters;

    public RegisterRequest(String endpointName) {
        this.endpointName = endpointName;
        this.lifetime = null;
        this.lwVersion = null;
        this.bindingMode = null;
        this.smsNumber = null;
        this.objectLinks = null;
        this.additionalParameters = Collections.unmodifiableMap(new HashMap<String, String>());
    }

    public RegisterRequest(String endpointName, Long lifetime, String lwVersion, BindingMode bindingMode,
            String smsNumber, LinkObject[] objectLinks, Map<String, String> additionalParameters) {
        this.endpointName = endpointName;
        this.lifetime = lifetime;
        this.lwVersion = lwVersion;
        this.bindingMode = bindingMode;
        this.smsNumber = smsNumber;
        this.objectLinks = objectLinks;
        this.additionalParameters = Collections.unmodifiableMap(additionalParameters);
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

    public Map<String, String> getAdditionalParameters() {
        return additionalParameters;
    }

    @Override
    public void accept(UplinkRequestVisitor visitor) {
        visitor.visit(this);
    }

}
