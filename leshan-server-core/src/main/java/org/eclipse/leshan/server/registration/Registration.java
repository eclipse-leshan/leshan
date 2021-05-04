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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.leshan.core.Link;
import org.eclipse.leshan.core.LwM2m.Version;
import org.eclipse.leshan.core.attributes.Attribute;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.util.StringUtils;
import org.eclipse.leshan.core.util.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An immutable structure which represent a LW-M2M client registration on the server
 */
public class Registration {

    private static final Logger LOG = LoggerFactory.getLogger(Registration.class);

    private static final long DEFAULT_LIFETIME_IN_SEC = 86400L;

    private final Date registrationDate;

    private final Identity identity;

    private final long lifeTimeInSec;

    private final String smsNumber;

    private final Version lwM2mVersion;

    private final EnumSet<BindingMode> bindingMode;

    private final Boolean queueMode; // since LWM2M 1.1

    // The LWM2M Client's unique end point name.
    private final String endpoint;

    private final String id;

    private final Link[] objectLinks;

    // Lazy Loaded map of supported object (object id => version)
    // built from objectLinks
    private final AtomicReference<Map<Integer, String>> supportedObjects;

    private final Map<String, String> additionalRegistrationAttributes;

    // The location where LWM2M objects are hosted on the device
    private final String rootPath;

    // All ContentFormat supported by the client
    private final Set<ContentFormat> supportedContentFormats;

    private final Date lastUpdate;

    protected Registration(Builder builder) {

        Validate.notNull(builder.registrationId);
        Validate.notEmpty(builder.endpoint);
        Validate.notNull(builder.identity);

        // mandatory params
        id = builder.registrationId;
        identity = builder.identity;
        endpoint = builder.endpoint;

        // object links related params
        objectLinks = builder.objectLinks;
        rootPath = builder.rootPath;
        supportedContentFormats = builder.supportedContentFormats;
        supportedObjects = new AtomicReference<Map<Integer, String>>(builder.supportedObjects);

        // other params
        lifeTimeInSec = builder.lifeTimeInSec;
        lwM2mVersion = builder.lwM2mVersion;
        bindingMode = builder.bindingMode;
        queueMode = builder.queueMode;
        registrationDate = builder.registrationDate;
        lastUpdate = builder.lastUpdate;
        smsNumber = builder.smsNumber;
        additionalRegistrationAttributes = builder.additionalRegistrationAttributes;

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

    public Version getLwM2mVersion() {
        return lwM2mVersion;
    }

    public EnumSet<BindingMode> getBindingMode() {
        return bindingMode;
    }

    public Boolean getQueueMode() {
        return queueMode;
    }

    /**
     * @return the path where the objects are hosted on the device
     */
    public String getRootPath() {
        return rootPath;
    }

    /**
     * @return all {@link ContentFormat} supported by the client.
     */
    public Set<ContentFormat> getSupportedContentFormats() {
        return supportedContentFormats;
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
        return !usesQueueMode();
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
        if (lwM2mVersion.olderThan(Version.V1_1))
            return bindingMode.contains(BindingMode.Q);
        else
            return queueMode;
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
                "Registration [registrationDate=%s, identity=%s, lifeTimeInSec=%s, smsNumber=%s, lwM2mVersion=%s, bindingMode=%s, queueMode=%s, endpoint=%s, id=%s, objectLinks=%s, supportedObjects=%s, additionalRegistrationAttributes=%s, rootPath=%s, supportedContentFormats=%s, lastUpdate=%s]",
                registrationDate, identity, lifeTimeInSec, smsNumber, lwM2mVersion, bindingMode, queueMode, endpoint,
                id, Arrays.toString(objectLinks), supportedObjects, additionalRegistrationAttributes, rootPath,
                supportedContentFormats, lastUpdate);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((additionalRegistrationAttributes == null) ? 0 : additionalRegistrationAttributes.hashCode());
        result = prime * result + ((bindingMode == null) ? 0 : bindingMode.hashCode());
        result = prime * result + ((endpoint == null) ? 0 : endpoint.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((identity == null) ? 0 : identity.hashCode());
        result = prime * result + ((lastUpdate == null) ? 0 : lastUpdate.hashCode());
        result = prime * result + (int) (lifeTimeInSec ^ (lifeTimeInSec >>> 32));
        result = prime * result + ((lwM2mVersion == null) ? 0 : lwM2mVersion.hashCode());
        result = prime * result + Arrays.hashCode(objectLinks);
        result = prime * result + ((queueMode == null) ? 0 : queueMode.hashCode());
        result = prime * result + ((registrationDate == null) ? 0 : registrationDate.hashCode());
        result = prime * result + ((rootPath == null) ? 0 : rootPath.hashCode());
        result = prime * result + ((smsNumber == null) ? 0 : smsNumber.hashCode());
        result = prime * result + ((supportedContentFormats == null) ? 0 : supportedContentFormats.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Registration other = (Registration) obj;
        if (additionalRegistrationAttributes == null) {
            if (other.additionalRegistrationAttributes != null)
                return false;
        } else if (!additionalRegistrationAttributes.equals(other.additionalRegistrationAttributes))
            return false;
        if (bindingMode == null) {
            if (other.bindingMode != null)
                return false;
        } else if (!bindingMode.equals(other.bindingMode))
            return false;
        if (endpoint == null) {
            if (other.endpoint != null)
                return false;
        } else if (!endpoint.equals(other.endpoint))
            return false;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (identity == null) {
            if (other.identity != null)
                return false;
        } else if (!identity.equals(other.identity))
            return false;
        if (lastUpdate == null) {
            if (other.lastUpdate != null)
                return false;
        } else if (!lastUpdate.equals(other.lastUpdate))
            return false;
        if (lifeTimeInSec != other.lifeTimeInSec)
            return false;
        if (lwM2mVersion == null) {
            if (other.lwM2mVersion != null)
                return false;
        } else if (!lwM2mVersion.equals(other.lwM2mVersion))
            return false;
        if (!Arrays.equals(objectLinks, other.objectLinks))
            return false;
        if (queueMode == null) {
            if (other.queueMode != null)
                return false;
        } else if (!queueMode.equals(other.queueMode))
            return false;
        if (registrationDate == null) {
            if (other.registrationDate != null)
                return false;
        } else if (!registrationDate.equals(other.registrationDate))
            return false;
        if (rootPath == null) {
            if (other.rootPath != null)
                return false;
        } else if (!rootPath.equals(other.rootPath))
            return false;
        if (smsNumber == null) {
            if (other.smsNumber != null)
                return false;
        } else if (!smsNumber.equals(other.smsNumber))
            return false;
        if (supportedContentFormats == null) {
            if (other.supportedContentFormats != null)
                return false;
        } else if (!supportedContentFormats.equals(other.supportedContentFormats))
            return false;
        return true;
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
        private EnumSet<BindingMode> bindingMode;
        private Boolean queueMode;
        private Version lwM2mVersion = Version.getDefault();
        private Link[] objectLinks;
        private String rootPath;
        private Set<ContentFormat> supportedContentFormats;
        private Map<Integer, String> supportedObjects;
        private Map<String, String> additionalRegistrationAttributes;

        // builder setting
        private boolean extractData; // if true extract data from objectLinks

        public Builder(String registrationId, String endpoint, Identity identity) {

            Validate.notNull(registrationId);
            Validate.notEmpty(endpoint);
            Validate.notNull(identity);
            this.registrationId = registrationId;
            this.endpoint = endpoint;
            this.identity = identity;
        }

        public Builder extractDataFromObjectLink(boolean extract) {
            this.extractData = extract;
            return this;
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

        public Builder bindingMode(EnumSet<BindingMode> bindingMode) {
            this.bindingMode = bindingMode;
            return this;
        }

        public Builder queueMode(Boolean queueMode) {
            this.queueMode = queueMode;
            return this;
        }

        public Builder lwM2mVersion(Version lwM2mVersion) {
            this.lwM2mVersion = lwM2mVersion;
            return this;
        }

        public Builder objectLinks(Link[] objectLinks) {
            this.objectLinks = objectLinks;
            return this;
        }

        public Builder rootPath(String rootPath) {
            this.rootPath = rootPath;
            return this;
        }

        public Builder supportedContentFormats(Set<ContentFormat> supportedContentFormats) {
            this.supportedContentFormats = supportedContentFormats;
            return this;
        }

        public Builder supportedContentFormats(ContentFormat... supportedContentFormats) {
            this.supportedContentFormats = new HashSet<>();
            for (ContentFormat contentFormat : supportedContentFormats) {
                this.supportedContentFormats.add(contentFormat);
            }
            return this;
        }

        public Builder additionalRegistrationAttributes(Map<String, String> additionalRegistrationAttributes) {
            this.additionalRegistrationAttributes = additionalRegistrationAttributes;
            return this;
        }

        private void extractDataFromObjectLinks() {
            if (objectLinks != null) {
                // Define default RootPath;
                rootPath = "/";

                // Parse object link to extract root path
                for (Link link : objectLinks) {
                    if (link != null && "oma.lwm2m".equals(Link.unquote(link.getAttributes().get("rt")))) {
                        rootPath = link.getUrl();
                        if (!rootPath.endsWith("/")) {
                            rootPath = rootPath + "/";
                        }
                        break;
                    }
                }

                // Extract data from link object
                for (Link link : objectLinks) {
                    // TODO extract object supported
                    // TODO extract available instances

                    if (link != null) {
                        // search supported Content format in root link
                        if (rootPath.equals(link.getUrl())) {
                            String ctValue = link.getAttributes().get("ct");
                            if (ctValue != null) {
                                supportedContentFormats = extractContentFormat(ctValue);
                            }
                        }
                    }
                }
            }
        }

        private Set<ContentFormat> extractContentFormat(String ctValue) {
            Set<ContentFormat> supportedContentFormats = new HashSet<>();

            // add content format from ct attributes
            String[] formats = ctValue.split(" ");
            for (String codeAsString : formats) {
                try {
                    ContentFormat contentformat = ContentFormat.fromCode(codeAsString);
                    if (supportedContentFormats.contains(contentformat)) {
                        LOG.warn("Duplicate Content format {} in ct={} attributes for registration {} of client {} ",
                                codeAsString, supportedContentFormats, registrationId, endpoint);
                    }
                    supportedContentFormats.add(contentformat);
                } catch (NumberFormatException e) {
                    LOG.warn(
                            "Invalid supported Content format {} in ct={} attributes for registration {} of client {} ",
                            codeAsString, supportedContentFormats, registrationId, endpoint);
                }
            }

            // add mandatory content format
            for (ContentFormat format : ContentFormat.knownContentFormat) {
                if (format.isMandatoryForClient(lwM2mVersion)) {
                    supportedContentFormats.add(format);
                }
            }
            return supportedContentFormats;
        }

        public Registration build() {
            // Define Default value
            rootPath = rootPath == null ? "/" : rootPath;
            lifeTimeInSec = lifeTimeInSec == null ? DEFAULT_LIFETIME_IN_SEC : lifeTimeInSec;
            lwM2mVersion = lwM2mVersion == null ? Version.getDefault() : lwM2mVersion;
            bindingMode = bindingMode == null ? EnumSet.of(BindingMode.U) : bindingMode;
            queueMode = queueMode == null && lwM2mVersion.newerThan(Version.V1_0) ? Boolean.FALSE : queueMode;
            registrationDate = registrationDate == null ? new Date() : registrationDate;
            lastUpdate = lastUpdate == null ? new Date() : lastUpdate;

            // Extract data from object links if wanted
            if (extractData) {
                extractDataFromObjectLinks();
            }

            // Make collection immutable
            // We create a new Collection and make it "unmodifiable".
            if (supportedContentFormats == null || supportedContentFormats.isEmpty()) {
                supportedContentFormats = Collections.emptySet();
            } else {
                supportedContentFormats = Collections.unmodifiableSet(new HashSet<>(supportedContentFormats));
            }
            if (additionalRegistrationAttributes == null || additionalRegistrationAttributes.isEmpty()) {
                additionalRegistrationAttributes = Collections.emptyMap();
            } else {
                additionalRegistrationAttributes = Collections
                        .unmodifiableMap(new HashMap<>(additionalRegistrationAttributes));
            }

            // Create Registration
            return new Registration(this);
        }
    }
}
