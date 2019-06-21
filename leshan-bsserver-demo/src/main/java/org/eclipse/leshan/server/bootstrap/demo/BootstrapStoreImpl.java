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
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.util.Map;

import org.eclipse.leshan.server.bootstrap.BootstrapConfig;
import org.eclipse.leshan.server.bootstrap.EditableBootstrapConfigStore;
import org.eclipse.leshan.server.bootstrap.InMemoryBootstrapConfigStore;
import org.eclipse.leshan.server.bootstrap.InvalidConfigurationException;
import org.eclipse.leshan.util.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

/**
 * A {@link EditableBootstrapConfigStore} which persist configuration in a file using json format.
 */
public class BootstrapStoreImpl extends InMemoryBootstrapConfigStore {

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

    public void addToStore(String endpoint, BootstrapConfig config) throws InvalidConfigurationException {
        super.addConfig(endpoint, config);
    }

    @Override
    public void addConfig(String endpoint, BootstrapConfig config) throws InvalidConfigurationException {
        addToStore(endpoint, config);
        saveToFile();
    }

    @Override
    public BootstrapConfig removeConfig(String enpoint) {
        BootstrapConfig res = super.removeConfig(enpoint);
        saveToFile();
        return res;
    }

    // /////// File persistence
    private void loadFromFile() {
        try {
            File file = new File(filename);
            if (file.exists()) {
                try (InputStreamReader in = new InputStreamReader(new FileInputStream(file))) {
                    Map<String, BootstrapConfig> configs = gson.fromJson(in, gsonType);
                    for (Map.Entry<String, BootstrapConfig> config : configs.entrySet()) {
                        addToStore(config.getKey(), config.getValue());
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
