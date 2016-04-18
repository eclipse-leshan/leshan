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
package org.eclipse.leshan.server.demo.extensions;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jetty.util.ConcurrentHashSet;
import org.eclipse.leshan.server.californium.impl.LeshanServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * Leshan Server Extensions Manager. Load and maintain server extension.
 */

public class LeshanServerExtensionsManager {

    private static final Logger LOG = LoggerFactory.getLogger(LeshanServerExtensionsManager.class);

    /**
     * Set of auto-starting extensions.
     * 
     * @see ExtensionConfig#autoStart
     */
    private final Set<String> autoStarting = new ConcurrentHashSet<String>();
    /**
     * Map of {@link ExtensionConfig#name} to {@link LeshanServerExtension}.
     */
    private final Map<String, LeshanServerExtension> extensions = new ConcurrentHashMap<String, LeshanServerExtension>();

    /**
     * Load extension for json file according the provided tags.
     * 
     * @param lwServer LWM2M server
     * @param extensions name of the JSON extensions file
     * @param extensionTags "," separated list of active tags. May be null.
     * @return Set of extension names.
     */
    public Set<String> loadExtensions(LeshanServer lwServer, String extensions, String extensionTags) {
        if (null != extensions) {
            try (Reader reader = new InputStreamReader(new FileInputStream(extensions))) {
                ExtensionConfig[] configurations = new Gson().fromJson(reader, ExtensionConfig[].class);
                String[] activeTags = null;
                if (null != extensionTags) {
                    activeTags = extensionTags.split(",");
                    for (int index = 0; activeTags.length > index; ++index) {
                        activeTags[index] = activeTags[index].trim();
                    }
                }
                for (ExtensionConfig configuration : configurations) {
                    if (!configuration.isActive(activeTags))
                        continue;
                    String name = configuration.name;
                    String className = configuration.className;
                    if (!ExtensionConfig.isDefined(name)) {
                        LOG.error("Extension missing name!");
                        continue;
                    }
                    if (!ExtensionConfig.isDefined(className)) {
                        LOG.error("Extension {} missing class name!", name);
                        continue;
                    }
                    if (this.extensions.containsKey(name)) {
                        LOG.error("Extension {} already loaded.", name);
                        continue;
                    }
                    try {
                        Class<?> extensionClass = Class.forName(className);
                        Object extensionInstance = extensionClass.newInstance();
                        if (extensionInstance instanceof LeshanServerExtension) {
                            LeshanServerExtension serverExtension = (LeshanServerExtension) extensionInstance;
                            if (serverExtension instanceof Tagging) {
                                // store/apply active tags also for sub extensions.
                                ((Tagging) serverExtension).setActiveTags(activeTags);
                            }
                            serverExtension.setup(lwServer, configuration, this);
                            this.extensions.put(name, serverExtension);
                            if (null == configuration.autoStart || configuration.autoStart) {
                                LOG.info("Server extension {} loaded (auto starting).", name);
                                autoStarting.add(name);
                            } else {
                                LOG.info("Server extension {} loaded.", name);
                            }
                        } else {
                            LOG.error("Wrong extension class {}, doesn't extend LeshanServerExtension.", className);
                        }
                    } catch (ClassNotFoundException e) {
                        LOG.error("Missing extension class {}.", className);
                    } catch (InstantiationException e) {
                        LOG.error("Error creating instance of extension class {}.", className);
                        LOG.error("Error:", e);
                    } catch (IllegalAccessException e) {
                        LOG.error("Error creating instance of extension class {}.", className);
                        LOG.error("Error:", e);
                    }
                }
            } catch (FileNotFoundException e1) {
                LOG.error("File not found {}.", extensions);
            } catch (IOException e1) {
                LOG.error("File I/O error.", e1);
            }
        }

        return this.extensions.keySet();
    }

    /**
     * Get set of extension names.
     * 
     * @return set of extension names
     */
    public Set<String> getExtensions() {
        return this.extensions.keySet();
    }

    /**
     * Start auto starting extensions.
     * 
     * @see #autoStarting
     * @see ExtensionConfig#autoStart
     */
    public void startAutoStarting() {
        for (String extension : autoStarting) {
            start(extension);
        }
    }

    /**
     * Stop auto starting extensions.
     * 
     * @see #autoStarting
     * @see ExtensionConfig#autoStart
     */
    public void stopAutoStarting() {
        for (String extension : autoStarting) {
            stop(extension);
        }
    }

    /**
     * Check, if extension is auto starting.
     * 
     * @param extension name of extension
     * @see #autoStarting
     * @see ExtensionConfig#autoStart
     */
    public boolean isAutoStarting(String extension) {
        return autoStarting.contains(extension);
    }

    /**
     * Remove extension from auto starting.
     * 
     * @param extension name of extension
     * @see #autoStarting
     * @see ExtensionConfig#autoStart
     */
    public void removeAutoStarting(String extension) {
        if (autoStarting.remove(extension)) {
            LOG.info("Server extension {} removed from auto start.", extension);
        }
    }

    /**
     * Check, if extension is available.
     * 
     * @param extension name of extension
     * @see #extensions
     */
    public boolean hasExtension(String extension) {
        return extensions.containsKey(extension);
    }

    /**
     * Start extension.
     * 
     * @param extension name of extension
     * @see #extensions
     */
    public void start(String extension) {
        LeshanServerExtension handler = extensions.get(extension);
        if (null != handler) {
            handler.start();
        }
    }

    /**
     * Stop extension.
     * 
     * @param extension name of extension
     * @see #extensions
     */
    public void stop(String extension) {
        LeshanServerExtension handler = extensions.get(extension);
        if (null != handler) {
            handler.stop();
        }
    }

}
