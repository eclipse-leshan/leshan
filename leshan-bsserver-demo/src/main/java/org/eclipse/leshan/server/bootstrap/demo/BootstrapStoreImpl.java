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
 *     Achim Kraus (Bosch Software Innovations GmbH) - add json as storage format
 *******************************************************************************/
package org.eclipse.leshan.server.bootstrap.demo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.Validate;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig;
import org.eclipse.leshan.server.bootstrap.BootstrapStore;
import org.eclipse.leshan.server.bootstrap.demo.ConfigurationChecker.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

/**
 * Simple bootstrap store implementation storing bootstrap information in memory
 */
public class BootstrapStoreImpl implements BootstrapStore {

    private static final Logger LOG = LoggerFactory.getLogger(BootstrapStoreImpl.class);

    // default location for persistence
    public static final String DEFAULT_FILE = "data/bootstrap.json";

    private final String filename;
    private final Gson gson;
    private final Type gsonType;

    public BootstrapStoreImpl() {
        this(DEFAULT_FILE);
    }

    /**
     * @param filename the file path to persist the registry
     */
    public BootstrapStoreImpl(String filename) {
        Validate.notEmpty(filename);
        GsonBuilder builder = new GsonBuilder();
        builder.setPrettyPrinting();
        this.gson = builder.create();
        this.gsonType = new TypeToken<Map<String, BootstrapConfig>>() {
        }.getType();
        this.filename = filename;
        this.loadFromFile();
    }

    private Map<String, BootstrapConfig> bootstrapByEndpoint = new ConcurrentHashMap<>();

    @Override
    public BootstrapConfig getBootstrap(String endpoint) {
        return bootstrapByEndpoint.get(endpoint);
    }

    public void addConfig(String endpoint, BootstrapConfig config) throws ConfigurationException {
        ConfigurationChecker.verify(config);
        bootstrapByEndpoint.put(endpoint, config);
        saveToFile();
    }

    public Map<String, BootstrapConfig> getBootstrapConfigs() {
        return Collections.unmodifiableMap(bootstrapByEndpoint);
    }

    public boolean deleteConfig(String enpoint) {
        BootstrapConfig res = bootstrapByEndpoint.remove(enpoint);
        saveToFile();
        return res != null;
    }

    // /////// File persistence

    @SuppressWarnings("unchecked")
    private void loadFromFile() {
        try {
            File file = new File(filename);
            if (file.exists()) {
                try (InputStreamReader in = new InputStreamReader(new FileInputStream(file))) {
                    Map<String, BootstrapConfig> config = gson.fromJson(in, gsonType);
                    bootstrapByEndpoint.putAll(config);
                }
            } else {
                // TODO temporary code for retro compatibility: remove it later.
                if (DEFAULT_FILE.equals(filename)) {
                    file = new File("data/bootstrap.data");// old bootstrap configurations default filename
                    if (file.exists()) {
                        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
                            bootstrapByEndpoint.putAll((Map<String, BootstrapConfig>) in.readObject());
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("Could not load bootstrap infos from file", e);
        }
    }

    private synchronized void saveToFile() {
        try {
            // Create file if it does not exists.
            File file = new File(filename);
            if (!file.exists()) {
                File parent = file.getParentFile();
                if (parent != null) {
                    parent.mkdirs();
                }
                file.createNewFile();
            }

            // Write file
            try (OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(filename))) {
                out.write(gson.toJson(getBootstrapConfigs(), gsonType));
            }
        } catch (Exception e) {
            LOG.error("Could not save bootstrap infos to file", e);
        }
    }
}
