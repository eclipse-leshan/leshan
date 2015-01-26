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
import java.util.Date;

import org.eclipse.leshan.LinkObject;
import org.eclipse.leshan.core.response.LwM2mResponse;

public class UpdateRequest implements UplinkRequest<LwM2mResponse> {

    private final InetAddress address;
    private final Integer port;
    private final Long lifeTimeInSec;
    private final String smsNumber;
    private final BindingMode bindingMode;
    private final String registrationId;
    private final LinkObject[] objectLinks;

    public UpdateRequest(String registrationId, InetAddress address, Integer port) {
        this(registrationId, address, port, null, null, null, null);
    }

    public UpdateRequest(String registrationId, InetAddress address, Integer port, Long lifetime, String smsNumber,
            BindingMode binding, LinkObject[] objectLinks) {
        this(registrationId, address, port, lifetime, smsNumber, binding, objectLinks, null);
    }

    public UpdateRequest(String registrationId, Long lifetime, String smsNumber, BindingMode binding,
            LinkObject[] objectLinks) {
        this(registrationId, null, null, lifetime, smsNumber, binding, objectLinks, null);
    }

    /**
     * Sets all fields.
     * 
     * @param registrationId the ID under which the client is registered
     * @param address the client's host name or IP address
     * @param port the UDP port the client uses for communication
     * @param lifetime the number of seconds the client would like its registration to be valid
     * @param smsNumber the SMS number the client can receive messages under
     * @param binding the binding mode(s) the client supports
     * @param objectLinks the objects and object instances the client hosts/supports
     * @param registrationDate the point in time the client registered with the server (?)
     * @throws NullPointerException if the registration ID is <code>null</code>
     */
    public UpdateRequest(String registrationId, InetAddress address, Integer port, Long lifetime, String smsNumber,
            BindingMode binding, LinkObject[] objectLinks, Date registrationDate) {

        if (registrationId == null) {
            throw new NullPointerException("Registration ID must not be null");
        }
        this.registrationId = registrationId;
        this.address = address;
        this.port = port;
        this.objectLinks = objectLinks;
        this.lifeTimeInSec = lifetime;
        this.bindingMode = binding;
        this.smsNumber = smsNumber;
    }

    public String getRegistrationId() {
        return registrationId;
    }

    public InetAddress getAddress() {
        return address;
    }

    public Integer getPort() {
        return port;
    }

    public LinkObject[] getObjectLinks() {
        return objectLinks;
    }

    public Long getLifeTimeInSec() {
        return lifeTimeInSec;
    }

    public String getSmsNumber() {
        return smsNumber;
    }

    public BindingMode getBindingMode() {
        return bindingMode;
    }

    @Override
    public void accept(UplinkRequestVisitor visitor) {
        visitor.visit(this);
    }
}
