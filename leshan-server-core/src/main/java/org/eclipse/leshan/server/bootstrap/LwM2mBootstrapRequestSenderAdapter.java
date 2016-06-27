/*******************************************************************************
 * Copyright (c) 2016 Sierra Wireless and others.
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
package org.eclipse.leshan.server.bootstrap;

import java.net.InetSocketAddress;

import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ResponseCallback;

public class LwM2mBootstrapRequestSenderAdapter implements LwM2mBootstrapRequestSender {

    @Override
    public <T extends LwM2mResponse> T send(String clientEndpoint, InetSocketAddress client, boolean secure,
            DownlinkRequest<T> request, Long timeout) throws InterruptedException {
        return null;
    }

    @Override
    public <T extends LwM2mResponse> void send(String clientEndpoint, InetSocketAddress client, boolean secure,
            DownlinkRequest<T> request, ResponseCallback<T> responseCallback, ErrorCallback errorCallback) {
    }
}
