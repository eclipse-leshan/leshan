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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.leshan.core.node.TimestampedLwM2mNodes;
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
                fireDataReceived(registration, request.getTimestampedNodes(), request);
            }
        });
        return response;
    }

    protected void fireDataReceived(Registration registration, TimestampedLwM2mNodes data, SendRequest request) {
        for (SendListener listener : listeners) {
            listener.dataReceived(registration, data, request);
        }
    }

    public void onError(Registration registration, Exception error) {
        for (SendListener listener : listeners) {
            listener.onError(registration, error);
        }
    }
}
