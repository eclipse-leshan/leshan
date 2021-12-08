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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.eclipse.leshan.core.util.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link SecurityStore} which persists {@link SecurityInfo} in a file.
 * <p>
 * This implementation serializes the store content into a file to be able to re-load the {@link SecurityInfo} when the
 * server is restarted.
 * </p>
 */
public class FileSecurityStore implements EditableSecurityStore {

    private static final Logger LOG = LoggerFactory.getLogger(FileSecurityStore.class);

    protected final Lock writeLock = new ReentrantReadWriteLock().writeLock();

    // the name of the file used to persist the store content
    private final String filename;

    // default location for persistence
    private static final String DEFAULT_FILE = "data/security.data";

    private final EditableSecurityStore editableSecurityStore;

    public FileSecurityStore(String file, EditableSecurityStore editableSecurityStore) {
        this.editableSecurityStore = editableSecurityStore;
        Validate.notEmpty(file);
        filename = file;
        loadFromFile();
    }

    public FileSecurityStore(EditableSecurityStore editableSecurityStore) {
        this(DEFAULT_FILE, editableSecurityStore);
    }

    @Override
    public Collection<SecurityInfo> getAll() {
        return editableSecurityStore.getAll();
    }

    @Override
    public SecurityInfo add(SecurityInfo info) throws NonUniqueSecurityInfoException {
        writeLock.lock();
        try {
            SecurityInfo previous = editableSecurityStore.add(info);
            saveToFile();
            return previous;
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public SecurityInfo remove(String endpoint, boolean infosAreCompromised) {
        writeLock.lock();
        try {
            SecurityInfo info = editableSecurityStore.remove(endpoint, infosAreCompromised);
            if (info != null) {
                saveToFile();
            }
            return info;
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void setListener(SecurityStoreListener listener) {
        editableSecurityStore.setListener(listener);
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
                    editableSecurityStore.add(info);
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

    @Override
    public SecurityInfo getByEndpoint(String endpoint) {
        return editableSecurityStore.getByEndpoint(endpoint);
    }

    @Override
    public SecurityInfo getByIdentity(String pskIdentity) {
        return editableSecurityStore.getByIdentity(pskIdentity);
    }
}
