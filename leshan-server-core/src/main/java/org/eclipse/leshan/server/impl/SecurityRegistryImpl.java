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
package org.eclipse.leshan.server.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.eclipse.leshan.server.security.NonUniqueSecurityInfoException;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.eclipse.leshan.server.security.SecurityRegistry;
import org.eclipse.leshan.util.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An in-memory security store.
 * <p>
 * This implementation serializes the registry content into a file to be able to re-load the security infos when the
 * server is restarted.
 * </p>
 */
public class SecurityRegistryImpl implements SecurityRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityRegistryImpl.class);

    // lock for the two maps
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final Lock readLock = readWriteLock.readLock();
    private final Lock writeLock = readWriteLock.writeLock();

    // by client end-point
    private Map<String, SecurityInfo> securityByEp = new HashMap<>();

    // by PSK identity
    private Map<String, SecurityInfo> securityByIdentity = new HashMap<>();

    // the name of the file used to persist the registry content
    private final String filename;

    // default location for persistence
    private static final String DEFAULT_FILE = "data/security.data";

    public SecurityRegistryImpl() {
        this(DEFAULT_FILE);
    }

    public SecurityRegistryImpl(String file) {
        Validate.notEmpty(file);
        filename = file;
        loadFromFile();
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
            return Collections.unmodifiableCollection(securityByEp.values());
        } finally {
            readLock.unlock();
        }
    }

    private SecurityInfo addToRegistry(SecurityInfo info) throws NonUniqueSecurityInfoException {
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

            return previous;
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public SecurityInfo add(SecurityInfo info) throws NonUniqueSecurityInfoException {
        writeLock.lock();
        try {
            SecurityInfo previous = addToRegistry(info);
            saveToFile();
            return previous;
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public SecurityInfo remove(String endpoint) {
        writeLock.lock();
        try {
            SecurityInfo info = securityByEp.get(endpoint);
            if (info != null) {
                if (info.getIdentity() != null) {
                    securityByIdentity.remove(info.getIdentity());
                }
                securityByEp.remove(endpoint);

                saveToFile();
            }
            return info;
        } finally {
            writeLock.unlock();
        }
    }

    protected void loadFromFile() {
        File file = new File(filename);
        if (!file.exists()) {
            return;
        }

        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file));) {
            SecurityInfo[] infos = (SecurityInfo[]) in.readObject();

            if (infos != null) {
                for (SecurityInfo info : infos) {
                    addToRegistry(info);
                }
                if (infos.length > 0) {
                    LOG.debug("{} security infos loaded", infos.length);
                }
            }
        } catch (NonUniqueSecurityInfoException | IOException | ClassNotFoundException e) {
            LOG.error("Could not load security infos from file", e);
        }
    }

    protected void saveToFile() {
        try {
            File file = new File(filename);
            if (!file.exists()) {
                File parent = file.getParentFile();
                if (parent != null) {
                    parent.mkdirs();
                }
                file.createNewFile();
            }
            try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filename));) {
                out.writeObject(this.getAll().toArray(new SecurityInfo[0]));
            }
        } catch (IOException e) {
            LOG.error("Could not save security infos to file", e);
        }
    }
}
