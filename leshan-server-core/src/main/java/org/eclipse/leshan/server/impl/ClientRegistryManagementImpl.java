/*******************************************************************************
 * Copyright (c) 2013-2015 Bosch Software Innovations GmbH and others.
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
 *     Bosch Software Innovations GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.server.impl;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.leshan.server.client.Client;
import org.eclipse.leshan.server.client.ClientRegistryListener;
import org.eclipse.leshan.server.client.ClientRegistryListenerManagement;
import org.eclipse.leshan.server.client.ClientRegistryNotification;

/**
 * Implementation for the client registry listener management and notification.
 */
public class ClientRegistryManagementImpl implements ClientRegistryListenerManagement, ClientRegistryNotification {

    private final List<ClientRegistryListener> registryListeners = new CopyOnWriteArrayList<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    public void addClientRegistryListener(ClientRegistryListener clientRegistryListener) {
        if (!registryListeners.contains(clientRegistryListener)) {
            registryListeners.add(clientRegistryListener);
        }
    }

    @Override
    public void removeClientRegistryListener(ClientRegistryListener clientRegistryListener) {
        registryListeners.remove(clientRegistryListener);
    }

    @Override
    public void notifyOnRegistration(final Client client) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                for (ClientRegistryListener clientRegistryListener : registryListeners) {
                    clientRegistryListener.registered(client);
                }
            }
        });
    }

    @Override
    public void notifyOnUpdate(final Client client) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                for (ClientRegistryListener clientRegistryListener : registryListeners) {
                    clientRegistryListener.updated(client);
                }
            }
        });
    }

    @Override
    public void notifyOnUnregistration(final Client client) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                for (ClientRegistryListener clientRegistryListener : registryListeners) {
                    clientRegistryListener.unregistered(client);
                }
            }
        });
    }
}
