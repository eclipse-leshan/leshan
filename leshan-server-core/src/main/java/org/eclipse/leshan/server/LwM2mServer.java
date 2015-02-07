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
package org.eclipse.leshan.server;

import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.response.ExceptionConsumer;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ResponseConsumer;
import org.eclipse.leshan.server.client.Client;
import org.eclipse.leshan.server.client.ClientRegistry;
import org.eclipse.leshan.server.observation.ObservationRegistry;
import org.eclipse.leshan.server.security.SecurityRegistry;

/**
 * An OMA Lightweight M2M device management server.
 *
 * Will receive client registration through the "/rd" resource. Is able to send requests (Read, Write, Create, Delete,
 * Execute, Discover, Observer) to specified clients.
 *
 * It's your main entry point for using the Leshan-core API.
 */
public interface LwM2mServer {

    /**
     * Start the server (bind port, start to listen CoAP messages.
     */
    void start();

    /**
     * Stops the server, i.e. unbinds it from all ports. Frees as much system resources as possible to still be able to
     * be started.
     */
    void stop();

    /**
     * Destroys the server, i.e. unbinds from all ports and frees all system resources.
     */
    void destroy();

    /**
     * Send a Lightweight M2M request synchronously. Will block until a response is received from the remote client.
     */
    <T extends LwM2mResponse> T send(Client destination, DownlinkRequest<T> request);

    /**
     * Send a Lightweight M2M request asynchronously.
     */
    <T extends LwM2mResponse> void send(Client destination, DownlinkRequest<T> request,
            ResponseConsumer<T> responseCallback, ExceptionConsumer errorCallback);

    /**
     * Get the client registry containing the list of connected clients. You can use this object for listening client
     * registration/deregistration.
     */
    ClientRegistry getClientRegistry();

    /**
     * Get the Observation registry containing of current observation. You can use this object for listening resource
     * observation or cancel it.
     */
    ObservationRegistry getObservationRegistry();

    /**
     * Get the SecurityRegistry containing of security information.
     */
    SecurityRegistry getSecurityRegistry();
}
