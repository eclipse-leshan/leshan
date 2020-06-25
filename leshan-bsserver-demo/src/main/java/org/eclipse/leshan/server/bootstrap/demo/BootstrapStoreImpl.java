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
 *     Rikard Höglund (RISE) - additions to support OSCORE
 *******************************************************************************/
package org.eclipse.leshan.server.bootstrap.demo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.californium.cose.AlgorithmID;
import org.eclipse.californium.cose.CoseException;
import org.eclipse.californium.oscore.HashMapCtxDB;
import org.eclipse.californium.oscore.OSCoreCtx;
import org.eclipse.californium.oscore.OSException;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.server.OscoreHandler;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig;
import org.eclipse.leshan.server.bootstrap.BootstrapStore;
import org.eclipse.leshan.server.bootstrap.demo.ConfigurationChecker.ConfigurationException;
import org.eclipse.leshan.util.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.upokecenter.cbor.CBORObject;

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
    public BootstrapConfig getBootstrap(String endpoint, Identity deviceIdentity) {
        return bootstrapByEndpoint.get(endpoint);
    }

    public void addConfig(String endpoint, BootstrapConfig config) throws ConfigurationException {
        ConfigurationChecker.verify(config);
        bootstrapByEndpoint.put(endpoint, config);
        addOscoreContext(config);
        saveToFile();
    }

    // If an OSCORE configuration came, add it to the context db
    public void addOscoreContext(BootstrapConfig config) {
        HashMapCtxDB db = OscoreHandler.getContextDB();
        LOG.trace("Adding OSCORE context information to the context database");
        BootstrapConfig.OscoreObject osc = null;
        for (Map.Entry<Integer, BootstrapConfig.OscoreObject> o : config.oscore.entrySet()) {
            osc = o.getValue();
            try {

                AlgorithmID hkdfAlg;
                if (osc.oscoreHmacAlgorithm.matches("-?\\d+")) { // As integer
                    hkdfAlg = AlgorithmID.FromCBOR(CBORObject.FromObject(Integer.parseInt(osc.oscoreHmacAlgorithm)));
                } else { // Indicated as string
                    hkdfAlg = AlgorithmID.valueOf(osc.oscoreHmacAlgorithm);
                }

                AlgorithmID aeadAlg;
                if (osc.oscoreAeadAlgorithm.matches("-?\\d+")) { // As integer
                    aeadAlg = AlgorithmID.FromCBOR(CBORObject.FromObject(Integer.parseInt(osc.oscoreAeadAlgorithm)));
                } else { // Indicated as string
                    aeadAlg = AlgorithmID.valueOf(osc.oscoreAeadAlgorithm);
                }

                // These empty byte arrays should be conveyed as nulls
                if (osc.oscoreMasterSalt.length == 0) {
                    osc.oscoreMasterSalt = null;
                }

                if (osc.oscoreIdContext.length == 0) {
                    osc.oscoreIdContext = null;
                }

                OSCoreCtx ctx = new OSCoreCtx(osc.oscoreMasterSecret, false, aeadAlg, osc.oscoreSenderId,
                        osc.oscoreRecipientId, hkdfAlg, 32, osc.oscoreMasterSalt, osc.oscoreIdContext);
                db.addContext(ctx);

            } catch (OSException | CoseException e) {
                LOG.error("Failed to add OSCORE context to context database.");
                e.printStackTrace();
            }
        }
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

    private void loadFromFile() {
        try {
            File file = new File(filename);
            if (file.exists()) {
                try (InputStreamReader in = new InputStreamReader(new FileInputStream(file))) {
                    Map<String, BootstrapConfig> config = gson.fromJson(in, gsonType);
                    bootstrapByEndpoint.putAll(config);
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
