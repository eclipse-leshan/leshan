/*******************************************************************************
 * Copyright (c) 2021 Sierra Wireless and others.
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
package org.eclipse.leshan.client.bootstrap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.servers.DmServerInfo;
import org.eclipse.leshan.client.servers.ServerInfo;
import org.eclipse.leshan.client.servers.ServersInfo;
import org.eclipse.leshan.client.servers.ServersInfoExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A abstract implementation of {@link BootstrapConsistencyChecker}
 */
public abstract class BaseBootstrapConsistencyChecker implements BootstrapConsistencyChecker {

    private static final Logger LOG = LoggerFactory.getLogger(BaseBootstrapConsistencyChecker.class);

    @Override
    public List<String> checkconfig(Map<Integer, LwM2mObjectEnabler> objectEnablers) {
        try {
            ServersInfo info = ServersInfoExtractor.getInfo(objectEnablers, true);

            List<String> errors = new ArrayList<>();
            if (info.bootstrap != null) {
                checkBootstrapServerInfo(info.bootstrap, errors);
            }
            for (DmServerInfo server : info.deviceManagements.values()) {
                checkDeviceMangementServerInfo(server, errors);
            }
            if (!errors.isEmpty()) {
                return errors;
            }
        } catch (RuntimeException e) {
            LOG.debug(e.getMessage(), e);
            return Arrays.asList(e.getMessage());
        }
        return null;
    }

    /**
     * Check if bootstrap server info is valid, add error message to <code>errors</code> if any.
     */
    protected abstract void checkBootstrapServerInfo(ServerInfo BootstrapServerInfo, List<String> errors);

    /**
     * Check if device management server info is valid, add error message to <code>errors</code> if any.
     */
    protected abstract void checkDeviceMangementServerInfo(DmServerInfo serverInfo, List<String> errors);
}
