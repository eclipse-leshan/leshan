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
package org.eclipse.leshan.server.californium.impl;

import java.net.InetSocketAddress;
import java.util.Arrays;

import org.eclipse.californium.scandium.dtls.pskstore.PskStore;
import org.eclipse.leshan.server.security.BootstrapSecurityStore;
import org.eclipse.leshan.server.security.SecurityInfo;

/**
 * PSK Store to feed a Bootstrap server.
 * 
 * Only supports getting the PSK key for a given identity. (Getting identity from IP only makes sense on the client
 * side.)
 */
public class LwM2mBootstrapPskStore implements PskStore {

    private BootstrapSecurityStore bsSecurityStore;

    public LwM2mBootstrapPskStore(BootstrapSecurityStore bsSecurityStore) {
        this.bsSecurityStore = bsSecurityStore;
    }

    @Override
    public byte[] getKey(String identity) {
        SecurityInfo info = bsSecurityStore.getByIdentity(identity);
        if (info == null || info.getPreSharedKey() == null) {
            return null;
        } else {
            // defensive copy
            return Arrays.copyOf(info.getPreSharedKey(), info.getPreSharedKey().length);
        }
    }

    @Override
    public String getIdentity(InetSocketAddress inetAddress) {
        throw new UnsupportedOperationException("Getting PSK Id by IP addresss dos not make sense on BS server side.");
    }
}
