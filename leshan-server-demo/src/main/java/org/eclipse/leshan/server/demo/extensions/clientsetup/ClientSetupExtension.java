/*******************************************************************************
 * Copyright (c) 2016 Bosch Software Innovations GmbH and others.
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
 *    Achim Kraus (Bosch Software Innovations GmbH) - Initial contribution
 ******************************************************************************/
package org.eclipse.leshan.server.demo.extensions.clientsetup;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import org.eclipse.leshan.server.californium.impl.LeshanServer;
import org.eclipse.leshan.server.demo.extensions.ExtensionConfig;
import org.eclipse.leshan.server.demo.extensions.LeshanServerExtension;
import org.eclipse.leshan.server.demo.extensions.LeshanServerExtensionsManager;
import org.eclipse.leshan.server.demo.extensions.Tagging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * Client setup extension. Extension to setup clients after registration. Configure client setup in separated JSON file.
 */
public class ClientSetupExtension implements LeshanServerExtension, Tagging {
    /**
     * Configuration key for client setup configuration file.
     */
    public static final String CONFIG_FILE = "FILE";
    /**
     * Configuration key for client setup default delay.
     */
    public static final String CONFIG_DELAY_IN_MS = "DELAY_IN_MS";

    private static final Logger LOG = LoggerFactory.getLogger(ClientSetupExtension.class);

    /**
     * Value for client setup default delay.
     */
    private static final int DEFAULT_DELAY_IN_MS = 5000;

    /**
     * Client registry.
     */
    private volatile ClientRegistry clientRegistry;
    /**
     * Active tags for selection of client setups via tags.
     * 
     * @see ExtensionConfig#isActive(String[])
     */
    private String[] activeTags;

    @Override
    public void setActiveTags(String[] activeTags) {
        this.activeTags = activeTags;
    }

    @Override
    public void setup(final LeshanServer lwServer, ExtensionConfig configuration, LeshanServerExtensionsManager manager) {
        String file = configuration.get(CONFIG_FILE, "");
        int delay = configuration.get(CONFIG_DELAY_IN_MS, DEFAULT_DELAY_IN_MS);

        ClientSetupConfig def = new ClientSetupConfig();
        def.name = "Generic";
        def.className = GenericClientSetup.class.getName();
        def.stateClass = GenericClientSetup.class;
        def.instanceUri = "/3/0";
        def.observeUris.add("*");
        def.delayInMs = delay;

        ClientSetupConfig[] configurations = new ClientSetupConfig[] { def };
        if (!file.isEmpty()) {
            try (Reader reader = new InputStreamReader(new FileInputStream(file))) {
                configurations = new Gson().fromJson(reader, ClientSetupConfig[].class);
                for (ClientSetupConfig config : configurations) {
                    if (!config.isActive(activeTags))
                        continue;
                    if (!ExtensionConfig.isDefined(config.className)) {
                        config.className = def.className;
                        config.stateClass = def.stateClass;
                        if (null == config.delayInMs)
                            config.delayInMs = delay;
                        LOG.info("Generic client state {} loaded.", config.name);
                    } else {
                        try {
                            Class<?> stateClass = Class.forName(config.className);
                            Object state = stateClass.newInstance();
                            if (state instanceof BaseClientSetup) {
                                config.stateClass = stateClass;
                                if (null == config.delayInMs)
                                    config.delayInMs = delay;
                                LOG.info("Client state {} loaded.", config.name);
                            } else {
                                LOG.error("Wrong client state class {}, doesn't extend BaseClientSetup.",
                                        config.className);
                            }
                        } catch (ClassNotFoundException e) {
                            LOG.error("Missing client state class {}.", config.className);
                        } catch (InstantiationException e) {
                            LOG.error("Error creating instance of client state class {}.", config.className);
                            LOG.error("Error:", e);
                        } catch (IllegalAccessException e) {
                            LOG.error("Error creating instance of client state class {}.", config.className);
                            LOG.error("Error:", e);
                        }
                    }
                }
            } catch (FileNotFoundException e1) {
                LOG.error("File not found {}.", file);
            } catch (IOException e1) {
                LOG.error("File I/O error.", e1);
            }
        }

        final ClientRegistry registry = new ClientRegistry(configurations);
        lwServer.getClientRegistry().addListener(registry);
        clientRegistry = registry;

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                registry.loop(lwServer);
            }
        }, "ClientSetupExtension");
        thread.start();
    }

    @Override
    public void start() {
        ClientRegistry registry = clientRegistry;
        if (null != registry) {
            registry.start();
            LOG.info("Extension client initialization enabled");
        }
    }

    @Override
    public void stop() {
        ClientRegistry registry = clientRegistry;
        if (null != registry) {
            registry.stop();
            LOG.info("Extension client initialization disabled");
        }
    }

}
