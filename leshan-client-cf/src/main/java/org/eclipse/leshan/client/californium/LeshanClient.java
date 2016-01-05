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
package org.eclipse.leshan.client.californium;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.leshan.client.LwM2mClient;
import org.eclipse.leshan.client.californium.impl.CaliforniumLwM2mClientRequestSender;
import org.eclipse.leshan.client.californium.impl.ObjectResource;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.core.request.UplinkRequest;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.util.Validate;

/**
 * A Lightweight M2M client.
 */
public class LeshanClient implements LwM2mClient {

    private final CoapServer clientSideServer;
    private final AtomicBoolean clientServerStarted = new AtomicBoolean(false);
    private final List<LwM2mObjectEnabler> objectEnablers;
    private final InetSocketAddress serverAddress;
    private CaliforniumLwM2mClientRequestSender requestSender;

    /**
     * Creates a new client from a server address and LWM2M objects to expose.
     * <p>
     * The client created by this constructor supports un-encrypted communication using UDP only and binds to an
     * ephemeral port on all local network interfaces.
     * </p>
     * <p>
     * In order to create a client supporting secure communication use the
     * {@link #LeshanClient(InetSocketAddress, CoapServer, List)} constructor and pass in a <code>CoapServer</code>
     * (pre-)configured with a secure <code>Endpoint</code> supporting DTLS.
     * </p>
     * 
     * @param serverAddress the IP address and port of the server the client is supposed to access.
     * @param objectEnablers the LWM2M object types the client should expose to the server
     */
    public LeshanClient(final InetSocketAddress serverAddress,
            final List<? extends LwM2mObjectEnabler> objectEnablers) {
        this(new InetSocketAddress(0), serverAddress, objectEnablers);
    }

    /**
     * Creates a new client from a local address, server address and LWM2M objects to expose.
     * <p>
     * The client created by this constructor supports un-encrypted communication using UDP only and binds to the
     * network interface and port given in the <code>clientAddress</code> parameter.
     * </p>
     * <p>
     * In order to create a client supporting secure communication use the
     * {@link #LeshanClient(InetSocketAddress, CoapServer, List)} constructor and pass in a <code>CoapServer</code>
     * (pre-)configured with a secure <code>Endpoint</code> supporting DTLS.
     * </p>
     * 
     * @param clientAddress the IP address and port the client should use for communicating with the server.
     * @param serverAddress the IP address and port of the server the client is supposed to access.
     * @param objectEnablers the LWM2M object types the client should expose to the server
     */
    public LeshanClient(final InetSocketAddress clientAddress, final InetSocketAddress serverAddress,
            final List<? extends LwM2mObjectEnabler> objectEnablers) {
        this(serverAddress, new CoapServer(), objectEnablers);
        clientSideServer.addEndpoint(new CoapEndpoint(clientAddress));
    }

    /**
     * Creates a new client from a server address, a pre-configured <code>CoapServer</code> and LWM2M objects to expose.
     * <p>
     * The client created by this constructor uses the given <code>CoapServer</code>'s first <code>Endpoint</code> for
     * communicating with the server.
     * </p>
     * <p>
     * In order to support secure communication an <code>Endpoint</code> needs to be registered with the
     * <code>CoapServer</code> that supports DTLS (Datagram TLS). The <em>Californium Scandium</em> project contains a
     * DTLS based <code>Connector</code> implementation that can be used to construct such a secure
     * <code>Endpoint</code>. Please consult <em>Scandium</em>'s documentation for details regarding the configuration
     * of the <code>DTLSConnector</code>.
     * </p>
     * 
     * @param serverAddress the IP address and port of the server the client is supposed to access.
     * @param serverLocal the CoAP server to use for communicating with the server
     * @param objectEnablers the LWM2M object types the client should expose to the server
     * @throws IllegalArgumentException if the given CoAP server already contains a resource with the same name as any
     *         of the objects' <code>id</code> property values.
     */
    public LeshanClient(final InetSocketAddress serverAddress, final CoapServer serverLocal,
            final List<? extends LwM2mObjectEnabler> objectEnablers) {
        Validate.notNull(serverLocal);
        Validate.notNull(serverAddress);
        Validate.notNull(objectEnablers);
        Validate.notEmpty(objectEnablers);
        this.clientSideServer = serverLocal;
        this.serverAddress = serverAddress;
        this.objectEnablers = new ArrayList<>(objectEnablers);
        for (LwM2mObjectEnabler enabler : objectEnablers) {
            if (clientSideServer.getRoot().getChild(Integer.toString(enabler.getId())) != null) {
                throw new IllegalArgumentException(
                        "Cannot add CoAP resource for object [id=" + enabler.getId() + "], resource already exists");
            }

            clientSideServer.add(new ObjectResource(enabler));
        }
    }

    @Deprecated
    public LeshanClient(final InetSocketAddress clientAddress, final InetSocketAddress serverAddress,
            final CoapServer serverLocal, final List<? extends LwM2mObjectEnabler> objectEnablers) {

        Validate.notNull(clientAddress);
        Validate.notNull(serverLocal);
        Validate.notNull(serverAddress);
        Validate.notNull(objectEnablers);
        Validate.notEmpty(objectEnablers);

        clientSideServer = serverLocal;
        this.serverAddress = serverAddress;
        this.objectEnablers = new ArrayList<>(objectEnablers);
        for (LwM2mObjectEnabler enabler : objectEnablers) {
            if (clientSideServer.getRoot().getChild(Integer.toString(enabler.getId())) != null) {
                throw new IllegalArgumentException(
                        "Trying to load Client Object of name '" + enabler.getId() + "' when one was already added.");
            }

            final ObjectResource clientObject = new ObjectResource(enabler);
            clientSideServer.add(clientObject);
        }

        final Endpoint endpoint = new CoapEndpoint(clientAddress);
        clientSideServer.addEndpoint(endpoint);

        requestSender = new CaliforniumLwM2mClientRequestSender(endpoint, serverAddress, this);
    }

    @Override
    public void start() {
        clientSideServer.start();
        if (requestSender == null) {
            requestSender = new CaliforniumLwM2mClientRequestSender(clientSideServer.getEndpoints().get(0),
                    serverAddress, this);
        }
        clientServerStarted.set(true);
    }

    @Override
    public void stop() {
        clientSideServer.stop();
        clientServerStarted.set(false);
    }

    @Override
    public void destroy() {
        clientSideServer.destroy();
        clientServerStarted.set(false);
    }

    @Override
    public <T extends LwM2mResponse> T send(final UplinkRequest<T> request) {
        if (!clientServerStarted.get()) {
            throw new IllegalStateException("Internal CoapServer is not started.");
        }
        return requestSender.send(request, null);
    }

    @Override
    public <T extends LwM2mResponse> T send(final UplinkRequest<T> request, long timeout) {
        if (!clientServerStarted.get()) {
            throw new IllegalStateException("Internal CoapServer is not started.");
        }
        return requestSender.send(request, timeout);
    }

    @Override
    public <T extends LwM2mResponse> void send(final UplinkRequest<T> request,
            final ResponseCallback<T> responseCallback, final ErrorCallback errorCallback) {
        if (!clientServerStarted.get()) {
            throw new IllegalStateException("Internal CoapServer is not started.");
        }
        requestSender.send(request, responseCallback, errorCallback);
    }

    @Override
    public List<LwM2mObjectEnabler> getObjectEnablers() {
        return objectEnablers;
    }
}
