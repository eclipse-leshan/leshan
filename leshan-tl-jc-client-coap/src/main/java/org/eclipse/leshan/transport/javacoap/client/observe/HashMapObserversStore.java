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
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.mbed.coap.packet.CoapRequest;
import com.mbed.coap.packet.Opaque;

public class HashMapObserversStore implements ObserversStore {

    private final ConcurrentHashMap<Object, CoapRequest> store = new ConcurrentHashMap<>();
    private final List<ObserversListener> listeners = new CopyOnWriteArrayList<>();

    @Override
    public Iterator<CoapRequest> iterator() {
        return store.values().iterator();
    }

    @Override
    public void add(CoapRequest observeRequest) {
        CoapRequest previous = store.put(toKey(observeRequest), observeRequest);
        if (previous != null)
            fireObserversRemoved(previous);
        fireObserversAdded(observeRequest);
    }

    @Override
    public void remove(CoapRequest observeRequest) {
        CoapRequest previous = store.remove(toKey(observeRequest));
        if (previous != null) {
            fireObserversRemoved(previous);
        }
    }

    @Override
    public boolean contains(CoapRequest observeRequest) {
        return store.containsKey(toKey(observeRequest));
    }

    @Override
    public void addListener(ObserversListener listener) {
        listeners.add(listener);
    }

    private void fireObserversAdded(CoapRequest observeRequest) {
        for (ObserversListener listener : listeners) {
            listener.observersAdded(observeRequest);
        }
    }

    @Override
    public void removeListener(ObserversListener listener) {
        listeners.remove(listener);
    }

    private void fireObserversRemoved(CoapRequest observeRequest) {
        for (ObserversListener listener : listeners) {
            listener.observersRemoved(observeRequest);
        }
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
        public String toString() {
            return String.format("ObserverKey [token=%s, peerAddress=%s]", token != null ? token.toHex() : "null",
                    peerAddress);
        }

        @Override
        public final boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof ObserverKey))
                return false;
            ObserverKey that = (ObserverKey) o;
            return Objects.equals(token, that.token) && Objects.equals(peerAddress, that.peerAddress);
        }

        @Override
        public final int hashCode() {
            return Objects.hash(token, peerAddress);
        }
    }
}
