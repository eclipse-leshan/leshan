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
 *     Achim Kraus (Bosch Software Innovations GmbH) - add json as storage format
 *     Orange - keep one JSON dependency
 *******************************************************************************/
package org.eclipse.leshan.server.bootstrap.demo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.eclipse.leshan.core.util.Validate;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig;
import org.eclipse.leshan.server.bootstrap.EditableBootstrapConfigStore;
import org.eclipse.leshan.server.bootstrap.InMemoryBootstrapConfigStore;
import org.eclipse.leshan.server.bootstrap.InvalidConfigurationException;
import org.eclipse.leshan.server.bootstrap.demo.json.ByteArraySerializer;
import org.eclipse.leshan.server.bootstrap.demo.json.EnumSetDeserializer;
import org.eclipse.leshan.server.bootstrap.demo.json.EnumSetSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.type.CollectionType;

/**
 * A {@link EditableBootstrapConfigStore} which persist configuration in a file using json format.
 */
public class JSONFileBootstrapStore extends InMemoryBootstrapConfigStore {

    private static final Logger LOG = LoggerFactory.getLogger(JSONFileBootstrapStore.class);

    // lock for the two maps
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final Lock readLock = readWriteLock.readLock();
    private final Lock writeLock = readWriteLock.writeLock();

    // default location for persistence
    public static final String DEFAULT_FILE = "data/bootstrapStore.json";

    private final String filename;
    private final ObjectMapper mapper;

    public JSONFileBootstrapStore() {
        this(DEFAULT_FILE);
    }

    /**
     * @param filename the file path to persist the registry
     */
    public JSONFileBootstrapStore(String filename) {
        Validate.notEmpty(filename);

        mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        SimpleModule module = new SimpleModule();
        module.addDeserializer(EnumSet.class, new EnumSetDeserializer());

        CollectionType collectionType = mapper.getTypeFactory().constructCollectionType(EnumSet.class, Object.class);
        module.addSerializer(new EnumSetSerializer(collectionType));

        module.addSerializer(new ByteArraySerializer(ByteArraySerializer.ByteMode.SIGNED));
        mapper.registerModule(module);

        this.filename = filename;
        this.loadFromFile();
    }

    @Override
    public Map<String, BootstrapConfig> getAll() {
        readLock.lock();
        try {
            return super.getAll();
        } finally {
            readLock.unlock();
        }
    }

    public void addToStore(String endpoint, BootstrapConfig config) throws InvalidConfigurationException {
        super.add(endpoint, config);
    }

    @Override
    public void add(String endpoint, BootstrapConfig config) throws InvalidConfigurationException {
        writeLock.lock();
        try {
            addToStore(endpoint, config);
            saveToFile();
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public BootstrapConfig remove(String enpoint) {
        writeLock.lock();
        try {
            BootstrapConfig res = super.remove(enpoint);
            saveToFile();
            return res;
        } finally {
            writeLock.unlock();
        }
    }

    // /////// File persistence
    private void loadFromFile() {
        try {
            File file = new File(filename);
            if (file.exists()) {
                try (InputStreamReader in = new InputStreamReader(new FileInputStream(file))) {
                    TypeReference<Map<String, BootstrapConfig>> bootstrapConfigTypeRef = new TypeReference<Map<String, BootstrapConfig>>() {
                    };
                    Map<String, BootstrapConfig> configs = mapper.readValue(in, bootstrapConfigTypeRef);
                    for (Map.Entry<String, BootstrapConfig> config : configs.entrySet()) {
                        addToStore(config.getKey(), config.getValue());
                    }

                }
            }
        } catch (Exception e) {
            LOG.error("Could not load bootstrap infos from file", e);
        }
    }

    private void saveToFile() {
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
                out.write(mapper.writeValueAsString(getAll()));
            }
        } catch (Exception e) {
            LOG.error("Could not save bootstrap infos to file", e);
        }
    }
}
