/*******************************************************************************
 * Copyright (c) 2020 Sierra Wireless and others.
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
package org.eclipse.leshan.client.engine;

import java.util.Map;

import org.eclipse.leshan.client.RegistrationUpdate;
import org.eclipse.leshan.client.servers.LwM2mServer;

/**
 * Manage the registration life-cycle:
 * <ul>
 * <li>Start bootstrap session if no device Management server is available</li>
 * <li>Register to device management server when available (at startup or after bootstrap)</li>
 * <li>Update registration periodically.</li>
 * <li>If communication failed with device management server, try to bootstrap again each 10 minutes until succeed</li>
 * </ul>
 * <br>
 **/
public interface RegistrationEngine {

    /**
     * Trigger an "empty" registration update to all servers.
     */
    void triggerRegistrationUpdate();

    /**
     * Trigger an "empty" registration update to the given servers.
     */
    void triggerRegistrationUpdate(LwM2mServer server);

    /**
     * Trigger the given registration update to all servers.
     */
    void triggerRegistrationUpdate(RegistrationUpdate registrationUpdate);

    /**
     * Trigger the given registration update to the given servers.
     */
    void triggerRegistrationUpdate(LwM2mServer server, RegistrationUpdate registrationUpdate);

    /**
     * Trigger a client initiated bootstrap.
     *
     * @param deregister True if client should deregister itself before to stop.
     *
     * @return true if the bootstrap can be initiated.
     */
    boolean triggerClientInitiatedBootstrap(boolean deregister);

    /**
     * Returns the current registration Id for this server.
     *
     * @return the client registration Id or <code>null</code> if the client is not registered to this server.
     */
    String getRegistrationId(LwM2mServer server);

    /**
     * @return All the registered Server indexed by the corresponding registration id;
     */
    Map<String, LwM2mServer> getRegisteredServers();

    /**
     * @return The registered server identified by the given id or null if there is no corresponding server registered.
     */
    LwM2mServer getRegisteredServer(long serverId);

    /**
     * @return true if the given server can currently communicate with the device.
     */
    boolean isAllowedToCommunicate(LwM2mServer server);

    /**
     * @return the endpoint name of the LWM2M client
     */
    String getEndpoint();

    /**
     * Starts the engine.
     */
    void start();

    /**
     * Stop the client and can be restarted later using {@link #start()}.
     *
     * @param deregister True if client should deregister itself before to stop.
     */
    void stop(boolean deregister);

    /**
     * Destroy the client, frees all system resources. Client can not be restarted.
     *
     * @param deregister True if client should deregister itself before to be destroyed.
     */
    void destroy(boolean deregister);

}
