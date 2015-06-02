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
package org.eclipse.leshan.server.client;

import java.util.Collection;

/**
 * A registry to access registered clients
 */
public interface ClientRegistry {

    /**
     * Retrieves a registered client by end-point.
     * 
     * @param endpoint endpoint name
     * @return the matching client or <code>null</code> if not found
     */
    Client get(String endpoint);

    /**
     * Returns an unmodifiable list of all registered clients.
     * 
     * @return the registered clients
     */
    Collection<Client> allClients();

    /**
     * Registers a new client.
     * 
     * An implementation must notify all registered listeners as part of processing the registration request.
     * 
     * @param client the client to register, identified by its end-point.
     */
    void registerClient(Client client);

    /**
     * Updates registration properties for a given client.
     * 
     * @param update the registration properties to update
     * @return the updated registered client or <code>null</code> if no client is registered under the given end-point
     *         name
     */
    Client updateClient(ClientUpdate update);

    /**
     * De-registers a client.
     * 
     * @param registrationId the client registrationId
     * @return the previously registered client or <code>null</code> if no client is registered under the given ID
     */
    Client deregisterClient(String registrationId);
}
