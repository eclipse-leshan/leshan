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

import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.server.client.Client;

/**
 * A response holder object, which is also ClientAware.
 */
public class ClientAwareResponseHolder implements ClientAware {

    private final LwM2mResponse response;
    private final Client client;

    /**
     * Creates a new client-aware LwM2M response holder.
     * 
     * @param response response to hold
     * @param client client
     */
    public ClientAwareResponseHolder(final LwM2mResponse response, final Client client) {
        this.response = response;
        this.client = client;
    }

    /**
     * Creates a new client-aware LwM2M response holder (without client, i.e. null).
     * 
     * @param response response to hold
     */
    public ClientAwareResponseHolder(final LwM2mResponse response) {
        this.response = response;
        this.client = null;
    }

    @Override
    public Client getClient() {
        return client;
    }

    /**
     * @return lwm2m response contained in this holder
     */
    public LwM2mResponse getResponse() {
        return response;
    }
}
