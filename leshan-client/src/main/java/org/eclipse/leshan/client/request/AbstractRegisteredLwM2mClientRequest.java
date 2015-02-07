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
package org.eclipse.leshan.client.request;

import org.eclipse.leshan.client.request.identifier.ClientIdentifier;

public abstract class AbstractRegisteredLwM2mClientRequest extends AbstractLwM2mClientRequest {

    protected final ClientIdentifier clientIdentifier;

    public AbstractRegisteredLwM2mClientRequest(final ClientIdentifier clientIdentifier, final long timeout) {
        super(timeout);
        this.clientIdentifier = clientIdentifier;
    }

    public AbstractRegisteredLwM2mClientRequest(final ClientIdentifier clientIdentifier) {
        this(clientIdentifier, DEFAULT_TIMEOUT_MS);
    }

    public ClientIdentifier getClientIdentifier() {
        return clientIdentifier;
    }

}