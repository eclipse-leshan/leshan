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
package org.eclipse.leshan.client;

import org.eclipse.leshan.client.resource.LwM2mObjectTree;
import org.eclipse.leshan.client.servers.ServerIdentity;

/**
 * A Lightweight M2M client.
 */
public interface LwM2mClient {

    /**
     * Starts the client.
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

    /**
     * Trigger a registration update to all registered Server.
     */
    void triggerRegistrationUpdate();

    /**
     * Trigger a registration update to the given server.
     */
    void triggerRegistrationUpdate(ServerIdentity server);

    /**
     * @return the {@link LwM2mObjectTree} containing all the object implemented by this client.
     */
    LwM2mObjectTree getObjectTree();
}
