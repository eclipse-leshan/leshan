/*******************************************************************************
 * Copyright (c) 2018 Sierra Wireless and others.
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
package org.eclipse.leshan.integration.tests;

import java.net.InetSocketAddress;

import org.eclipse.californium.scandium.dtls.pskstore.PskStore;
import org.eclipse.californium.scandium.util.ServerNames;

public class SinglePSKStore implements PskStore {

    private String identity;
    private byte[] key;

    public SinglePSKStore(String identity, byte[] key) {
        this.identity = identity;
        this.key = key;
    }

    @Override
    public byte[] getKey(String identity) {
        return key;
    }

    @Override
    public byte[] getKey(ServerNames serverName, String identity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getIdentity(InetSocketAddress inetAddress) {
        return identity;
    }

    @Override
    public String getIdentity(InetSocketAddress peerAddress, ServerNames virtualHost) {
        throw new UnsupportedOperationException();
    }

    public void setKey(byte[] key) {
        this.key = key;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }
}
