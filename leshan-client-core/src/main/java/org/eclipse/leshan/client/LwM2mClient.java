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
 *     Zebra Technologies - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.client;

import java.util.List;

import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.core.request.UplinkRequest;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ResponseCallback;

public interface LwM2mClient {

    /**
     * Starts the client (bind port, start to listen CoAP messages).
     */
    public void start();

    /**
     * Stops the client, i.e. unbinds it from all ports. Frees as much system resources as possible to still be able to
     * be started.
     */
    public void stop();

    /**
     * Destroys the client, i.e. unbinds from all ports and frees all system resources.
     */
    void destroy();

    /**
     * Send a Lightweight M2M request synchronously. Will block until a response is received from the remote server.
     *
     * @param request the request to the server
     * @return the response or <code>null</code> if the timeout expires (CoAP timeout).
     */
    public <T extends LwM2mResponse> T send(final UplinkRequest<T> request);

    /**
     * Send a Lightweight M2M request synchronously. Will block until a response is received from the remote server.
     * 
     * @param request the request to the server
     * @param timeout the request timeout in millisecond
     * @return the response or <code>null</code> if the timeout expires (given parameter or CoAP timeout).
     */
    public <T extends LwM2mResponse> T send(final UplinkRequest<T> request, long timeout);

    /**
     * Sends a Lightweight M2M request asynchronously.
     * 
     * @param request the request to the server
     * @param responseCallback the callback to process the LWM2M response
     * @param errorCallback the callback to process errors (e.g. CoAP timeout)
     */
    public <T extends LwM2mResponse> void send(final UplinkRequest<T> request,
            final ResponseCallback<T> responseCallback, final ErrorCallback errorCallback);

    List<LwM2mObjectEnabler> getObjectEnablers();

}