/*******************************************************************************
 * Copyright (c) 2021 Sierra Wireless and others.
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
package org.eclipse.leshan.server.send;

import java.util.Map;

import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.request.SendRequest;
import org.eclipse.leshan.server.registration.Registration;

/**
 * Listener used to be aware of new data sent by LWM2M client with "Send" Request.
 * 
 * @see SendRequest
 */
public interface SendListener {

    /**
     * Called when new data are received from a LWM2M client via a {@link SendRequest}
     * 
     * @param registration Registration of the client which send the data.
     * @param data The data received
     * @param request The request received
     */
    void dataReceived(Registration registration, Map<String, LwM2mNode> data, SendRequest request);

    // TODO should we add a listener, if called if something wrong happened when we handle SendRequest ?
}
