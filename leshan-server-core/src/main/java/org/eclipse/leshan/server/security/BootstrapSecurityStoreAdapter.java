/*******************************************************************************
 * Copyright (c) 2021 Sierra Wireless and others.
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
package org.eclipse.leshan.server.security;

import java.util.Arrays;
import java.util.Iterator;

import org.eclipse.leshan.core.oscore.OscoreIdentity;

public class BootstrapSecurityStoreAdapter implements BootstrapSecurityStore {

    private SecurityStore store;

    public BootstrapSecurityStoreAdapter(SecurityStore store) {
        this.store = store;
    }

    @Override
    public Iterator<SecurityInfo> getAllByEndpoint(String endpoint) {
        SecurityInfo securityInfo = store.getByEndpoint(endpoint);
        if (securityInfo == null)
            return null;
        return Arrays.asList(securityInfo).iterator();
    }

    @Override
    public SecurityInfo getByIdentity(String pskIdentity) {
        return store.getByIdentity(pskIdentity);
    }

    @Override
    public SecurityInfo getByOscoreIdentity(OscoreIdentity oscoreIdentity) {
        return store.getByOscoreIdentity(oscoreIdentity);
    }
}
