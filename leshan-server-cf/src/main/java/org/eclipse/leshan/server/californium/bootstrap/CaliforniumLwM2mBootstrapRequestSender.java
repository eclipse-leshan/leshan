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
package org.eclipse.leshan.server.californium.bootstrap;

import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.node.codec.LwM2mNodeDecoder;
import org.eclipse.leshan.core.node.codec.LwM2mNodeEncoder;
import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.server.bootstrap.LwM2mBootstrapRequestSender;
import org.eclipse.leshan.server.californium.request.RequestSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CaliforniumLwM2mBootstrapRequestSender implements LwM2mBootstrapRequestSender {

    static final Logger LOG = LoggerFactory.getLogger(CaliforniumLwM2mBootstrapRequestSender.class);

    private final LwM2mModel model;

    private final RequestSender sender;

    public CaliforniumLwM2mBootstrapRequestSender(Endpoint secureEndpoint, Endpoint nonSecureEndpoint, LwM2mModel model,
            LwM2mNodeEncoder encoder, LwM2mNodeDecoder decoder) {
        this.model = model;
        this.sender = new RequestSender(secureEndpoint, nonSecureEndpoint, encoder, decoder);
    }

    @Override
    public <T extends LwM2mResponse> T send(final String endpointName, final Identity destination,
            final DownlinkRequest<T> request, long timeout) throws InterruptedException {
        return sender.sendLwm2mRequest(endpointName, destination, null, model, null, request, timeout);
    }

    @Override
    public <T extends LwM2mResponse> void send(final String endpointName, final Identity destination,
            final DownlinkRequest<T> request, final long timeout, ResponseCallback<T> responseCallback,
            ErrorCallback errorCallback) {
        sender.sendLwm2mRequest(endpointName, destination, null, model, null, request, timeout, responseCallback,
                errorCallback);
    }
}
