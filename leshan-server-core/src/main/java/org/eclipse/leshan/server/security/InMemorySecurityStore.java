/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.eclipse.leshan.core.oscore.OscoreIdentity;

/**
 * A {@link SecurityStore} which store {@link SecurityInfo} in memory.
 */
public class InMemorySecurityStore implements EditableSecurityStore {

    // lock for the two maps
    protected final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    protected final Lock readLock = readWriteLock.readLock();
    protected final Lock writeLock = readWriteLock.writeLock();

    // by client end-point
    protected Map<String, SecurityInfo> securityByEp = new HashMap<>();

    // by PSK identity
    protected Map<String, SecurityInfo> securityByPskIdentity = new HashMap<>();

    // by PSK oscoreIdentity
    protected Map<OscoreIdentity, SecurityInfo> securityByOscoreIdentity = new HashMap<>();

    private final List<SecurityStoreListener> listeners = new CopyOnWriteArrayList<>();

    public InMemorySecurityStore() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SecurityInfo getByEndpoint(String endpoint) {
        readLock.lock();
        try {
            return securityByEp.get(endpoint);
        } finally {
            readLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SecurityInfo getByIdentity(String identity) {
        readLock.lock();
        try {
            return securityByPskIdentity.get(identity);
        } finally {
            readLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SecurityInfo getByOscoreIdentity(OscoreIdentity oscoreIdentity) {
        readLock.lock();
        try {
            return securityByOscoreIdentity.get(oscoreIdentity);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Collection<SecurityInfo> getAll() {
        readLock.lock();
        try {
            return Collections.unmodifiableCollection(new ArrayList<>(securityByEp.values()));
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public SecurityInfo add(SecurityInfo info) throws NonUniqueSecurityInfoException {
        writeLock.lock();
        try {
            // For PSK, check if PSK identity is not already used.
            String pskIdentity = info.getPskIdentity();
            if (pskIdentity != null) {
                SecurityInfo infoByPskIdentity = securityByPskIdentity.get(pskIdentity);
                if (infoByPskIdentity != null && !info.getEndpoint().equals(infoByPskIdentity.getEndpoint())) {
                    throw new NonUniqueSecurityInfoException("PSK Identity " + pskIdentity + " is already used");
                }
                securityByPskIdentity.put(pskIdentity, info);
            }

            // For OSCORE, check if Oscore identity is not already used.
            OscoreIdentity oscoreIdentity = info.getOscoreSetting() != null
                    ? info.getOscoreSetting().getOscoreIdentity()
                    : null;
            if (oscoreIdentity != null) {
                SecurityInfo infoByOscoreIdentity = securityByOscoreIdentity.get(oscoreIdentity);
                if (infoByOscoreIdentity != null && !info.getEndpoint().equals(infoByOscoreIdentity.getEndpoint())) {
                    throw new NonUniqueSecurityInfoException("Oscore Identity " + oscoreIdentity + " is already used");
                }
                securityByOscoreIdentity.put(oscoreIdentity, info);
            }

            // Add new security info
            SecurityInfo previous = securityByEp.put(info.getEndpoint(), info);

            // For PSK, remove index by PSK Identity if needed
            String previousPskIdentity = previous == null ? null : previous.getPskIdentity();
            if (previousPskIdentity != null && !previousPskIdentity.equals(pskIdentity)) {
                securityByPskIdentity.remove(previousPskIdentity);
            }

            // For OSCORE, remove index by OSCORE Identity if needed
            OscoreIdentity previousOscoreIdentity = previous == null || previous.getOscoreSetting() == null ? null
                    : previous.getOscoreSetting().getOscoreIdentity();
            if (previousOscoreIdentity != null && !previousOscoreIdentity.equals(oscoreIdentity)) {
                securityByOscoreIdentity.remove(previousOscoreIdentity);
            }

            return previous;
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public SecurityInfo remove(String endpoint, boolean infosAreCompromised) {
        writeLock.lock();
        try {
            SecurityInfo info = securityByEp.get(endpoint);
            if (info != null) {
                // For PSK, remove index by PSK Identity if needed
                if (info.getPskIdentity() != null) {
                    securityByPskIdentity.remove(info.getPskIdentity());
                }
                // For OSCORE, remove index by OSCORE Identity if needed
                if (info.getOscoreSetting() != null) {
                    securityByOscoreIdentity.remove(info.getOscoreSetting().getOscoreIdentity());
                }
                securityByEp.remove(endpoint);
                for (SecurityStoreListener listener : listeners) {
                    listener.securityInfoRemoved(infosAreCompromised, info);
                }
            }
            return info;
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void addListener(SecurityStoreListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(SecurityStoreListener listener) {
        listeners.remove(listener);
    }
}
