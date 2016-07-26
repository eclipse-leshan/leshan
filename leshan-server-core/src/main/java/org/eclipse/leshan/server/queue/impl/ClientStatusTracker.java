/*******************************************************************************
 * Copyright (c) 2016 Bosch Software Innovations GmbH and others.
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
 *     Balasubramanian Azhagappan, Daniel Maier (Bosch Software Innovations GmbH)
 *                                  - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.server.queue.impl;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.leshan.server.queue.ClientState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks the status of each client endpoint. Also ensures that the state transitions are carried out
 *
 * A client starts with a REACHABLE state on a Register event. Before the first message is sent, the status is set to
 * RECEIVING. For further queued messages the client remains in this state until there are no more messages to be
 * delivered. When there are no more messages left, the state is set to REACHABLE. On timeout of a message, the status
 * is set to UNREACHABLE.
 *
 * On Client updates or notifies, the UNREACHABLE state is changed to REACHABLE and then set to RECEIVING before sending
 * any messages. This is done to ensure that messages are sent only once in a multi-threaded message queue processing.
 *
 * @see ClientState
 */
public final class ClientStatusTracker {
    private static final Logger LOG = LoggerFactory.getLogger(ClientStatusTracker.class);
    private final ConcurrentMap<String, ClientState> clientStatus = new ConcurrentHashMap<>();

    public boolean setClientUnreachable(String endpoint) {
        return transitState(endpoint, ClientState.RECEIVING, ClientState.UNREACHABLE);
    }

    public boolean setClientReachable(String endpoint) {
        return transitState(endpoint, ClientState.UNREACHABLE, ClientState.REACHABLE)
                || clientStatus.putIfAbsent(endpoint, ClientState.REACHABLE) == null;
    }

    public boolean startClientReceiving(String endpoint) {
        return transitState(endpoint, ClientState.REACHABLE, ClientState.RECEIVING);
    }

    public boolean stopClientReceiving(String endpoint) {
        return transitState(endpoint, ClientState.RECEIVING, ClientState.REACHABLE);
    }

    public void clearClientState(String endpoint) {
        clientStatus.remove(endpoint);
    }

    private boolean transitState(String endpoint, ClientState from, ClientState to) {
        boolean updated = clientStatus.replace(endpoint, from, to);
        if (LOG.isDebugEnabled()) {
            if (updated) {
                LOG.debug("Client {} state update {} -> {}", endpoint, from, to);
            } else {
                LOG.debug("Cannot update Client {} state {} -> {}. Current state is {}", endpoint, from, to,
                        clientStatus.get(endpoint));
            }
        }
        return updated;
    }
}
