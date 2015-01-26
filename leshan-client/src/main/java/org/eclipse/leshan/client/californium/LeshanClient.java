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
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.network.CoAPEndpoint;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.californium.core.server.resources.Resource;
import org.eclipse.leshan.LinkObject;
import org.eclipse.leshan.client.LwM2mClient;
import org.eclipse.leshan.client.LwM2mServerMessageDeliverer;
import org.eclipse.leshan.client.californium.impl.CaliforniumLwM2mClientRequestSender;
import org.eclipse.leshan.client.californium.impl.ObjectResource;
import org.eclipse.leshan.client.resource.LinkFormattable;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.core.request.UplinkRequest;
import org.eclipse.leshan.core.response.ExceptionConsumer;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ResponseConsumer;
import org.eclipse.leshan.util.Validate;

/**
 * A Lightweight M2M client.
 */
public class LeshanClient implements LwM2mClient {

    private final CoapServer clientSideServer;
    private final AtomicBoolean clientServerStarted = new AtomicBoolean(false);
    private final CaliforniumLwM2mClientRequestSender requestSender;

    public LeshanClient(final InetSocketAddress clientAddress, final InetSocketAddress serverAddress,
            final List<LwM2mObjectEnabler> objectEnablers) {
        this(clientAddress, serverAddress, new CoapServer(), objectEnablers);
    }

    public LeshanClient(final InetSocketAddress clientAddress, final InetSocketAddress serverAddress,
            final CoapServer serverLocal, final List<LwM2mObjectEnabler> objectEnablers) {

        Validate.notNull(clientAddress);
        Validate.notNull(serverLocal);
        Validate.notNull(serverAddress);
        Validate.notNull(objectEnablers);
        Validate.notEmpty(objectEnablers);

        // TODO I'm not sure this is still necessary
        serverLocal.setMessageDeliverer(new LwM2mServerMessageDeliverer(serverLocal.getRoot()));

        final Endpoint endpoint = new CoAPEndpoint(clientAddress);
        serverLocal.addEndpoint(endpoint);

        clientSideServer = serverLocal;

        for (LwM2mObjectEnabler enabler : objectEnablers) {
            if (clientSideServer.getRoot().getChild(Integer.toString(enabler.getId())) != null) {
                throw new IllegalArgumentException("Trying to load Client Object of name '" + enabler.getId()
                        + "' when one was already added.");
            }

            final ObjectResource clientObject = new ObjectResource(enabler);
            clientSideServer.add(clientObject);
        }

        requestSender = new CaliforniumLwM2mClientRequestSender(serverLocal.getEndpoint(clientAddress), serverAddress,
                getObjectModel());
    }

    @Override
    public void start() {
        clientSideServer.start();
        clientServerStarted.set(true);
    }

    @Override
    public void stop() {
        clientSideServer.stop();
        clientServerStarted.set(false);
    }

    @Override
    public <T extends LwM2mResponse> T send(final UplinkRequest<T> request) {
        if (!clientServerStarted.get()) {
            throw new RuntimeException("Internal CoapServer is not started.");
        }
        return requestSender.send(request);
    }

    @Override
    public <T extends LwM2mResponse> void send(final UplinkRequest<T> request,
            final ResponseConsumer<T> responseCallback, final ExceptionConsumer errorCallback) {
        if (!clientServerStarted.get()) {
            throw new RuntimeException("Internal CoapServer is not started.");
        }
        requestSender.send(request, responseCallback, errorCallback);
    }

    // TODO this function should be refactored when we will implements discover request
    @Override
    public LinkObject[] getObjectModel(final Integer... ids) {
        if (ids.length > 3) {
            throw new IllegalArgumentException(
                    "An Object Model Only Goes 3 levels deep:  Object ID/ObjectInstance ID/Resource ID");
        }

        if (ids.length == 0) {
            final StringBuilder registrationMasterLinkObject = new StringBuilder();
            for (final Resource clientObject : clientSideServer.getRoot().getChildren()) {
                if (clientObject instanceof LinkFormattable) {
                    registrationMasterLinkObject.append(((LinkFormattable) clientObject).asLinkFormat()).append(",");
                }
            }

            registrationMasterLinkObject.deleteCharAt(registrationMasterLinkObject.length() - 1);

            return LinkObject.parse(registrationMasterLinkObject.toString().getBytes());
        }

        final Resource clientObject = clientSideServer.getRoot().getChild(Integer.toString(ids[0]));

        if (clientObject == null) {
            return new LinkObject[] {};
        } else if (ids.length == 1) {
            return LinkObject.parse(((LinkFormattable) clientObject).asLinkFormat().getBytes());
        }

        final Resource clientObjectInstance = clientObject.getChild(Integer.toString(ids[1]));

        if (clientObjectInstance == null) {
            return new LinkObject[] {};
        } else if (ids.length == 2) {
            return LinkObject.parse(((LinkFormattable) clientObjectInstance).asLinkFormat().getBytes());
        }

        final Resource clientResource = clientObjectInstance.getChild(Integer.toString(ids[2]));

        if (clientResource == null) {
            return new LinkObject[] {};
        }

        return LinkObject.parse(((LinkFormattable) clientResource).asLinkFormat().getBytes());
    }

}
