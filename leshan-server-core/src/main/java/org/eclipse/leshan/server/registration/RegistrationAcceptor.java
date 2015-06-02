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
package org.eclipse.leshan.server.registration;

import org.eclipse.leshan.server.client.Client;

/**
 * A registration acceptor decides if a client is allowed to be registered.
 */
public interface RegistrationAcceptor {

    /**
     * Should return true, if the client is allowed to be registered.
     *
     * @param client client with registration pending
     * @return true, if the client is allowed to be registered, otherwise false.
     */
    boolean acceptRegistration(Client client);
}
