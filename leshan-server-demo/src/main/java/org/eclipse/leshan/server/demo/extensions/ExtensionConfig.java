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

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extension configuration data. Intended to be read from JSON format.
 */
@SuppressWarnings("serial")
public class ExtensionConfig implements Serializable {
    private static final Logger LOG = LoggerFactory.getLogger(ExtensionConfig.class);

    /**
     * Name of extension.
     */
    public String name;
    /**
     * Class name of extension class. Class must implement {@link LeshanServerExtension}
     */
    public String className;
    /**
     * Auto start indicator. Default: true
     */
    public Boolean autoStart;
    /**
     * Tags for optional loading extension. If used, the extension is only started, if one tag is contained in the
     * active tags passed in by arguments. If empty, the extension is always started.
     * 
     * @see #isActive(String[])
     */
    public Set<String> tags = new HashSet<>();
    /**
     * Configuration properties passed in to extension. See special extension documentation for values.
     * 
     * @see #get(String, String)
     * @see #get(String, Boolean)
     * @see #get(String, Integer)
     * @see #get(String, Long)
     */
    public Map<String, String> configuration = new HashMap<>();

    /**
     * Get configuration string property for provided name.
     * 
     * @param key property name
     * @param def default value
     * @return configured value.
     */
    public String get(String key, String def) {
        String value = configuration.get(key);
        if (null != value && !value.isEmpty()) {
            return value;
        }
        return def;
    }

    /**
     * Get configuration long property for provided name.
     * 
     * @param key property name
     * @param def default value
     * @return configured value.
     */
    public Long get(String key, Long def) {
        String value = configuration.get(key);
        if (null != value && !value.isEmpty()) {
            try {
                return Long.decode(value);
            } catch (NumberFormatException ex) {
                LOG.error("Error decoding '" + key + "' := '" + value + "'", ex);
            }
        }
        return def;
    }

    /**
     * Get configuration int property for provided name.
     * 
     * @param key property name
     * @param def default value
     * @return configured value.
     */
    public Integer get(String key, Integer def) {
        String value = configuration.get(key);
        if (null != value && !value.isEmpty()) {
            try {
                return Integer.decode(value);
            } catch (NumberFormatException ex) {
                LOG.error("Error decoding '" + key + "' := '" + value + "'", ex);
            }
        }
        return def;
    }

    /**
     * Get configuration boolean property for provided name.
     * 
     * @param key property name
     * @param def default value
     * @return configured value.
     */
    public Boolean get(String key, Boolean def) {
        String value = configuration.get(key);
        if (null != value && !value.isEmpty()) {
            try {
                return Boolean.valueOf(value);
            } catch (NumberFormatException ex) {
                LOG.error("Error decoding '" + key + "' := '" + value + "'", ex);
            }
        }
        return def;
    }

    /**
     * Check, if extension is active according the provided active tags.
     * 
     * If {@link #tags} are empty, the extension is always started. If {@link #tags} are configured, the extension is
     * only started, if one tag is contained in the provided active tags.
     * 
     * @param activeTags active tags. May be null or of length 0, if not provided by the environment.
     * @return true, if extension is active, false, otherwise.
     */
    public boolean isActive(String[] activeTags) {
        if (tags.isEmpty())
            return true;
        if (null != activeTags) {
            for (String tag : activeTags) {
                if (tags.contains(tag))
                    return true;
            }
        }
        return false;
    }

    /**
     * Check, if the value is provided and not empty.
     * 
     * @param value value to be checked.
     * @return true, if value is provided (not null) and not empty, false, otherwise.
     */
    public static boolean isDefined(String value) {
        return null != value && !value.isEmpty();
    }
}
