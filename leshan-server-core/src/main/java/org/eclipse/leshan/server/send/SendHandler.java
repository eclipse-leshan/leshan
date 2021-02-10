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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.request.SendRequest;
import org.eclipse.leshan.core.response.SendResponse;
import org.eclipse.leshan.core.response.SendableResponse;
import org.eclipse.leshan.server.registration.Registration;

/**
 * Class responsible to handle "Send" request from LWM2M client.
 * 
 * @see SendRequest
 */
public class SendHandler implements SendService {

    private final List<SendListener> listeners = new CopyOnWriteArrayList<>();;

    @Override
    public void addListener(SendListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(SendListener listener) {
        listeners.remove(listener);
    }

    public SendableResponse<SendResponse> handleSend(final Registration registration, final SendRequest request) {
        SendableResponse<SendResponse> response = new SendableResponse<>(SendResponse.success(), new Runnable() {
            @Override
            public void run() {
                fireDataReceived(registration, request.getNodes(), request);
            }
        });
        return response;
    }

    protected void fireDataReceived(Registration registration, Map<LwM2mPath, LwM2mNode> data, SendRequest request) {
        HashMap<String, LwM2mNode> nodes = new HashMap<>();
        for (Entry<LwM2mPath, LwM2mNode> entry : data.entrySet()) {
            nodes.put(entry.getKey().toString(), entry.getValue());
        }

        for (SendListener listener : listeners) {
            listener.dataReceived(registration, Collections.unmodifiableMap(nodes), request);
        }
    }
}
