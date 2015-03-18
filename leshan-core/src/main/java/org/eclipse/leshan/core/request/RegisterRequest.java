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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.PublicKey;

import org.eclipse.leshan.LinkObject;
import org.eclipse.leshan.core.response.RegisterResponse;

public class RegisterRequest implements UplinkRequest<RegisterResponse> {

    private String endpointName = null;
    private Long lifetime = null;
    private String lwVersion = null;
    private BindingMode bindingMode = null;
    private String smsNumber = null;
    private LinkObject[] objectLinks = null;
    private InetSocketAddress registrationEndpoint = null;
    private InetAddress sourceAddress = null;
    private int sourcePort;
    private String pskIdentity = null;
    private PublicKey publicKey = null;

    public RegisterRequest(String endpointName) {
        this.endpointName = endpointName;
    }

    public RegisterRequest(String endpointName, Long lifetime, String lwVersion, BindingMode bindingMode,
            String smsNumber, LinkObject[] objectLinks) {
        this.endpointName = endpointName;
        this.lifetime = lifetime;
        this.lwVersion = lwVersion;
        this.bindingMode = bindingMode;
        this.smsNumber = smsNumber;
        this.objectLinks = objectLinks;
    }

    public RegisterRequest(String endpointName, Long lifetime, String lwVersion, BindingMode bindingMode,
            String smsNumber, LinkObject[] objectLinks, InetAddress sourceAddress, int sourcePort,
            InetSocketAddress registrationEndpoint, String pskIdentity, PublicKey publicKey) {
        this.endpointName = endpointName;
        this.lifetime = lifetime;
        this.lwVersion = lwVersion;
        this.bindingMode = bindingMode;
        this.smsNumber = smsNumber;
        this.objectLinks = objectLinks;

        this.sourceAddress = sourceAddress;
        this.sourcePort = sourcePort;
        this.registrationEndpoint = registrationEndpoint;
        this.pskIdentity = pskIdentity;
        this.publicKey = publicKey;
    }

    public InetAddress getSourceAddress() {
        return sourceAddress;
    }

    public int getSourcePort() {
        return sourcePort;
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

    public InetSocketAddress getRegistrationEndpoint() {
        return registrationEndpoint;
    }

    public boolean isSecure() {
        return pskIdentity != null || publicKey != null;
    }

    public String getPskIdentity() {
        return pskIdentity;
    }

    public PublicKey getSourcePublicKey() {
        return publicKey;
    }

    @Override
    public void accept(UplinkRequestVisitor visitor) {
        visitor.visit(this);
    }
}
