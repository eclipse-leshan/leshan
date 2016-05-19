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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.eclipse.leshan.server.californium.impl.LeshanServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extension to toggle other extensions. The extension, which should be toggled by this extension are configured in
 * {@link ExtensionConfig#configuration} with the extension name as key and {@link #CONFIG_EXTENSION_VALUE} as value.
 * 
 * <pre>
 *   "configuration": {
 *     "START_ENABLED": "false",
 *     "ENABLED_TIME_IN_S": "600",
 *     "DISABLED_TIME_IN_S": "600",
 *     "MessageDelayer": "TOGGLE",
 *     "MultiRequests": "TOGGLE"
 *   }
 * </pre>
 */
public class ToggleExtension implements LeshanServerExtension {
    /**
     * Configuration key for starting mode. If true, the extension starts with the toggled extensions enabled, if false,
     * with the toggled extensions disabled.
     */
    public static final String CONFIG_START_ENABLED = "START_ENABLED";
    /**
     * Configuration key for enabled time in seconds.
     */
    public static final String CONFIG_ENABLED_TIME_IN_S = "ENABLED_TIME_IN_S";
    /**
     * Configuration key for disabled time in seconds.
     */
    public static final String CONFIG_DISABLED_TIME_IN_S = "DISABLED_TIME_IN_S";
    /**
     * Configuration value to mark keys as name of toggling extensions.
     */
    public static final String CONFIG_EXTENSION_VALUE = "TOGGLE";

    private static final Logger LOG = LoggerFactory.getLogger(ToggleExtension.class);

    /**
     * Default starting mode.
     */
    private static final boolean DEFAULT_START_ENABLED = false;
    /**
     * Default value of enabled time in seconds.
     */
    private static final int DEFAULT_ENABLED_TIME_IN_S = 60 * 10;
    /**
     * Default value of disabled time in seconds.
     */
    private static final int DEFAULT_DISABLED_TIME_IN_S = 60 * 20;

    private boolean enabled;

    private void apply(LeshanServerExtensionsManager manager, Set<String> extensions, boolean mode) {
        for (String extension : extensions) {
            if (mode) {
                manager.start(extension);
            } else {
                manager.stop(extension);
            }
        }
    }

    private void loop(ExtensionConfig configuration, LeshanServerExtensionsManager manager) {
        Set<String> extensions = new HashSet<>();
        Set<String> exclude = new HashSet<>();
        exclude.add(CONFIG_START_ENABLED);
        exclude.add(CONFIG_ENABLED_TIME_IN_S);
        exclude.add(CONFIG_DISABLED_TIME_IN_S);
        exclude.add(configuration.name);
        final boolean startEnabled = configuration.get(CONFIG_START_ENABLED, DEFAULT_START_ENABLED);
        final int enabledTime = configuration.get(CONFIG_ENABLED_TIME_IN_S, DEFAULT_ENABLED_TIME_IN_S);
        final int disabledTime = configuration.get(CONFIG_DISABLED_TIME_IN_S, DEFAULT_DISABLED_TIME_IN_S);
        boolean enableMode = startEnabled;
        boolean disabled = true;

        for (Map.Entry<String, String> config : configuration.configuration.entrySet()) {
            String extension = config.getKey();
            if (exclude.contains(extension))
                continue;
            if (CONFIG_EXTENSION_VALUE.equals(config.getValue())) {
                if (!manager.hasExtension(extension)) {
                    LOG.warn("Extension {} not available!", extension);
                    continue;
                }
                LOG.info("Extension {} add to toggles", extension);
                extensions.add(extension);
                manager.removeAutoStarting(extension);
            }
        }
        if (startEnabled) {
            LOG.info("Toggles extension starting with enable time {}[s] followed by disabletime {}[s]", enabledTime,
                    disabledTime);
        } else {
            LOG.info("Toggles extension starting with disable time {}[s] followed by enable time {}[s]", disabledTime,
                    enabledTime);
        }
        long nano = System.nanoTime();
        while (true) {
            boolean enable;
            synchronized (this) {
                try {
                    wait(5000);
                } catch (InterruptedException e) {
                }
                enable = enabled;
            }
            if (enable) {
                if (disabled) {
                    nano = System.nanoTime();
                    enableMode = startEnabled;
                    disabled = false;
                }
                if (nano <= System.nanoTime()) {
                    if (enableMode) {
                        LOG.info("Toggles extension starts enable for {}[s]", enabledTime);
                    } else {
                        LOG.info("Toggles extension starts disable for {}[s]", disabledTime);
                    }
                    apply(manager, extensions, enableMode);
                    int time = enableMode ? enabledTime : disabledTime;
                    nano = System.nanoTime() + TimeUnit.NANOSECONDS.convert(time, TimeUnit.SECONDS);
                    enableMode = !enableMode;
                }
            } else if (!disabled) {
                apply(manager, extensions, false);
                disabled = true;
            }
        }
    }

    @Override
    public void setup(LeshanServer lwServer, final ExtensionConfig configuration,
            final LeshanServerExtensionsManager manager) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                loop(configuration, manager);
            }
        }, "Toggle");
        thread.start();
    }

    @Override
    public void start() {
        synchronized (this) {
            enabled = true;
            notify();
        }
        LOG.info("Extension toggles enabled");
    }

    @Override
    public void stop() {
        synchronized (this) {
            enabled = false;
            notify();
        }
        LOG.info("Extension toggles disabled");
    }

}
