/*******************************************************************************
 * Copyright (c) 2025 Sierra Wireless and others.
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
 *******************************************************************************/
package org.eclipse.leshan.server.registration;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Date;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.leshan.core.LwM2m.LwM2mVersion;
import org.eclipse.leshan.core.LwM2m.Version;
import org.eclipse.leshan.core.endpoint.EndpointUri;
import org.eclipse.leshan.core.link.Link;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.peer.LwM2mPeer;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.server.security.Authorizer;

public interface IRegistration {

    String getId();

    Date getRegistrationDate();

    /**
     * Gets the clients transport layer data.
     *
     * @return transport layer data from client's most recent registration or registration update.
     */
    LwM2mPeer getClientTransportData();

    /**
     * Gets the client's network socket address.
     *
     * @return the source address from the client's most recent CoAP message. It could return {@code null} if client
     *         does not communicate over IP.
     */
    InetSocketAddress getSocketAddress();

    /**
     * Gets the client's network address.
     *
     * @return the source address from the client's most recent CoAP message. It could return {@code null} if client
     *         does not communicate over IP.
     */
    InetAddress getAddress();

    /**
     * Gets the client's network port number.
     *
     * @return the source port from the client's most recent CoAP message. It could return {@code null} if client does
     *         not communicate over IP.
     */
    Integer getPort();

    Link[] getObjectLinks();

    Link[] getSortedObjectLinks();

    Long getLifeTimeInSec();

    String getSmsNumber();

    LwM2mVersion getLwM2mVersion();

    EnumSet<BindingMode> getBindingMode();

    Boolean getQueueMode();

    /**
     * @return the path where the objects are hosted on the device
     */
    String getRootPath();

    String getFullPrefixPath();

    /**
     * @return all {@link ContentFormat} supported by the client.
     */
    Set<ContentFormat> getSupportedContentFormats();

    /**
     * @return all available object instance by the client
     */
    Set<LwM2mPath> getAvailableInstances();

    /**
     * Gets the unique name the client has registered with.
     *
     * @return the name
     */
    String getEndpoint();

    Date getLastUpdate();

    long getExpirationTimeStamp();

    long getExpirationTimeStamp(long gracePeriodInSec);

    /**
     * @return True if DTLS handshake can be initiated by the Server for this registration.
     */
    boolean canInitiateConnection();

    /**
     * @return true if the last registration update was done less than lifetime seconds ago.
     */
    boolean isAlive();

    /**
     * This is the same idea than {@link Registration#isAlive()} but with a grace period. <br>
     *
     * @param gracePeriodInSec an extra time for the registration lifetime.
     * @return true if the last registration update was done less than lifetime+gracePeriod seconds ago.
     */
    boolean isAlive(long gracePeriodInSec);

    Map<String, String> getAdditionalRegistrationAttributes();

    boolean usesQueueMode();

    /**
     * @param objectid the object id for which we want to know the supported version.
     * @return the supported version of the object with the id {@code objectid}. If the object is not supported return
     *         {@code null}
     */
    Version getSupportedVersion(Integer objectid);

    /**
     * @return a map from {@code objectId} {@literal =>} {@code supportedVersion} for each supported objects. supported.
     */
    Map<Integer, Version> getSupportedObject();

    /**
     * @return Some custom registration data which could have been added at Registration by the {@link Authorizer}
     */
    Map<String, String> getCustomRegistrationData();

    /**
     * @return URI of the server endpoint used by client to register.
     */
    EndpointUri getEndpointUri();

    boolean isGateway();

    Map<String /* prefix */, String /* endpoint */> getChildEndDevices();
}
