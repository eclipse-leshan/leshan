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

package org.eclipse.leshan.integration.tests;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.californium.core.WebLink;
import org.eclipse.californium.core.coap.LinkFormat;
import org.eclipse.leshan.bootstrap.BootstrapStoreImpl;
import org.eclipse.leshan.client.LwM2mClient;
import org.eclipse.leshan.client.californium.LeshanClient;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectsInitializer;
import org.eclipse.leshan.server.LwM2mServer;
import org.eclipse.leshan.server.californium.LeshanServerBuilder;
import org.eclipse.leshan.server.californium.impl.LwM2mBootstrapServerImpl;
import org.eclipse.leshan.server.client.Client;
import org.eclipse.leshan.server.impl.SecurityRegistryImpl;

/**
 * Helper for running a server and executing a client against it.
 * 
 */
public final class IntegrationTestHelper {

    static final String ENDPOINT_IDENTIFIER = "kdfflwmtm";
    static final int CLIENT_PORT = 44022;

    private final String clientDataModel = "</lwm2m>;rt=\"oma.lwm2m\", </lwm2m/1/101>, </lwm2m/1/102>, </lwm2m/2/0>, </lwm2m/2/1>, </lwm2m/2/2>, </lwm2m/3/0>, </lwm2m/4/0>, </lwm2m/5>";
    Set<WebLink> objectsAndInstances = LinkFormat.parse(clientDataModel);

    LwM2mServer server;
    LwM2mClient client;

    final InetSocketAddress serverAddress = new InetSocketAddress(InetAddress.getLoopbackAddress(), 5683);
    final InetSocketAddress clientAddress = new InetSocketAddress(InetAddress.getLoopbackAddress(), CLIENT_PORT);

    private LwM2mBootstrapServerImpl bootstrapServer;
    private boolean startBootstrap;

    public IntegrationTestHelper() {
        this(false);
    }

    public IntegrationTestHelper(final boolean startBootstrap) {
        this.startBootstrap = startBootstrap;
    }

    LwM2mClient createClient(final InetSocketAddress serverAddress) {
        ObjectsInitializer initializer = new ObjectsInitializer();
        List<ObjectEnabler> objects = initializer.create(2, 3);
        return new LeshanClient(clientAddress, serverAddress, new ArrayList<LwM2mObjectEnabler>(objects));
    }

    public void start() {
        final InetSocketAddress serverAddressSecure = new InetSocketAddress(InetAddress.getLoopbackAddress(), 5684);
        server = new LeshanServerBuilder().setLocalAddress(serverAddress).setLocalAddressSecure(serverAddressSecure)
                .build();

        if (startBootstrap) {
            bootstrapServer = new LwM2mBootstrapServerImpl(new BootstrapStoreImpl(), new SecurityRegistryImpl());
            bootstrapServer.start();
            client = createClient(serverAddressSecure);
            client.start();
        } else {
            server.start();
            client = createClient(serverAddress);
            client.start();
        }
    }

    public void stop() {
        client.stop();
        server.stop();
        if (bootstrapServer != null) {
            bootstrapServer.stop();
        }
    }

    Client getClient() {
        return server.getClientRegistry().get(ENDPOINT_IDENTIFIER);
    }
}
