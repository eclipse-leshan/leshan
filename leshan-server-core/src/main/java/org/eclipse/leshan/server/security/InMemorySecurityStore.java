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
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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
    protected Map<String, SecurityInfo> securityByIdentity = new HashMap<>();

    private SecurityStoreListener listener;

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
            return securityByIdentity.get(identity);
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
            String identity = info.getIdentity();
            if (identity != null) {
                SecurityInfo infoByIdentity = securityByIdentity.get(info.getIdentity());
                if (infoByIdentity != null && !info.getEndpoint().equals(infoByIdentity.getEndpoint())) {
                    throw new NonUniqueSecurityInfoException("PSK Identity " + info.getIdentity() + " is already used");
                }

                securityByIdentity.put(info.getIdentity(), info);
            }

            SecurityInfo previous = securityByEp.put(info.getEndpoint(), info);
            String previousIdentity = previous == null ? null : previous.getIdentity();
            if (previousIdentity != null && !previousIdentity.equals(identity)) {
                securityByIdentity.remove(previousIdentity);
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
                if (info.getIdentity() != null) {
                    securityByIdentity.remove(info.getIdentity());
                }
                securityByEp.remove(endpoint);
                if (listener != null) {
                    listener.securityInfoRemoved(infosAreCompromised, info);
                }
            }
            return info;
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void setListener(SecurityStoreListener listener) {
        this.listener = listener;
    }
}
