/*******************************************************************************
 * Copyright (c) 2015 Sierra Wireless and others.
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
package org.eclipse.leshan.server.demo.servlet.log;

import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.californium.core.coap.EmptyMessage;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.interceptors.MessageInterceptor;
import org.eclipse.leshan.server.client.Registration;
import org.eclipse.leshan.server.client.RegistrationService;

public class CoapMessageTracer implements MessageInterceptor {

    private final Map<String, CoapMessageListener> listeners = new ConcurrentHashMap<>();

    private final RegistrationService registry;

    public void addListener(String endpoint, CoapMessageListener listener) {
        Registration registration = registry.getByEndpoint(endpoint);
        if (registration != null) {
            listeners.put(toStringAddress(registration.getAddress(), registration.getPort()), listener);
        }
    }

    public void removeListener(String endpoint) {
        Registration registration = registry.getByEndpoint(endpoint);
        if (registration != null) {
            listeners.remove(toStringAddress(registration.getAddress(), registration.getPort()));
        }
    }

    private String toStringAddress(InetAddress clientAddress, int clientPort) {
        return clientAddress.toString() + ":" + clientPort;
    }

    public CoapMessageTracer(RegistrationService registry) {
        this.registry = registry;
    }

    @Override
    public void sendRequest(Request request) {
        CoapMessageListener listener = listeners.get(toStringAddress(request.getDestination(),
                request.getDestinationPort()));
        if (listener != null) {
            listener.trace(new CoapMessage(request, false));
        }
    }

    @Override
    public void sendResponse(Response response) {
        CoapMessageListener listener = listeners.get(toStringAddress(response.getDestination(),
                response.getDestinationPort()));
        if (listener != null) {
            listener.trace(new CoapMessage(response, false));
        }
    }

    @Override
    public void sendEmptyMessage(EmptyMessage message) {
        CoapMessageListener listener = listeners.get(toStringAddress(message.getDestination(),
                message.getDestinationPort()));
        if (listener != null) {
            listener.trace(new CoapMessage(message, false));
        }
    }

    @Override
    public void receiveRequest(Request request) {
        CoapMessageListener listener = listeners.get(toStringAddress(request.getSource(), request.getSourcePort()));
        if (listener != null) {
            listener.trace(new CoapMessage(request, true));
        }

    }

    @Override
    public void receiveResponse(Response response) {
        CoapMessageListener listener = listeners.get(toStringAddress(response.getSource(), response.getSourcePort()));
        if (listener != null) {
            listener.trace(new CoapMessage(response, true));
        }

    }

    @Override
    public void receiveEmptyMessage(EmptyMessage message) {
        CoapMessageListener listener = listeners.get(toStringAddress(message.getSource(), message.getSourcePort()));
        if (listener != null) {
            listener.trace(new CoapMessage(message, true));
        }

    }

}
