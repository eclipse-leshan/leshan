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
 *     Sierra Wireless - initial API and implementation
 *     Bosch Software Innovations GmbH - externalize registry listeners
 *******************************************************************************/
package org.eclipse.leshan.server.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.leshan.server.client.Client;
import org.eclipse.leshan.server.client.ClientRegistry;
import org.eclipse.leshan.server.client.ClientUpdate;
import org.eclipse.leshan.util.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * In-memory client registry.
 */
public class ClientRegistryImpl implements ClientRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(ClientRegistryImpl.class);

    private final Map<String /* end-point */, Client> clientsByEp = new ConcurrentHashMap<>();

    @Override
    public Collection<Client> allClients() {
        return Collections.unmodifiableCollection(clientsByEp.values());
    }

    @Override
    public Client get(String endpoint) {
        return clientsByEp.get(endpoint);
    }

    @Override
    public void registerClient(Client client) {
        Validate.notNull(client);
        LOG.debug("Registering new client: {}", client);
        clientsByEp.put(client.getEndpoint(), client);
    }

    @Override
    public Client updateClient(ClientUpdate update) {
        Validate.notNull(update);
        LOG.debug("Updating registration for client: {}", update);
        Client client = findByRegistrationId(update.getRegistrationId());
        if (client == null) {
            return null;
        } else {
            Client clientUpdated = update.updateClient(client);
            clientsByEp.put(clientUpdated.getEndpoint(), clientUpdated);

            return clientUpdated;
        }
    }

    @Override
    public Client deregisterClient(String registrationId) {
        Validate.notNull(registrationId);
        LOG.debug("Deregistering client with registrationId: {}", registrationId);
        Client toBeUnregistered = findByRegistrationId(registrationId);
        return toBeUnregistered == null ? null : clientsByEp.remove(toBeUnregistered.getEndpoint());
    }

    private Client findByRegistrationId(String id) {
        Client result = null;
        if (id != null) {
            for (Client client : clientsByEp.values()) {
                if (id.equals(client.getRegistrationId())) {
                    result = client;
                    break;
                }
            }
        }
        return result;
    }
}
