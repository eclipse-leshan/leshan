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
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.server.client.Client;
import org.eclipse.leshan.server.client.ClientRegistry;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
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
     * Starts the server (bind port, start to listen CoAP messages).
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
     * Sends a Lightweight M2M request synchronously. Will block until a response is received from the remote client.
     * 
     * @param destination the remote client
     * @param request the request to the client
     * @return the response or <code>null</code> if the CoAP timeout expires ( see
     *         http://tools.ietf.org/html/rfc7252#section-4.2 ).
     */
    <T extends LwM2mResponse> T send(Client destination, DownlinkRequest<T> request);

    /**
     * Sends a Lightweight M2M request synchronously. Will block until a response is received from the remote client.
     * 
     * @param destination the remote client
     * @param request the request to the client
     * @param timeout the request timeout in millisecond
     * @return the response or <code>null</code> if the timeout expires (given parameter or CoAP timeout).
     */
    <T extends LwM2mResponse> T send(Client destination, DownlinkRequest<T> request, long timeout);

    /**
     * Sends a Lightweight M2M request asynchronously.
     */
    <T extends LwM2mResponse> void send(Client destination, DownlinkRequest<T> request,
            ResponseCallback<T> responseCallback, ErrorCallback errorCallback);

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

    /**
     * Get the provider in charge of retrieving the object definitions for each client.
     */
    LwM2mModelProvider getModelProvider();

}
