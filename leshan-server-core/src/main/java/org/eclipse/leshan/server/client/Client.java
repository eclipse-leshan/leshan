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
package org.eclipse.leshan.server.client;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;

import org.eclipse.leshan.LinkObject;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.util.Validate;

/**
 * An immutable structure which represent a LW-M2M client registered on the server
 */
public class Client {

    private static final long DEFAULT_LIFETIME_IN_SEC = 86400L;

    private static final String DEFAULT_LWM2M_VERSION = "1.0";

    private final Date registrationDate;

    private final InetAddress address;

    private final int port;

    /*
     * The address of the LWM2M Server's CoAP end point the client used to register.
     */
    private final InetSocketAddress registrationEndpointAddress;

    private final long lifeTimeInSec;

    private final String smsNumber;

    private final String lwM2mVersion;

    private final BindingMode bindingMode;

    /**
     * The LWM2M Client's unique end point name.
     */
    private final String endpoint;

    private final String registrationId;

    private final LinkObject[] objectLinks;

    /** The location where LWM2M objects are hosted on the device */
    private final String rootPath;

    private final Date lastUpdate;

    public Client(String registrationId, String endpoint, InetAddress address, int port,
            InetSocketAddress registrationEndpointAddress) {
        this(registrationId, endpoint, address, port, null, null, null, null, null, registrationEndpointAddress);
    }

    public Client(String registrationId, String endpoint, InetAddress address, int port, String lwM2mVersion,
            Long lifetimeInSec, String smsNumber, BindingMode bindingMode, LinkObject[] objectLinks,
            InetSocketAddress registrationEndpointAddress) {
        this(registrationId, endpoint, address, port, lwM2mVersion, lifetimeInSec, smsNumber, bindingMode, objectLinks,
                registrationEndpointAddress, null, null);
    }

    public Client(String registrationId, String endpoint, InetAddress address, int port, String lwM2mVersion,
            Long lifetimeInSec, String smsNumber, BindingMode bindingMode, LinkObject[] objectLinks,
            InetSocketAddress registrationEndpointAddress, Date registrationDate, Date lastUpdate) {

        Validate.notEmpty(endpoint);
        Validate.notNull(address);
        Validate.notNull(port);
        Validate.notNull(registrationEndpointAddress);

        this.registrationId = registrationId;
        this.endpoint = endpoint;
        this.address = address;
        this.port = port;

        this.objectLinks = objectLinks;

        // extract the root objects path from the object links
        String rootPath = "/";
        if (objectLinks != null) {
            for (LinkObject link : objectLinks) {
                if (link != null && "oma.lwm2m".equals(link.getAttributes().get("rt"))) {
                    rootPath = link.getUrl();
                    break;
                }
            }
        }
        this.rootPath = rootPath;

        this.registrationDate = registrationDate == null ? new Date() : registrationDate;
        this.lifeTimeInSec = lifetimeInSec == null ? DEFAULT_LIFETIME_IN_SEC : lifetimeInSec;
        this.lwM2mVersion = lwM2mVersion == null ? DEFAULT_LWM2M_VERSION : lwM2mVersion;
        this.bindingMode = bindingMode == null ? BindingMode.U : bindingMode;
        this.smsNumber = smsNumber;
        this.registrationEndpointAddress = registrationEndpointAddress;
        this.lastUpdate = lastUpdate == null ? new Date() : lastUpdate;
    }

    public String getRegistrationId() {
        return registrationId;
    }

    public Date getRegistrationDate() {
        return registrationDate;
    }

    /**
     * Gets the client's network address.
     * 
     * @return the source address from the client's most recent CoAP message.
     */
    public InetAddress getAddress() {
        return address;
    }

    /**
     * Gets the client's network port number.
     * 
     * @return the source port from the client's most recent CoAP message.
     */
    public int getPort() {
        return port;
    }

    /**
     * Gets the network address and port number of LWM2M Server's CoAP endpoint the client originally registered at.
     * 
     * A LWM2M Server may listen on multiple CoAP end points, e.g. a non-secure and a secure one. Clients are often
     * behind a firewall which will only let incoming UDP packets pass if they originate from the same address:port that
     * the client has initiated communication with, e.g. by means of registering with the LWM2M Server. It is therefore
     * important to know, which of the server's CoAP end points the client contacted for registration.
     * 
     * This information can be used to uniquely identify the CoAP endpoint that should be used to access resources on
     * the client.
     * 
     * @return the network address and port number
     */
    public InetSocketAddress getRegistrationEndpointAddress() {
        return registrationEndpointAddress;
    }

    public LinkObject[] getObjectLinks() {
        return objectLinks;
    }

    public LinkObject[] getSortedObjectLinks() {
        // sort the list of objects
        if (objectLinks == null) {
            return null;
        }

        LinkObject[] res = Arrays.copyOf(objectLinks, objectLinks.length);

        Arrays.sort(res, new Comparator<LinkObject>() {

            /* sort by objectid, object instance and ressource */
            @Override
            public int compare(LinkObject o1, LinkObject o2) {
                if (o1 == null && o2 == null)
                    return 0;
                if (o1 == null)
                    return -1;
                if (o2 == null)
                    return 1;
                // by object
                Integer oi1 = o1.getObjectId();
                Integer oi2 = o2.getObjectId();

                if (oi1 == null && oi2 == null) {
                    return 0;
                }
                if (oi1 == null) {
                    return -1;
                }
                if (oi2 == null) {
                    return 1;
                }
                int oicomp = oi1.compareTo(oi2);
                if (oicomp != 0) {
                    return oicomp;
                }

                Integer oii1 = o1.getObjectInstanceId();
                Integer oii2 = o2.getObjectInstanceId();
                if (oii1 == null && oii2 == null) {
                    return 0;
                }
                if (oii1 == null) {
                    return -1;
                }
                if (oii2 == null) {
                    return 1;
                }
                int oiicomp = oii1.compareTo(oii2);
                if (oiicomp != 0) {
                    return oiicomp;
                }

                Integer or1 = o1.getResourceId();
                Integer or2 = o2.getResourceId();
                if (or1 == null && or2 == null) {
                    return 0;
                }
                if (or1 == null) {
                    return -1;
                }
                if (or2 == null) {
                    return 1;
                }
                return or1.compareTo(or2);

            }
        });

        return res;
    }

    public synchronized Long getLifeTimeInSec() {
        return lifeTimeInSec;
    }

    public String getSmsNumber() {
        return smsNumber;
    }

    public String getLwM2mVersion() {
        return lwM2mVersion;
    }

    public BindingMode getBindingMode() {
        return bindingMode;
    }

    /**
     * @return the path where the objects are hosted on the device
     */
    public String getRootPath() {
        return rootPath;
    }

    /**
     * Gets the unique name the client has registered with.
     * 
     * @return the name
     */
    public String getEndpoint() {
        return endpoint;
    }

    public Date getLastUpdate() {
        return lastUpdate;
    }

    public boolean isAlive() {
        return lastUpdate.getTime() + lifeTimeInSec * 1000 > System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return String
                .format("Client [registrationDate=%s, address=%s, port=%s, registrationEndpoint=%s, lifeTimeInSec=%s, smsNumber=%s, lwM2mVersion=%s, bindingMode=%s, endpoint=%s, registrationId=%s, objectLinks=%s, lastUpdate=%s]",
                        registrationDate, address, port, registrationEndpointAddress, lifeTimeInSec, smsNumber,
                        lwM2mVersion, bindingMode, endpoint, registrationId, Arrays.toString(objectLinks), lastUpdate);
    }

    /**
     * Computes a hash code for this client.
     * 
     * @return the hash code based on the <em>endpoint</em> property
     */
    @Override
    public int hashCode() {
        return getEndpoint().hashCode();
    }

    /**
     * Compares this Client to another object.
     * 
     * @return <code>true</code> if the other object is a Client instance and its <em>endpoint</em> property has the
     *         same value as this Client
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Client) {
            Client other = (Client) obj;
            return this.getEndpoint().equals(other.getEndpoint());
        } else {
            return false;
        }
    }
}
