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

import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.request.exception.InvalidResponseException;
import org.eclipse.leshan.core.request.exception.RequestCanceledException;
import org.eclipse.leshan.core.request.exception.RequestRejectedException;
import org.eclipse.leshan.core.request.exception.TimeoutException;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.observation.ObservationService;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationService;
import org.eclipse.leshan.server.security.SecurityStore;

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
     * 
     * @throws CodecException if request payload can not be encoded.
     * @throws InterruptedException if the thread was interrupted.
     * @throws RequestRejectedException if the request is rejected by foreign peer.
     * @throws RequestCanceledException if the request is cancelled.
     * @throws InvalidResponseException if the response received is malformed.
     */
    <T extends LwM2mResponse> T send(Registration destination, DownlinkRequest<T> request) throws InterruptedException,
            CodecException, InvalidResponseException, RequestCanceledException, RequestRejectedException;

    /**
     * Sends a Lightweight M2M request synchronously. Will block until a response is received from the remote client.
     * 
     * @param destination the remote client
     * @param request the request to send to the client
     * @param timeout the request timeout in millisecond
     * @return the response or <code>null</code> if the timeout expires (given parameter or CoAP timeout).
     * 
     * @throws CodecException if request payload can not be encoded.
     * @throws InterruptedException if the thread was interrupted.
     * @throws RequestRejectedException if the request is rejected by foreign peer.
     * @throws RequestCanceledException if the request is cancelled.
     * @throws InvalidResponseException if the response received is malformed.
     */
    <T extends LwM2mResponse> T send(Registration destination, DownlinkRequest<T> request, long timeout)
            throws InterruptedException, CodecException, InvalidResponseException, RequestCanceledException,
            RequestRejectedException;

    /**
     * Sends a Lightweight M2M request asynchronously.
     * 
     * @param destination the remote client
     * @param request the request to send to the client
     * @param responseCallback a callback called when a response is received (successful or error response)
     * @param errorCallback a callback called when an error or exception occurred when response is received. It can be :
     *        <ul>
     *        <li>{@link RequestRejectedException} if the request is rejected by foreign peer.</li>
     *        <li>{@link RequestCanceledException} if the request is cancelled.</li>
     *        <li>{@link InvalidResponseException} if the response received is malformed.</li>
     *        <li>{@link TimeoutException} if the CoAP timeout expires ( see
     *        http://tools.ietf.org/html/rfc7252#section-4.2 ).</li>
     *        <li>or any other RuntimeException for unexpected issue.
     *        </ul>
     * @throws CodecException if request payload can not be encoded.
     */
    <T extends LwM2mResponse> void send(Registration destination, DownlinkRequest<T> request,
            ResponseCallback<T> responseCallback, ErrorCallback errorCallback) throws CodecException;

    /**
     * Get the registration service to access to registered clients. You can use this object for listening client
     * registration lifecycle.
     */
    RegistrationService getRegistrationService();

    /**
     * Get the Observation service to access current observations. You can use this object for listening resource
     * observation or cancel it.
     */
    ObservationService getObservationService();

    /**
     * Get the SecurityStore containing of security information.
     */
    SecurityStore getSecurityStore();

    /**
     * Get the provider in charge of retrieving the object definitions for each client.
     */
    LwM2mModelProvider getModelProvider();

}
