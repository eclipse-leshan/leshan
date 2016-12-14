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

import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.leshan.LinkObject;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.util.Validate;

/**
 * An immutable structure which represent a LW-M2M client registration on the server
 */
public class Registration implements Serializable {

    private static final long serialVersionUID = 1L;

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

    private final String id;

    private final LinkObject[] objectLinks;

    private final Map<String, String> additionalRegistrationAttributes;

    /** The location where LWM2M objects are hosted on the device */
    private final String rootPath;

    private final Date lastUpdate;

    protected Registration(String id, String endpoint, InetAddress address, int port, String lwM2mVersion,
            Long lifetimeInSec, String smsNumber, BindingMode bindingMode, LinkObject[] objectLinks,
            InetSocketAddress registrationEndpointAddress,

            Date registrationDate, Date lastUpdate, Map<String, String> additionalRegistrationAttributes) {

        Validate.notNull(id);
        Validate.notEmpty(endpoint);
        Validate.notNull(address);
        Validate.notNull(port);
        Validate.notNull(registrationEndpointAddress);

        this.id = id;
        this.endpoint = endpoint;
        this.address = address;
        this.port = port;
        this.smsNumber = smsNumber;
        this.registrationEndpointAddress = registrationEndpointAddress;

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

        this.lifeTimeInSec = lifetimeInSec == null ? DEFAULT_LIFETIME_IN_SEC : lifetimeInSec;
        this.lwM2mVersion = lwM2mVersion == null ? DEFAULT_LWM2M_VERSION : lwM2mVersion;
        this.bindingMode = bindingMode == null ? BindingMode.U : bindingMode;
        this.registrationDate = registrationDate == null ? new Date() : registrationDate;
        this.lastUpdate = lastUpdate == null ? new Date() : lastUpdate;
        this.additionalRegistrationAttributes = additionalRegistrationAttributes == null
                ? Collections.unmodifiableMap(new HashMap<String, String>())
                : Collections.unmodifiableMap(additionalRegistrationAttributes);

    }

    public String getId() {
        return id;
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

            /* sort by path */
            @Override
            public int compare(LinkObject o1, LinkObject o2) {
                if (o1 == null && o2 == null)
                    return 0;
                if (o1 == null)
                    return -1;
                if (o2 == null)
                    return 1;
                // by URL
                String[] url1 = o1.getUrl().split("/");
                String[] url2 = o2.getUrl().split("/");

                for (int i = 0; i < url1.length && i < url2.length; i++) {
                    // is it two numbers?
                    if (isNumber(url1[i]) && isNumber(url2[i])) {
                        int cmp = Integer.parseInt(url1[i]) - Integer.parseInt(url2[i]);
                        if (cmp != 0) {
                            return cmp;
                        }
                    } else {

                        int v = url1[i].compareTo(url2[i]);

                        if (v != 0) {
                            return v;
                        }
                    }
                }

                return url1.length - url2.length;
            }
        });

        return res;
    }

    private static boolean isNumber(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public Long getLifeTimeInSec() {
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

    public Map<String, String> getAdditionalRegistrationAttributes() {
        return additionalRegistrationAttributes;
    }

    public boolean usesQueueMode() {
        return bindingMode.equals(BindingMode.UQ);
    }

    @Override
    public String toString() {
        return String.format(
                "Registration [registrationDate=%s, address=%s, port=%s, registrationEndpoint=%s, lifeTimeInSec=%s, smsNumber=%s, lwM2mVersion=%s, bindingMode=%s, endpoint=%s, registrationId=%s, objectLinks=%s, lastUpdate=%s]",
                registrationDate, address, port, registrationEndpointAddress, lifeTimeInSec, smsNumber, lwM2mVersion,
                bindingMode, endpoint, id, Arrays.toString(objectLinks), lastUpdate);
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
        if (obj instanceof Registration) {
            Registration other = (Registration) obj;
            return this.getEndpoint().equals(other.getEndpoint());
        } else {
            return false;
        }
    }

    public static class Builder {
        private final String registrationId;
        private final String endpoint;
        private final InetAddress address;
        private final int port;
        private final InetSocketAddress registrationEndpointAddress;

        private Date registrationDate;
        private Date lastUpdate;
        private Long lifeTimeInSec;
        private String smsNumber;
        private BindingMode bindingMode;
        private String lwM2mVersion;
        private LinkObject[] objectLinks;
        private Map<String, String> additionalRegistrationAttributes;

        public Builder(String registrationId, String endpoint, InetAddress address, int port,
                InetSocketAddress registrationEndpointAddress) {

            Validate.notNull(registrationId);
            Validate.notEmpty(endpoint);
            Validate.notNull(address);
            Validate.notNull(port);
            Validate.notNull(registrationEndpointAddress);
            this.registrationId = registrationId;
            this.endpoint = endpoint;
            this.address = address;
            this.port = port;
            this.registrationEndpointAddress = registrationEndpointAddress;

        }

        public Builder registrationDate(Date registrationDate) {
            this.registrationDate = registrationDate;
            return this;
        }

        public Builder lastUpdate(Date lastUpdate) {
            this.lastUpdate = lastUpdate;
            return this;
        }

        public Builder lifeTimeInSec(Long lifetimeInSec) {
            this.lifeTimeInSec = lifetimeInSec;
            return this;
        }

        public Builder smsNumber(String smsNumber) {
            this.smsNumber = smsNumber;
            return this;
        }

        public Builder bindingMode(BindingMode bindingMode) {
            this.bindingMode = bindingMode;
            return this;
        }

        public Builder lwM2mVersion(String lwM2mVersion) {
            this.lwM2mVersion = lwM2mVersion;
            return this;
        }

        public Builder objectLinks(LinkObject[] objectLinks) {
            this.objectLinks = objectLinks;
            return this;
        }

        public Builder additionalRegistrationAttributes(Map<String, String> additionalRegistrationAttributes) {
            this.additionalRegistrationAttributes = additionalRegistrationAttributes;
            return this;
        }

        public Registration build() {
            return new Registration(Builder.this.registrationId, Builder.this.endpoint, Builder.this.address,
                    Builder.this.port, Builder.this.lwM2mVersion, Builder.this.lifeTimeInSec, Builder.this.smsNumber,
                    this.bindingMode, this.objectLinks, this.registrationEndpointAddress, this.registrationDate,
                    this.lastUpdate, this.additionalRegistrationAttributes);
        }

    }

}
