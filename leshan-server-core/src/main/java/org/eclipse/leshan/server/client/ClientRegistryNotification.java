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
 * A client registry notification provides a facility for notifying of client registry listeners.
 */
public interface ClientRegistryNotification {
    /**
     * Calls client registry listeners on registration event of a client.
     *
     * @param client new registered client
     */
    void notifyOnRegistration(Client client);

    /**
     * Calls client registry listeners on update event of a client.
     *
     * @param client updated client
     */
    void notifyOnUpdate(Client client);

    /**
     * Calls client registry listeners on unregistration of a client.
     *
     * @param client unregistered client
     */
    void notifyOnUnregistration(Client client);

}
