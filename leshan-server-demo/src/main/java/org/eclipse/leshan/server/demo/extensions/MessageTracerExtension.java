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
 *    Achim Kraus (Bosch Software Innovations GmbH) - Initial contribution
 ******************************************************************************/
package org.eclipse.leshan.server.demo.extensions;

import org.eclipse.californium.core.CaliforniumLogger;
import org.eclipse.californium.core.coap.EmptyMessage;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.californium.core.network.interceptors.MessageTracer;
import org.eclipse.californium.scandium.ScandiumLogger;
import org.eclipse.leshan.server.californium.impl.LeshanServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageTracerExtension extends MessageTracer implements LeshanServerExtension {
    private static final Logger LOG = LoggerFactory.getLogger(MessageTracerExtension.class);
    private volatile boolean enabled;

    @Override
    public void setup(LeshanServer lwServer, ExtensionConfig configuration, LeshanServerExtensionsManager manager) {
        ScandiumLogger.initialize();
        CaliforniumLogger.initialize();
        for (Endpoint endpoint : lwServer.getCoapServer().getEndpoints()) {
            endpoint.addInterceptor(this);
        }
    }

    @Override
    public void start() {
        enabled = true;
        LOG.info("Extension message tracer enabled");
    }

    @Override
    public void stop() {
        enabled = false;
        LOG.info("Extension message tracer disabled");
    }

    @Override
    public void sendRequest(Request request) {
        if (enabled) {
            super.sendRequest(request);
        }
    }

    @Override
    public void sendResponse(Response response) {
        if (enabled) {
            super.sendResponse(response);
        }
    }

    @Override
    public void sendEmptyMessage(EmptyMessage message) {
        if (enabled) {
            super.sendEmptyMessage(message);
        }
    }

    @Override
    public void receiveRequest(Request request) {
        if (enabled) {
            super.receiveRequest(request);
        }
    }

    @Override
    public void receiveResponse(Response response) {
        if (enabled) {
            super.receiveResponse(response);
        }
    }

    @Override
    public void receiveEmptyMessage(EmptyMessage message) {
        if (enabled) {
            super.receiveEmptyMessage(message);
        }
    }

}
