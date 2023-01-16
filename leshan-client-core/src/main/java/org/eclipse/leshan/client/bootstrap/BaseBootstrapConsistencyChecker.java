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

import static org.eclipse.leshan.core.LwM2mId.DEVICE;
import static org.eclipse.leshan.core.LwM2mId.DVC_SUPPORTED_BINDING;
import static org.eclipse.leshan.core.LwM2mId.SECURITY;
import static org.eclipse.leshan.core.LwM2mId.SERVER;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.servers.DmServerInfo;
import org.eclipse.leshan.client.servers.ServerInfo;
import org.eclipse.leshan.client.servers.ServersInfo;
import org.eclipse.leshan.client.servers.ServersInfoExtractor;
import org.eclipse.leshan.core.request.BindingMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A abstract implementation of {@link BootstrapConsistencyChecker}
 */
public abstract class BaseBootstrapConsistencyChecker implements BootstrapConsistencyChecker {

    private static final Logger LOG = LoggerFactory.getLogger(BaseBootstrapConsistencyChecker.class);

    @Override
    public List<String> checkconfig(Map<Integer, LwM2mObjectEnabler> objectEnablers) {
        List<String> errors = new ArrayList<>();

        // validate if mandatory object enabler are present.
        checkMandatoryObjectEnabler(objectEnablers, errors);

        // validate device object
        if (errors.isEmpty()) {
            checkDeviceObjectEnabler(objectEnablers, errors);
        }

        // validate bootstrap config.
        if (errors.isEmpty()) {
            checkBootstrapConfig(objectEnablers, errors);
        }

        if (!errors.isEmpty()) {
            return errors;
        } else {
            return null;
        }
    }

    /**
     * Check mandatory object enabler are present
     */
    protected void checkMandatoryObjectEnabler(Map<Integer, LwM2mObjectEnabler> objectEnablers, List<String> errors) {
        // Maybe it could make sense to use LwM2mModel and search for mandatory Object.
        // But there is some issue with current LeshanClient design :
        // See : https://github.com/eclipse/leshan/pull/1378#discussion_r1071426343

        if (!objectEnablers.containsKey(SECURITY)) {
            errors.add("Client MUST have ObjectEnabler for 'Security' Object (ID:0)");
        }
        if (!objectEnablers.containsKey(SERVER)) {
            errors.add("Client MUST have ObjectEnabler for 'Server' Object (ID:1)");
        }
        if (!objectEnablers.containsKey(DEVICE)) {
            errors.add("Client MUST have ObjectEnabler for 'Device' Object (ID:3)");
        }
    }

    /**
     * Check device object is valid
     */
    protected void checkDeviceObjectEnabler(Map<Integer, LwM2mObjectEnabler> objectEnablers, List<String> errors) {
        LwM2mObjectEnabler deviceObjectEnabler = objectEnablers.get(DEVICE);
        if (deviceObjectEnabler.getAvailableInstanceIds().size() != 1
                || !deviceObjectEnabler.getAvailableInstanceIds().contains(0)) {
            errors.add("'Device' object MUST have 1 object instance with instance ID : 0");
        } else if (!deviceObjectEnabler.getAvailableResourceIds(0).contains(DVC_SUPPORTED_BINDING)) {
            errors.add("'Device' object MUST support mandatory ressource 'Supported Binding' (ID:16)");
        } else {
            EnumSet<BindingMode> deviceSupportedBindingMode = ServersInfoExtractor
                    .getDeviceSupportedBindingMode(deviceObjectEnabler, 0);
            if (deviceSupportedBindingMode == null) {
                errors.add("'Supported Binding' (ID:16) resource from 'Device' object (ID:3) MUST have value");
            }
        }
    }

    /**
     * Check if config is valid enough to bootstrap the client
     */
    protected void checkBootstrapConfig(Map<Integer, LwM2mObjectEnabler> objectEnablers, List<String> errors) {
        try {
            ServersInfo info = ServersInfoExtractor.getInfo(objectEnablers, true);

            if (info.bootstrap != null) {
                checkBootstrapServerInfo(info.bootstrap, errors);
            }
            for (DmServerInfo server : info.deviceManagements.values()) {
                checkDeviceMangementServerInfo(server, errors);
            }
        } catch (RuntimeException e) {
            LOG.debug(e.getMessage(), e);
            errors.add(e.getMessage());
        }
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
