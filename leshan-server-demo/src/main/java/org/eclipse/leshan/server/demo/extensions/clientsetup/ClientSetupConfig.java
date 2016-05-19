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

import java.util.HashSet;
import java.util.Set;

import org.eclipse.leshan.server.demo.extensions.ExtensionConfig;

/**
 * Client setup configuration. Used for detail configuration for the {@link ClientSetupExtension}.
 */
@SuppressWarnings("serial")
public class ClientSetupConfig extends ExtensionConfig {

    /**
     * Set, if this configuration is intended for a client with a special endpoint name.
     */
    public String endpointName;
    /**
     * Set, if no {@link #endpointName} is provided to filter the clients to apply this configuration. Checks the
     * reported instances during registration to match with the provided URI. e.g. "/5/0" to configure devices, which
     * supports the "firmware update".
     */
    public String instanceUri;
    /**
     * Delay after the registration before the initialization of the client starts.
     */
    public Integer delayInMs;
    /**
     * If enabled, synchronize device time during initialization.
     */
    public boolean syncTime;
    /**
     * Set of URIs to observe. If set only contains "*" all URIs contained in the registration are observed.
     */
    public Set<String> observeUris = new HashSet<>();

    /**
     * Class for the client initialization state. Extends {@link BaseClientSetup}. Setup during startup. Not intended to
     * be contained in the JSON file.
     */
    public Class<?> stateClass;
}
