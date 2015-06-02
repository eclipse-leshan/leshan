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
package org.eclipse.leshan.server.client;

/**
 * A Client registry listener management provides a facility for adding/removing client registry listeners.
 */
public interface ClientRegistryListenerManagement {

    /**
     * Adds a listener which is notified on client registration changes.
     *
     * @param clientRegistryListener listener to add
     */
    void addClientRegistryListener(ClientRegistryListener clientRegistryListener);

    /**
     * Removes an already registered listener.
     *
     * @param clientRegistryListener listener to remove
     */
    void removeClientRegistryListener(ClientRegistryListener clientRegistryListener);
}
