/*******************************************************************************
 * Copyright (c) 2023 Sierra Wireless and others.
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
package org.eclipse.leshan.transport.javacoap.client.observe;

import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import com.mbed.coap.packet.CoapRequest;
import com.mbed.coap.packet.Opaque;

public class HashMapObserversStore implements ObserversStore {

    private final ConcurrentHashMap<Object, CoapRequest> store = new ConcurrentHashMap<>();

    @Override
    public Iterator<CoapRequest> iterator() {
        return store.values().iterator();
    }

    @Override
    public void add(CoapRequest observeRequest) {
        store.put(toKey(observeRequest), observeRequest);
    }

    @Override
    public void remove(CoapRequest observeRequest) {
        store.remove(toKey(observeRequest));
    }

    protected Object toKey(CoapRequest observeRequest) {
        return new ObserverKey(observeRequest.getToken(), observeRequest.getPeerAddress());
    }

    static class ObserverKey {
        final Opaque token;
        final InetSocketAddress peerAddress;

        ObserverKey(Opaque token, InetSocketAddress peerAddress) {
            super();
            this.token = token;
            this.peerAddress = peerAddress;
        }

        @Override
        public int hashCode() {
            return Objects.hash(peerAddress, token);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ObserverKey other = (ObserverKey) obj;
            return Objects.equals(peerAddress, other.peerAddress) && Objects.equals(token, other.token);
        }
    }
}
