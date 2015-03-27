/*******************************************************************************
 * Copyright (c) 2015 Sierra Wireless and others.
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
package org.eclipse.leshan.server.client;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.Date;

import org.eclipse.leshan.LinkObject;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.util.Validate;

/**
 * A container object for updating a LW-M2M client's registration properties on the server.
 */
public class ClientUpdate {

    private final String registrationId;

    private final InetAddress address;
    private final Integer port;

    private final Long lifeTimeInSec;
    private final String smsNumber;
    private final BindingMode bindingMode;
    private final LinkObject[] objectLinks;

    public ClientUpdate(String registrationId, InetAddress address, Integer port, Long lifeTimeInSec, String smsNumber,
            BindingMode bindingMode, LinkObject[] objectLinks) {
        Validate.notNull(registrationId);
        this.registrationId = registrationId;
        this.address = address;
        this.port = port;
        this.lifeTimeInSec = lifeTimeInSec;
        this.smsNumber = smsNumber;
        this.bindingMode = bindingMode;
        this.objectLinks = objectLinks;
    }

    /**
     * Returns an updated version of the client.
     * 
     * @param client the registered client
     * @return the updated client
     */
    public Client updateClient(Client client) {
        InetAddress address = this.address != null ? this.address : client.getAddress();
        int port = this.port != null ? this.port : client.getPort();
        LinkObject[] linkObject = this.objectLinks != null ? this.objectLinks : client.getObjectLinks();
        long lifeTimeInSec = this.lifeTimeInSec != null ? this.lifeTimeInSec : client.getLifeTimeInSec();
        BindingMode bindingMode = this.bindingMode != null ? this.bindingMode : client.getBindingMode();
        String smsNumber = this.smsNumber != null ? this.smsNumber : client.getSmsNumber();

        // this needs to be done in any case, even if no properties have changed, in order
        // to extend the client registration's time-to-live period ...
        Date lastUpdate = new Date();

        return new Client(client.getRegistrationId(), client.getEndpoint(), address, port, client.getLwM2mVersion(),
                lifeTimeInSec, smsNumber, bindingMode, linkObject, client.getRegistrationEndpointAddress(),
                client.getRegistrationDate(), lastUpdate);
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

    public Long getLifeTimeInSec() {
        return lifeTimeInSec;
    }

    public String getSmsNumber() {
        return smsNumber;
    }

    public BindingMode getBindingMode() {
        return bindingMode;
    }

    public LinkObject[] getObjectLinks() {
        return objectLinks;
    }

    @Override
    public String toString() {
        return String
                .format("ClientUpdate [registrationId=%s, address=%s, port=%s, lifeTimeInSec=%s, smsNumber=%s, bindingMode=%s, objectLinks=%s]",
                        registrationId, address, port, lifeTimeInSec, smsNumber, bindingMode,
                        Arrays.toString(objectLinks));
    }
}
