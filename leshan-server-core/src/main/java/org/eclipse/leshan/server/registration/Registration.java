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
 *     Achim Kraus (Bosch Software Innovations GmbH) - use Identity as destination
 *******************************************************************************/
package org.eclipse.leshan.server.registration;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.leshan.core.Link;
import org.eclipse.leshan.core.attributes.Attribute;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.util.StringUtils;
import org.eclipse.leshan.core.util.Validate;

/**
 * An immutable structure which represent a LW-M2M client registration on the server
 */
public class Registration implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final long DEFAULT_LIFETIME_IN_SEC = 86400L;

    private static final String DEFAULT_LWM2M_VERSION = "1.0";

    private final Date registrationDate;

    private final Identity identity;

    private final long lifeTimeInSec;

    private final String smsNumber;

    private final String lwM2mVersion;

    private final BindingMode bindingMode;

    /**
     * The LWM2M Client's unique end point name.
     */
    private final String endpoint;

    private final String id;

    private final Link[] objectLinks;

    // Lazy Loaded map of supported object (object id => version)
    // built from objectLinks
    private final AtomicReference<Map<Integer, String>> supportedObjects;

    private final Map<String, String> additionalRegistrationAttributes;

    /** The location where LWM2M objects are hosted on the device */
    private final String rootPath;

    private final Date lastUpdate;

    protected Registration(String id, String endpoint, Identity identity, String lwM2mVersion, Long lifetimeInSec,
            String smsNumber, BindingMode bindingMode, Link[] objectLinks, Date registrationDate, Date lastUpdate,
            Map<String, String> additionalRegistrationAttributes, Map<Integer, String> supportedObjects) {

        Validate.notNull(id);
        Validate.notEmpty(endpoint);
        Validate.notNull(identity);

        this.id = id;
        this.identity = identity;
        this.endpoint = endpoint;
        this.smsNumber = smsNumber;

        this.objectLinks = objectLinks;
        // Parse object link to extract root path.
        String rootPath = "/";
        if (objectLinks != null) {
            for (Link link : objectLinks) {
                if (link != null && "oma.lwm2m".equals(Link.unquote(link.getAttributes().get("rt")))) {
                    rootPath = link.getUrl();
                    break;
                }
            }
        }
        if (!rootPath.endsWith("/"))
            rootPath = rootPath + "/";
        this.rootPath = rootPath;
        this.supportedObjects = new AtomicReference<Map<Integer, String>>(supportedObjects);
        this.lifeTimeInSec = lifetimeInSec == null ? DEFAULT_LIFETIME_IN_SEC : lifetimeInSec;
        this.lwM2mVersion = lwM2mVersion == null ? DEFAULT_LWM2M_VERSION : lwM2mVersion;
        this.bindingMode = bindingMode == null ? BindingMode.U : bindingMode;
        this.registrationDate = registrationDate == null ? new Date() : registrationDate;
        this.lastUpdate = lastUpdate == null ? new Date() : lastUpdate;
        if (additionalRegistrationAttributes == null || additionalRegistrationAttributes.isEmpty()) {
            this.additionalRegistrationAttributes = Collections.emptyMap();
        } else {
            // We create a new HashMap to have a real immutable map and to avoid "unmodifiableMap" encapsulation.
            this.additionalRegistrationAttributes = Collections
                    .unmodifiableMap(new HashMap<>(additionalRegistrationAttributes));
        }

    }

    public String getId() {
        return id;
    }

    public Date getRegistrationDate() {
        return registrationDate;
    }

    /**
     * Gets the clients identity.
     * 
     * @return identity from client's most recent registration or registration update.
     */
    public Identity getIdentity() {
        return identity;
    }

    /**
     * Gets the client's network socket address.
     * 
     * @return the source address from the client's most recent CoAP message.
     */
    public InetSocketAddress getSocketAddress() {
        return identity.getPeerAddress();
    }

    /**
     * Gets the client's network address.
     * 
     * @return the source address from the client's most recent CoAP message.
     */
    public InetAddress getAddress() {
        return identity.getPeerAddress().getAddress();
    }

    /**
     * Gets the client's network port number.
     * 
     * @return the source port from the client's most recent CoAP message.
     */
    public int getPort() {
        return identity.getPeerAddress().getPort();
    }

    public Link[] getObjectLinks() {
        return objectLinks;
    }

    public Link[] getSortedObjectLinks() {
        // sort the list of objects
        if (objectLinks == null) {
            return null;
        }

        Link[] res = Arrays.copyOf(objectLinks, objectLinks.length);

        Arrays.sort(res, new Comparator<Link>() {

            /* sort by path */
            @Override
            public int compare(Link o1, Link o2) {
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
        return !StringUtils.isEmpty(s) && StringUtils.isNumeric(s);
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

    public long getExpirationTimeStamp() {
        return getExpirationTimeStamp(0L);
    }

    public long getExpirationTimeStamp(long gracePeriodInSec) {
        return lastUpdate.getTime() + lifeTimeInSec * 1000 + gracePeriodInSec * 1000;
    }

    /**
     * @return True if DTLS handshake can be initiated by the Server for this registration.
     */
    public boolean canInitiateConnection() {
        // We consider that initiates a connection (acting as DTLS client to initiate a handshake) does not make sense
        // for QueueMode as if we lost connection device is probably absent.
        return !bindingMode.useQueueMode() && bindingMode.useUDP();
    }

    /**
     * @return true if the last registration update was done less than lifetime seconds ago.
     */
    public boolean isAlive() {
        return isAlive(0);
    }

    /**
     * This is the same idea than {@link Registration#isAlive()} but with a grace period. <br>
     * 
     * @param gracePeriodInSec an extra time for the registration lifetime.
     * @return true if the last registration update was done less than lifetime+gracePeriod seconds ago.
     */
    public boolean isAlive(long gracePeriodInSec) {
        return getExpirationTimeStamp(gracePeriodInSec) > System.currentTimeMillis();
    }

    public Map<String, String> getAdditionalRegistrationAttributes() {
        return additionalRegistrationAttributes;
    }

    public boolean usesQueueMode() {
        return bindingMode.useQueueMode() && bindingMode.useUDP();
    }

    /**
     * @param objectid the object id for which we want to know the supported version.
     * @return the supported version of the object with the id {@code objectid}. If the object is not supported return
     *         {@code null}
     */
    public String getSupportedVersion(Integer objectid) {
        return getSupportedObject().get(objectid);
    }

    /**
     * @return a map from {@code objectId} {@literal =>} {@code supportedVersion} for each supported objects. supported.
     */
    public Map<Integer, String> getSupportedObject() {
        Map<Integer, String> objects = supportedObjects.get();
        if (objects != null)
            return objects;

        supportedObjects.compareAndSet(null, Collections.unmodifiableMap(getSupportedObject(rootPath, objectLinks)));
        return supportedObjects.get();
    }

    @Override
    public String toString() {
        return String.format(
                "Registration [registrationDate=%s, identity=%s, lifeTimeInSec=%s, smsNumber=%s, lwM2mVersion=%s, bindingMode=%s, endpoint=%s, registrationId=%s, objectLinks=%s, lastUpdate=%s]",
                registrationDate, identity, lifeTimeInSec, smsNumber, lwM2mVersion, bindingMode, endpoint, id,
                Arrays.toString(objectLinks), lastUpdate);
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

    /**
     * Build a Map {@code objectId} {@literal =>} {@code supportedVersion} from root path and registration object links.
     * 
     * @param rootPath the rootpath of LWM2M tree.
     * @param objectLinks the registraiton object links payload.
     * @return a Map {@code objectId} {@literal =>} {@code supportedVersion}.
     */
    public static Map<Integer, String> getSupportedObject(String rootPath, Link[] objectLinks) {
        Map<Integer, String> objects = new HashMap<>();
        for (Link link : objectLinks) {
            if (link != null) {
                Pattern p = Pattern.compile("^\\Q" + rootPath + "\\E(\\d+)(?:/\\d+)*$");
                Matcher m = p.matcher(link.getUrl());
                if (m.matches()) {
                    try {
                        // extract object id and version
                        int objectId = Integer.parseInt(m.group(1));
                        String version = link.getAttributes().get(Attribute.OBJECT_VERSION);
                        // un-quote version (see https://github.com/eclipse/leshan/issues/732)
                        version = Link.unquote(version);
                        String currentVersion = objects.get(objectId);

                        // store it in map
                        if (currentVersion == null) {
                            // we never find version for this object add it
                            if (version != null) {
                                objects.put(objectId, version);
                            } else {
                                objects.put(objectId, ObjectModel.DEFAULT_VERSION);
                            }
                        } else {
                            // if version is already set, we override it only if new version is not DEFAULT_VERSION
                            if (version != null && !version.equals(ObjectModel.DEFAULT_VERSION)) {
                                objects.put(objectId, version);
                            }
                        }
                    } catch (NumberFormatException e) {
                        // This should not happened except maybe if the number in url is too long...
                        // In this case we just ignore it because this is not an object id.
                    }
                }
            }
        }
        return objects;
    }

    public static class Builder {
        private final String registrationId;
        private final String endpoint;
        private final Identity identity;

        private Date registrationDate;
        private Date lastUpdate;
        private Long lifeTimeInSec;
        private String smsNumber;
        private BindingMode bindingMode;
        private String lwM2mVersion;
        private Link[] objectLinks;
        private Map<Integer, String> supportedObjects;
        private Map<String, String> additionalRegistrationAttributes;

        public Builder(String registrationId, String endpoint, Identity identity) {

            Validate.notNull(registrationId);
            Validate.notEmpty(endpoint);
            Validate.notNull(identity);
            this.registrationId = registrationId;
            this.endpoint = endpoint;
            this.identity = identity;
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

        public Builder objectLinks(Link[] objectLinks) {
            this.objectLinks = objectLinks;
            return this;
        }

        public Builder supportedObjects(Map<Integer, String> supportedObjects) {
            this.supportedObjects = Collections.unmodifiableMap(supportedObjects);
            return this;
        }

        public Builder additionalRegistrationAttributes(Map<String, String> additionalRegistrationAttributes) {
            this.additionalRegistrationAttributes = additionalRegistrationAttributes;
            return this;
        }

        public Registration build() {
            return new Registration(Builder.this.registrationId, Builder.this.endpoint, Builder.this.identity,
                    Builder.this.lwM2mVersion, Builder.this.lifeTimeInSec, Builder.this.smsNumber, this.bindingMode,
                    this.objectLinks, this.registrationDate, this.lastUpdate, this.additionalRegistrationAttributes,
                    this.supportedObjects);
        }

    }
}
