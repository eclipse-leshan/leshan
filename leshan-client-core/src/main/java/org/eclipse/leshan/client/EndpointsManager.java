/*******************************************************************************
 * Copyright (c) 2017 Sierra Wireless and others.
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
package org.eclipse.leshan.client;

import java.util.Collection;

import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.client.servers.ServerInfo;

/**
 * A class responsible to handle transport layer used for LWM2M communication.
 *
 */
public interface EndpointsManager {

    /**
     * Create the endpoint/connector/socket based on given {@link ServerInfo}.
     * 
     * @return a {@link ServerIdentity} object which could be used to {@link #forceReconnection(ServerIdentity, boolean)} or
     *         {@link #getMaxCommunicationPeriodFor(ServerIdentity, long)}
     */
    ServerIdentity createEndpoint(ServerInfo serverInfo);

    /**
     * Create an endpoint/connector/socket for each given {@link ServerInfo}.
     * 
     * @return a collection of {@link ServerIdentity} object which could be used to {@link #forceReconnection(ServerIdentity, boolean)}
     *         or {@link #getMaxCommunicationPeriodFor(ServerIdentity, long)}
     */
    Collection<ServerIdentity> createEndpoints(Collection<? extends ServerInfo> serverInfo);

    /**
     * Return the longest possible communication period based on given lifetime for the given server.
     * <p>
     * EndpointsManager should be aware about the kind of transport layer used to contact this server. So it should be
     * able to calculate the communication period to avoid that registration expires taking into account specific
     * transport layer latency (like DTLS Handshake, CoAP retransmission ...)
     * 
     * @return the longest possible communication period based on given lifetime for the given server.
     */
    long getMaxCommunicationPeriodFor(ServerIdentity server, long lifetimeInSeconds);

    /**
     * Force a "reconnection" to the server. "reconnection" meaning changes depending of transport layer used to
     * communicate with this server.
     * <p>
     * E.g. for DTLS, reconnection means start a new handshake.
     * 
     * @param resume True if we must try to resume the connection (e.g. Abbreviated Handshake for DTLS)
     */
    void forceReconnection(ServerIdentity server, boolean resume);

    /**
     * Starts the manager.
     */
    void start();

    /**
     * Stop the manager and can be restarted later using {@link #start()}.
     */
    void stop();

    /**
     * Destroy the manager, frees all system resources. Client can not be restarted.
     */
    void destroy();
}
