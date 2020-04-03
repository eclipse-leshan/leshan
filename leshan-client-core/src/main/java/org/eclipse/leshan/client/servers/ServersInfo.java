/*******************************************************************************
 * Copyright (c) 2015 Sierra Wireless and others.
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
package org.eclipse.leshan.client.servers;

import java.util.HashMap;
import java.util.Map;

/**
 * It contains all information about servers (LWM2M Bootstrap server or LWM2M server).
 */
public class ServersInfo {

    public ServerInfo bootstrap;

    // <shortServerId, Info>
    public Map<Long, DmServerInfo> deviceManagements = new HashMap<>();

    @Override
    public String toString() {
        return String.format("ServersInfo [bootstrap=%s, deviceManagements=%s]", bootstrap, deviceManagements);
    }

}
