/*******************************************************************************
 * Copyright (c) 2022 Sierra Wireless and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 *
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.core.californium.identity;

import java.util.HashMap;

import org.eclipse.californium.core.network.Endpoint;

public class IdentityHandlerProvider {

    private final HashMap<Endpoint, IdentityHandler> identityHandlers = new HashMap<>();

    public void addIdentityHandler(Endpoint endpoint, IdentityHandler identityHandler) {
        identityHandlers.put(endpoint, identityHandler);
    }

    public IdentityHandler getIdentityHandler(Endpoint endpoint) {
        return identityHandlers.get(endpoint);
    }
}
