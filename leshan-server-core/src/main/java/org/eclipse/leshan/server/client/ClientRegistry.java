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
 *******************************************************************************/
package org.eclipse.leshan.server.client;

import java.util.Collection;

import org.eclipse.leshan.core.request.UpdateRequest;

/**
 * A registry to access registered clients
 */
public interface ClientRegistry {

    /**
     * Retrieves a registered client by end-point.
     * 
     * @param endpoint
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
     * Adds a new listener to be notified with client registration events.
     * 
     * @param listener
     */
    void addListener(ClientRegistryListener listener);

    /**
     * Removes a client registration listener.
     * 
     * @param listener the listener to be removed
     */
    void removeListener(ClientRegistryListener listener);

    /**
     * Registers a new client.
     * 
     * An implementation must notify all registered listeners as part of processing the registration request.
     * 
     * @param client the client to register, identified by its end-point.
     * @return any <em>stale</em> registration information for the given client's end-point name or <code>null</code> if
     *         no stale registration info exists for the end-point. This may happen, if a client somehow loses track of
     *         its registration status with this server and simply starts over with a new registration request in order
     *         to remedy the situation. According to the LWM2M spec an implementation must remove the <em>stale</em>
     *         registration information in this case.
     */
    Client registerClient(Client client);

    /**
     * Updates registration properties for a given client.
     * 
     * @param update the registration properties to update
     * @return the updated registered client or <code>null</code> if no client is registered under the given end-point
     *         name
     */
    Client updateClient(UpdateRequest update);

    /**
     * De-registers a client.
     * 
     * @param registrationId the client registrationId
     * @return the previously registered client or <code>null</code> if no client is registered under the given ID
     */
    Client deregisterClient(String registrationId);
}
