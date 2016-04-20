/*******************************************************************************
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
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
 *     Balasubramanian Azhagappan (Bosch Software Innovations GmbH)
 *                                  - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.server.queue.impl;

import org.eclipse.leshan.server.client.ClientRegistry;
import org.eclipse.leshan.server.queue.ClientState;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Tracks the status of each client endpoint.
 *
 * @see ClientState
 */
public final class ClientStatusTracker {
    private final ConcurrentMap<String, ClientState> clientStatus = new ConcurrentHashMap<>();
    private final ClientRegistry clientRegistry;

    public ClientStatusTracker(ClientRegistry clientRegistry) {
        this.clientRegistry = clientRegistry;
    }

    /**
     * sets the status of the client to {@link ClientState#UNREACHABLE} atomically
     *
     * @param endpoint target client endpoint name who's state has changed
     * @return true if the value is set successfully, false if the value was already changed,
     *                  indicating the state was already updated, may be by a different thread.
     */
    public boolean setClientUnreachable(String endpoint) {
        if(clientStatus.get(endpoint) != null) {
            return clientStatus.replace(endpoint, ClientState.REACHABLE, ClientState.UNREACHABLE);
        }
        else {
            clientStatus.putIfAbsent(endpoint, ClientState.UNREACHABLE);
            return true;
        }
    }

    /**
     * sets the status of the client to {@link ClientState#REACHABLE} atomically
     *
     * @param endpoint target client endpoint name who's state has changed
     * @return true if the value is set successfully, false if the value was already changed,
     *                  indicating the state was already updated, may be by a different thread.
     */
    public boolean setClientReachable(String endpoint) {
        if(clientStatus.get(endpoint) != null) {
            return clientStatus.replace(endpoint, ClientState.UNREACHABLE, ClientState.REACHABLE);
        }
        else {
            clientStatus.putIfAbsent(endpoint, ClientState.REACHABLE);
            return true;
        }
    }

    /**
     * returns true if client state is ClientState.REACHABLE
     *
     * @param endpoint clients endpoint.
     * @return true if client is not in {@link ClientState#UNREACHABLE}
     * or if no status is available for the client but known to ClientRegistry, then it is assumed to be reachable <br>
     * false client has timed out and the status is marked as {@link ClientState#UNREACHABLE}
     */
    public boolean isClientReachable(String endpoint) {
        return (clientStatus.get(endpoint)!= null)? clientStatus.get(endpoint).equals(ClientState.REACHABLE)
                                            : true;
    }

    public void clearClientState(String endpoint) {
        clientStatus.remove(endpoint);
    }
}
